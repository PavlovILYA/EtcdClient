package ru.tcinet.repository;

import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtcdRepository {
    private final Client etcdClient;
    private static final long LEASE_TTL = 10; // TTL в секундах

    public String getValue(String keyStr) throws ExecutionException, InterruptedException {
        ByteSequence key = ByteSequence.from(keyStr, StandardCharsets.UTF_8);

        CompletableFuture<GetResponse> getFuture = etcdClient.getKVClient().get(key);
        GetResponse response = getFuture.get();

        List<KeyValue> kvList = response.getKvs();
        if (kvList.isEmpty()) {
            return null;
        } else {
            return kvList.get(0).getValue().toString(StandardCharsets.UTF_8);
        }
    }

    public void updateLease(String keyStr, String valueStr, long leaseId) throws ExecutionException, InterruptedException {
        ByteSequence key = ByteSequence.from(keyStr, StandardCharsets.UTF_8);
        ByteSequence value = ByteSequence.from(valueStr, StandardCharsets.UTF_8);

        PutOption putOption = PutOption.builder().withLeaseId(leaseId).build();

        // Выполнение вставки с новым leaseId
        CompletableFuture<PutResponse> putFuture = etcdClient.getKVClient().put(key, value, putOption);

        PutResponse response = putFuture.get();

        log.info("LeaseId обновлен: {}", leaseId);
    }

    public boolean putValueWithLeaseIfKeyNotExist(String keyStr, String valueStr, long leaseId) throws ExecutionException, InterruptedException {
        ByteSequence key = ByteSequence.from(keyStr, StandardCharsets.UTF_8);
        ByteSequence value = ByteSequence.from(valueStr, StandardCharsets.UTF_8);

        PutOption putOption = PutOption.builder().withLeaseId(leaseId).build();

        // Условие: ключ должен быть ПУСТЫМ (не существовать)
        Cmp keyDoesNotExist = new Cmp(key, Cmp.Op.EQUAL, CmpTarget.createRevision(0));

        // Действие, если ключ не существует: записать значение
        Op putOp = Op.put(key, value, putOption);

        // Действие, если ключ уже есть: просто получить его текущее значение
        Op getOp = Op.get(key, GetOption.DEFAULT);

        // Выполнение транзакции
        Txn txn = etcdClient.getKVClient().txn().If(keyDoesNotExist).Then(putOp).Else(getOp);
        CompletableFuture<TxnResponse> txnFuture = txn.commit();
        TxnResponse response = txnFuture.get();

        log.info("Транзакция вставки: {}", response.isSucceeded());
        return response.isSucceeded();
    }

    public long getNewLeaseId() throws ExecutionException, InterruptedException {
        return etcdClient.getLeaseClient().grant(LEASE_TTL).get().getID();
    }

}

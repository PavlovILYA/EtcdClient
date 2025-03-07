package ru.tcinet.unused;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.PutOption;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LeaderElection {
    private static final String ETCD_ENDPOINT = "http://localhost:2379";
    private static final String LEADER_KEY = "/election/leader";
    private static final long LEASE_TTL = 5; // TTL в секундах
    private Client client;
    private long leaseId;

    public LeaderElection() {
        client = Client.builder().endpoints(ETCD_ENDPOINT).build();
    }

    public void runElection(String candidateId) {
        try {
            // 1. Создаем аренду (lease) с TTL
            leaseId = client.getLeaseClient().grant(LEASE_TTL).get().getID();
            System.out.println(candidateId + " получил lease ID: " + leaseId);

            // 2. Пытаемся записать ключ лидера в etcd с этим lease
            ByteSequence key = ByteSequence.from(LEADER_KEY, StandardCharsets.UTF_8);
            ByteSequence value = ByteSequence.from(candidateId, StandardCharsets.UTF_8);
            PutOption putOption = PutOption.builder().withLeaseId(leaseId).build();
            client.getKVClient().txn()
                    .If(new Cmp(ByteSequence.from(LEADER_KEY, StandardCharsets.UTF_8),
                            Cmp.Op.EQUAL, CmpTarget.createRevision(0)))
                    .Then(Op.put(key, value, putOption)) // Записываем, если ключ не существует
                    .commit();
//            client.getKVClient().put(key, value, putOption).get();
            System.out.println(candidateId + " стал лидером!");

            // 3. Обновляем lease (продлеваем TTL, чтобы оставаться лидером)
            keepAliveLease();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Ошибка при выполнении выборов: " + e.getMessage());
        }
    }

    private void keepAliveLease() {
        CompletableFuture<Void> future = client.getLeaseClient().keepAliveOnce(leaseId).thenAccept(keepAliveResponse ->
            System.out.println("Lease обновлен, TTL: " + keepAliveResponse.getTTL())
        );

        future.exceptionally(e -> {
            System.err.println("Ошибка обновления lease: " + e.getMessage());
            return null;
        });
    }

    public static void main(String[] args) {
        LeaderElection election = new LeaderElection();
        String candidateId = "Сервис-" + System.currentTimeMillis(); // Уникальный ID кандидата
        election.runElection(candidateId);
    }
}

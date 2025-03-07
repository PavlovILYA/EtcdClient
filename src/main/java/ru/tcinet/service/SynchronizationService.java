package ru.tcinet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tcinet.repository.EtcdRepository;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynchronizationService {
    private final EtcdRepository etcdRepository;
    private static final String LEADER_KEY = "/election/leader";


    private String candidateId;
    @PostConstruct
    public void init() {
        candidateId = "Сервис-" + System.currentTimeMillis();
        log.info("This is the candidate id: {}", candidateId);
    }


    public boolean isLeader() throws ExecutionException, InterruptedException {
        String value = etcdRepository.getValue(LEADER_KEY);
        if (value == null) {
            return etcdRepository.putValueWithLeaseIfKeyNotExist(LEADER_KEY, candidateId, etcdRepository.getNewLeaseId());
        } else {
            return value.equals(candidateId);
        }
    }

    public void synchronize() throws ExecutionException, InterruptedException {
        String value = etcdRepository.getValue(LEADER_KEY);
        if (value == null) {
            etcdRepository.putValueWithLeaseIfKeyNotExist(LEADER_KEY, candidateId, etcdRepository.getNewLeaseId());
        } else if (value.equals(candidateId)) {
            etcdRepository.updateLease(LEADER_KEY, candidateId, etcdRepository.getNewLeaseId());
        } else {
            log.info("sleep");
        }
    }
}

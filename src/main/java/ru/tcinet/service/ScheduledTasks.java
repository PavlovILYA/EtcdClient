package ru.tcinet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.tcinet.repository.EtcdRepository;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasks {
    private final SynchronizationService synchronizationService;

    @Scheduled(cron = "${useful.work.cron:* * * * * *}")
    public void doWork() throws ExecutionException, InterruptedException {
        if (synchronizationService.isLeader()) {
            log.info("I AM A LEADER! DO USEFUL WORK!");
        }
    }

    @Scheduled(cron = "${election.cron:* * * * * *}")
    public void startElection() throws ExecutionException, InterruptedException {
        synchronizationService.synchronize();
    }
}

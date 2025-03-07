package ru.tcinet.config;

import io.etcd.jetcd.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EtcdConfiguration {
    @Value("${etcd.endpoint-1}")
    private String endpoint1;
    @Value("${etcd.endpoint-2}")
    private String endpoint2;
    @Value("${etcd.endpoint-3}")
    private String endpoint3;

    @Bean
    public Client getEtcdClient() {
        return Client.builder()
                .endpoints(endpoint1, endpoint2, endpoint3)
                .build();
    }
}

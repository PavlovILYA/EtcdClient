package ru.tcinet;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.kv.GetResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.CompletableFuture;

@EnableScheduling
@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
//        oldMain();
    }

    private static void oldMain() {
        String host1 = "89.169.148.215";
        String host2 = "158.160.47.78";
        String host3 = "158.160.40.209";
        ByteSequence key = ByteSequence.from("mykey2".getBytes());
        ByteSequence value = ByteSequence.from("Hello, etcd!".getBytes());

        try (Client client = Client.builder()
                .endpoints(
                        "http://" + host1 + ":2379",
                        "http://" + host2 + ":2379",
                        "http://" + host3 + ":2379"
                )
                .build()) {
            KV kvClient = client.getKVClient();

            // Put a key-value pair
            kvClient.put(key, value).get();

            // Retrieve the value using CompletableFuture
            CompletableFuture<GetResponse> getFuture = kvClient.get(key);
            GetResponse response = getFuture.get();
            response.getKvs().forEach(keyValue -> {
                System.out.println(
                        new String(keyValue.getKey().getBytes()) + ": " + new String(keyValue.getValue().getBytes())
                );
            });

            // Delete the key
            kvClient.delete(key).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package com.didadida.video;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class KafkaConfigTest {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    @Test
    public void testKafkaConfig() {
        System.out.println("Kafka Bootstrap Servers: " + bootstrapServers);
        System.out.println("Kafka Consumer Group ID: " + consumerGroupId);
    }
}

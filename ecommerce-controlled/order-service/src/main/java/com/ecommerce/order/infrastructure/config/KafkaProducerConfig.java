package com.ecommerce.order.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Kafka producer configuration for Spring Cloud Stream.
 * Per docs/events/kafka-topics.md and AGENTS.md §7.8
 * 
 * Configuration via application.yml:
 * - Topic: order.events
 * - Broker: 127.0.0.1:29023
 * - Key: orderId (UUID)
 */
@Configuration
@EnableScheduling
public class KafkaProducerConfig {
    // Spring Cloud Stream configuration is externalized to application.yml
    // This class enables scheduling for OutboxPublisher
}

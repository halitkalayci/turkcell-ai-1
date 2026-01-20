package com.ecommerce.inventory.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * Kafka consumer configuration for Spring Cloud Stream.
 * Per docs/events/kafka-topics.md and AGENTS.md §7.8
 * 
 * Configuration via application.yml:
 * - Topic: order.events
 * - Consumer group: inventory-service-order-events
 * - Broker: 127.0.0.1:29023
 * - Retry: 5 attempts with exponential backoff
 * - DLQ: order.events.dlq
 */
@Configuration
public class KafkaConsumerConfig {
    // Spring Cloud Stream configuration is externalized to application.yml
}

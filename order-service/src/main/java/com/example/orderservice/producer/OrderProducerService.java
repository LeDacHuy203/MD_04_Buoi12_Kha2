package com.example.orderservice.producer;

import com.example.orderservice.dto.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.order-events:order-events}")
    private String topicName;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        String key = event.getOrderId();
        log.info("==> [Kafka Producer] Gửi OrderCreatedEvent vào topic '{}' với key '{}': {}", topicName, key, event);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topicName, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("==> [Kafka Producer] Đã gửi thành công! Topic: {}, Partition: {}, Offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("==> [Kafka Producer] Gửi sự kiện thất bại cho orderId '{}': {}", key, ex.getMessage(), ex);
            }
        });
    }
}

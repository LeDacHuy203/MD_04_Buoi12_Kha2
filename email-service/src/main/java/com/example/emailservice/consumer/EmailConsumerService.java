package com.example.emailservice.consumer;

import com.example.emailservice.dto.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class EmailConsumerService {

    // Danh sách lịch sử email đã gửi trong bộ nhớ để phục vụ kiểm tra
    private final List<String> sentEmailHistory = Collections.synchronizedList(new ArrayList<>());

    @KafkaListener(
            topics = "${app.kafka.topic.order-events:order-events}",
            groupId = "${spring.kafka.consumer.group-id:email-group}"
    )
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("==================================================================");
        log.info("📧 [EMAIL-SERVICE] [Group: email-group] Nhận sự kiện OrderCreatedEvent!");
        log.info("📧 [EMAIL-SERVICE] Đang soạn thảo email xác nhận đơn hàng '{}'...", event.getOrderId());
        log.info("📧 [EMAIL-SERVICE] Người nhận: {}", event.getCustomerEmail());
        log.info("📧 [EMAIL-SERVICE] Chi tiết: Sản phẩm #{}, Số lượng: {}, Tổng thanh toán: {} VNĐ",
                event.getProductId(), event.getQuantity(), event.getTotalPrice());

        // Giả lập gửi email thành công
        String logMessage = String.format("Gửi email thành công tới '%s' cho đơn hàng '%s'",
                event.getCustomerEmail(), event.getOrderId());
        sentEmailHistory.add(logMessage);

        log.info("📧 [EMAIL-SERVICE] ==> ĐÃ GỬI EMAIL THÀNH CÔNG tới khách hàng: {}", event.getCustomerEmail());
        log.info("==================================================================");
    }

    public List<String> getSentEmailHistory() {
        return sentEmailHistory;
    }
}

package com.example.emailservice.controller;

import com.example.emailservice.consumer.EmailConsumerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailConsumerService emailConsumerService;

    @GetMapping("/history")
    public ResponseEntity<List<String>> getEmailHistory() {
        return ResponseEntity.ok(emailConsumerService.getSentEmailHistory());
    }
}

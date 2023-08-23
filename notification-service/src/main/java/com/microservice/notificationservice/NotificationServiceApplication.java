package com.microservice.notificationservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.notificationservice.services.interfaces.EmailSenderService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;

@SpringBootApplication
@Slf4j
public class NotificationServiceApplication {

    @Autowired
    EmailSenderService emailSenderService;

    @Autowired
    private ObjectMapper objectMapper;

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    @KafkaListener(topics = "notificationTopic")
    public void sendNotificationEmailsWhenSubmittingRequest(RequestPlacedEvent requestPlacedEvent)
            throws MessagingException, JsonProcessingException {
        log.info("Received notification for Request {}", objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(requestPlacedEvent));
        emailSenderService.sendNotificationEmailsWhenSubmittingRequest(requestPlacedEvent);
    }
}

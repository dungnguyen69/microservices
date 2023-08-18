package com.microservice.notificationservice;

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

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    @KafkaListener(topics = "notificationTopic")
    public void handNotification(RequestPlacedEvent requestPlacedEvent) throws MessagingException {
        //Send out an email notification
        emailSenderService.sendVerificationEmails(requestPlacedEvent);
        log.info("Received notification for Request - {}", requestPlacedEvent);
    }
}

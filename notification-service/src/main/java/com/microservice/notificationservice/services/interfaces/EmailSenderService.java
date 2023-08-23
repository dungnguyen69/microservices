package com.microservice.notificationservice.services.interfaces;

import com.microservice.notificationservice.RequestPlacedEvent;
import jakarta.mail.MessagingException;

public interface EmailSenderService {
    public void sendNotificationEmailsWhenSubmittingRequest(RequestPlacedEvent requestPlacedEvent) throws MessagingException;
}

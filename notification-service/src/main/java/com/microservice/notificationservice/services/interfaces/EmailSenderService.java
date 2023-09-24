package com.microservice.notificationservice.services.interfaces;

import com.microservice.notificationservice.RequestPlacedEvent;
import com.microservice.notificationservice.VerificationEvent;
import jakarta.mail.MessagingException;

public interface EmailSenderService {
    public void sendNotificationEmailsWhenSubmittingRequest(RequestPlacedEvent requestPlacedEvent) throws MessagingException;

    public void sendVerificationEmail(VerificationEvent verificationEvent) throws MessagingException;

}

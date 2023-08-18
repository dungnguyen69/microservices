package com.microservice.notificationservice.services;

import com.microservice.notificationservice.RequestPlacedEvent;
import com.microservice.notificationservice.models.Request;
import com.microservice.notificationservice.models.User;
import com.microservice.notificationservice.services.interfaces.EmailSenderService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderServiceImpl implements EmailSenderService {

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendVerificationEmails(RequestPlacedEvent requestPlacedEvent) throws MessagingException {
        for (Request request : requestPlacedEvent.getRequests()) {
            sendVerificationEmail(request.getAccepter(), request.getRequestId());
            sendVerificationEmail(request.getNextKeeper(), request.getRequestId());
        }
    }

    private void sendVerificationEmail(User user, String requestId) throws MessagingException {
        String toAddress = user.getEmail();
        String subject = "Please verify your registration";
        String content = """
                Dear [[name]],
                <br> The order [[requestId]] status  is PENDING <br>
                <br> Please wait for confirmation<br>
                <br> Thank you! <br>""";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom(fromAddress);
        helper.setTo(toAddress);
        helper.setSubject(subject);
        content = content.replace("[[name]]", user
                .getFirstName()
                .concat(" " + user.getLastName()));
        content = content.replace("[[requestId]]", requestId);
        helper.setText(content, true);
        mailSender.send(message);
    }
}

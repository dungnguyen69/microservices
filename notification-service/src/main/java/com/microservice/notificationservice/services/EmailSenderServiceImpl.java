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
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmailSenderServiceImpl implements EmailSenderService {

    String submitContent = """
            Dear [[name]],
            <br> The order [[requestId]] status  is PENDING <br>
            <br> Please wait for confirmation<br>
            <br> Thank you! <br>""";
    @Value("${spring.mail.username}")
    private String fromAddress;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private WebClient.Builder webClientBuilder;

    @Override
    public void sendNotificationEmailsWhenSubmittingRequest(RequestPlacedEvent requestPlacedEvent) throws MessagingException {
        for (Request request : requestPlacedEvent.getRequests()) {
            sendVerificationEmail(request.getAccepter_Id(), request.getRequestId(), submitContent);
            sendVerificationEmail(request.getNextKeeper_Id(), request.getRequestId(), submitContent);
        }
    }

    private void sendVerificationEmail(int userId, String requestId, String content) throws MessagingException {
        User user = findUserById(userId);
        String toAddress = user.getEmail();
        String subject = "Please verify your registration";
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom(fromAddress);
        helper.setTo(toAddress);
        helper.setSubject(subject);
        content = content.replace("[[name]]", user.getFirstName().concat(" " + user.getLastName()));
        content = content.replace("[[requestId]]", requestId);
        helper.setText(content, true);
        mailSender.send(message);
    }

    private User findUserById(int id) {
        return webClientBuilder
                .build()
                .get()
                .uri("http://user-service/api/users/{id}", id)
                .retrieve()
                .bodyToMono(User.class)
                .block();
    }

}

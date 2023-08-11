package com.fullstack.Backend.services;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fullstack.Backend.dto.users.*;
import com.fullstack.Backend.models.User;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    public CompletableFuture<User> findById(int id);

    public CompletableFuture<Boolean> doesUserExist(int id);

    public CompletableFuture<User> findByUsername(String username);

    public CompletableFuture<ResponseEntity<Object>> authenticateUser(LoginDTO loginRequest, Authentication
            authentication);

    public CompletableFuture<ResponseEntity<Object>> registerUser(RegisterDTO registerRequest, String siteURL) throws
            MessagingException, UnsupportedEncodingException;

    public void sendVerificationEmail(User user, String siteURL) throws
            MessagingException, UnsupportedEncodingException;

    public CompletableFuture<ResponseEntity<Object>> verify(String verificationCode) throws ExecutionException, InterruptedException;

    public CompletableFuture<ResponseEntity<Object>> showUsersWithPaging(int pageIndex, int pageSize, String
            sortBy, String sortDir, FilterUserDTO dto) throws ExecutionException, InterruptedException;

    public CompletableFuture<ResponseEntity<Object>> resendRegistrationToken(String siteURL, String existingToken) throws ExecutionException, InterruptedException, MessagingException;

    public CompletableFuture<ResponseEntity<Object>> sendResetPasswordEmail(String siteURL, String userEmail) throws ExecutionException, InterruptedException, MessagingException;


    public CompletableFuture<ResponseEntity<Object>> saveResetPassword(ResetPasswordDTO dto) throws ExecutionException, InterruptedException, MessagingException;

    public CompletableFuture<ResponseEntity<Object>> saveForgotPassword(ForgotPasswordDTO dto) throws ExecutionException, InterruptedException, MessagingException;

    CompletableFuture<ResponseEntity<Object>> verifyPasswordToken(String token);

    public CompletableFuture<ResponseEntity<Object>> getSuggestKeywordUsers(int fieldColumn, String keyword, FilterUserDTO filter) throws InterruptedException, ExecutionException;

    public CompletableFuture<ResponseEntity<Object>> providePermission(int userId, String permission) throws ExecutionException, InterruptedException;

    public CompletableFuture<ResponseEntity<Object>> updateProfile(ProfileDTO request) throws ExecutionException, InterruptedException;

}

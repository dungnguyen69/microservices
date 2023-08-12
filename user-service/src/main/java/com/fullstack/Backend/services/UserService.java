package com.fullstack.Backend.services;

import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.fullstack.Backend.dto.*;
import com.fullstack.Backend.models.User;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    public Optional<User> findById(int id) throws InterruptedException;

    public Boolean doesUserExist(int id);

    public ResponseEntity<Object> findByUsername(String username);

    public ResponseEntity<Object> authenticateUser(LoginDTO loginRequest, Authentication
            authentication);

    public ResponseEntity<Object> registerUser(RegisterDTO registerRequest, String siteURL) throws
            MessagingException, UnsupportedEncodingException;

    public void sendVerificationEmail(User user, String siteURL) throws
            MessagingException, UnsupportedEncodingException;

    public ResponseEntity<Object> verify(String verificationCode) throws ExecutionException, InterruptedException;

    public ResponseEntity<Object> showUsersWithPaging(int pageIndex, int pageSize, String
            sortBy, String sortDir, FilterUserDTO dto) throws ExecutionException, InterruptedException;

    public ResponseEntity<Object> resendRegistrationToken(String siteURL, String existingToken) throws ExecutionException, InterruptedException, MessagingException;

    public ResponseEntity<Object> sendResetPasswordEmail(String siteURL, String userEmail) throws ExecutionException, InterruptedException, MessagingException;


    public ResponseEntity<Object> saveResetPassword(ResetPasswordDTO dto) throws ExecutionException, InterruptedException, MessagingException;

    public ResponseEntity<Object> saveForgotPassword(ForgotPasswordDTO dto) throws ExecutionException, InterruptedException, MessagingException;

    ResponseEntity<Object> verifyPasswordToken(String token);

    public ResponseEntity<Object> getSuggestKeywordUsers(int fieldColumn, String keyword, FilterUserDTO filter) throws InterruptedException, ExecutionException;

    public ResponseEntity<Object> providePermission(int userId, String permission) throws ExecutionException, InterruptedException;

    public ResponseEntity<Object> updateProfile(ProfileDTO request) throws ExecutionException, InterruptedException;

}

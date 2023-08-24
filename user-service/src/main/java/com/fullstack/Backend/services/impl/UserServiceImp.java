package com.fullstack.Backend.services.impl;

import com.fullstack.Backend.dto.*;
import com.fullstack.Backend.enums.Role;
import com.fullstack.Backend.event.VerificationEvent;
import com.fullstack.Backend.models.PasswordResetToken;
import com.fullstack.Backend.models.SystemRole;
import com.fullstack.Backend.models.User;
import com.fullstack.Backend.models.VerificationToken;
import com.fullstack.Backend.repositories.interfaces.PasswordResetTokenRepository;
import com.fullstack.Backend.repositories.interfaces.SystemRoleRepository;
import com.fullstack.Backend.repositories.interfaces.UserRepository;
import com.fullstack.Backend.repositories.interfaces.VerificationTokenRepository;
import com.fullstack.Backend.responses.users.JwtResponse;
import com.fullstack.Backend.responses.users.KeywordSuggestionResponse;
import com.fullstack.Backend.responses.users.MessageResponse;
import com.fullstack.Backend.responses.users.UsersResponse;
import com.fullstack.Backend.security.JwtUtils;
import com.fullstack.Backend.services.UserService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.fullstack.Backend.constant.constant.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;


@Service
@Slf4j
public class UserServiceImp implements UserService, UserDetailsService {
    @Value("${app.client.baseUrl}")
    String baseUrl;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserRepository _userRepository;
    @Autowired
    private SystemRoleRepository _systemRoleRepository;
    @Autowired
    private VerificationTokenRepository _tokenRepository;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private PasswordResetTokenRepository _passwordResetTokenRepository;
    @Value("${spring.mail.username}")
    private String fromAddress;

    @Autowired
    private KafkaTemplate<String, VerificationEvent> kafkaTemplate;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = _userRepository
                .findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
        return UserDetailsImpl.build(user);
    }

    @Override
    public Optional<User> findById(int id) {
        log.info("User ID: " + id);
        return _userRepository.findById(id);
    }

    @Override
    public User findByUsername(String username) {
        var user = _userRepository.findByUserName(username);
        return user.orElse(null);
    }

    @Override
    public ResponseEntity<Object> authenticateUser(LoginDTO loginRequest, Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);
        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new JwtResponse(userDetails
                        .getUser()
                        .getId(), jwtCookie.getValue(), userDetails.getUsername(), userDetails.getEmail(), roles, userDetails
                        .getUser()
                        .getBadgeId(), userDetails.getUser().getFirstName(), userDetails
                        .getUser()
                        .getLastName(), userDetails.getUser().getPhoneNumber(), userDetails.getUser().getProject()));
    }

    @Override
    @Transactional
    public ResponseEntity<Object> registerUser(RegisterDTO registerRequest, String siteURL) throws MessagingException {
        if(nameExists(registerRequest.getUserName())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: " + registerRequest.getUserName() + " is already taken!"));
        }

        if(emailExists(registerRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: " + registerRequest.getEmail() + " is already in use!"));
        }

        String token = RandomString.make(64);
        /* Create new user's account */
        User user = new User();
        user.setUserName(registerRequest.getUserName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(encoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setProject(null);
        user.setBadgeId(generateBadgeId());
        user.setCreatedDate(new Date());
        user.setEnabled(false);
        Set<SystemRole> roles = new HashSet<>();
        SystemRole userRole = _systemRoleRepository
                .findByName(Role.ROLE_USER.name())
                .orElseThrow(() -> new RuntimeException("Role is not found."));
        roles.add(userRole);
        user.setSystemRoles(roles);
        save(user);
        createVerificationToken(user, token);
        String verifyURL = baseUrl + "/email-verification?token=" + token;
        kafkaTemplate.send("verificationTopic", new VerificationEvent(user,verifyURL));
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @Override
    @Transactional
    public ResponseEntity<Object> verify(String verificationCode) {
        final VerificationToken verificationToken = getVerificationToken(verificationCode);
        MessageResponse messageResponse;
        if(verificationToken == null || verificationToken.getUser().isEnabled()) {
            messageResponse
                    = new MessageResponse("Sorry, we could not verify account. It maybe already verified," + "or verification code is incorrect.");
            messageResponse.setStatus("INVALID");
            return ResponseEntity.badRequest().body(messageResponse);
        }

        User userByToken = verificationToken.getUser();
        Calendar cal = Calendar.getInstance();

        if((verificationToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
            messageResponse = new MessageResponse("Verification code was expired!");
            messageResponse.setStatus("EXPIRED");
            return ResponseEntity.badRequest().body(messageResponse);
        }

        userByToken.setEnabled(true);
        save(userByToken);
        _tokenRepository.delete(verificationToken);
        messageResponse = new MessageResponse("Verify successfully");
        messageResponse.setStatus("VALID");
        return ResponseEntity.ok().body(messageResponse);

    }

    @Override
    public ResponseEntity<Object> showUsersWithPaging(int pageIndex, int pageSize, String sortBy, String sortDir, FilterUserDTO dto) {
        List<UserDTO> usersList = getUserList(dto);
        List<String> projectList = usersList.stream().map(UserDTO::getProject).distinct().toList();
        int totalElements = usersList.size();
        usersList = getPage(usersList, pageIndex, pageSize);
        UsersResponse response = new UsersResponse();
        response.setUsersList(usersList);
        response.setPageNo(pageIndex);
        response.setPageSize(pageSize);
        response.setTotalElements(totalElements);
        response.setTotalPages(getTotalPages(pageSize, totalElements));
        response.setProjectList(projectList);
        return new ResponseEntity<>(response, OK);
    }

    @Override
    public ResponseEntity<Object> resendRegistrationToken(String siteURL, String existingToken)
            throws MessagingException {
        VerificationToken newToken = generateNewVerificationToken(existingToken);
        User user = newToken.getUser();
        String verifyURL = baseUrl + "/email-verification?token=" + newToken.getToken();
        kafkaTemplate.send("verificationTopic", new VerificationEvent(user,verifyURL));
        return ResponseEntity.ok(new MessageResponse("Resent successfully!"));
    }

    @Override
    @Transactional
    public ResponseEntity<Object> sendResetPasswordEmail(String siteURL, String userEmail) throws MessagingException {
        String token = RandomString.make(64);
        User user = findByEmail(userEmail);

        if(user == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email is not valid!"));
        }

        PasswordResetToken existingToken = findUserFromResetPasswordToken(user);
        if(existingToken != null) {
            String
                    newToken
                    = generateResetPasswordToken(existingToken.getToken()); /* Change old token to new token and return it */
            String verifyURL = baseUrl + "/receive-forgot-password?token=" + newToken;
            sendResetPasswordEmail(user, verifyURL);
            return ResponseEntity.ok(new MessageResponse("Sent successfully!"));
        }
        PasswordResetToken myToken = new PasswordResetToken(token, user);
        _passwordResetTokenRepository.save(myToken);
        String verifyURL = baseUrl + "/receive-forgot-password?token=" + myToken.getToken();
        sendResetPasswordEmail(user, verifyURL);
        return ResponseEntity.ok(new MessageResponse("Sent successfully!"));
    }

    @Override
    public ResponseEntity<Object> saveResetPassword(ResetPasswordDTO dto) {
        Optional<User> user = findById(dto.getId());

        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("User is not existent"));
        }

        boolean isOldPasswordSimilarToNewOne = BCrypt.checkpw(dto.getOldPassword(), user.get().getPassword());
        if(!isOldPasswordSimilarToNewOne) /* Compare old password and new password */ {
            return ResponseEntity.badRequest().body(new MessageResponse("Current password is incorrect!"));
        }

        if(!Objects.equals(dto.getNewPassword(), dto.getConfirmPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("New password must be identical to confirm password"));
        }

        changeUserPassword(user.get(), dto.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Changed successfully!"));
    }

    @Override
    @Transactional
    public ResponseEntity<Object> saveForgotPassword(ForgotPasswordDTO dto) {
        User user = findByToken(dto.getToken());
        if(user == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("User is not existent"));
        }

        PasswordResetToken token = _passwordResetTokenRepository.findByToken(dto.getToken());
        if(token == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Token is not existent"));
        }

        if(!Objects.equals(dto.getNewPassword(), dto.getConfirmPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Password must be identical to confirm password"));
        }

        changeUserPassword(user, dto.getNewPassword());
        _passwordResetTokenRepository.delete(token);
        return ResponseEntity.ok(new MessageResponse("Changed successfully!"));
    }

    @Override
    public ResponseEntity<Object> verifyPasswordToken(String token) {
        final Calendar cal = Calendar.getInstance();
        /* Validate token */
        final PasswordResetToken passToken = _passwordResetTokenRepository.findByToken(token);
        MessageResponse messageResponse;
        if(passToken == null) {
            messageResponse = new MessageResponse("User is not valid because token is not existent");
            messageResponse.setStatus("INVALID");
            return ResponseEntity.badRequest().body(messageResponse);
        }

        boolean isTokenExpired = passToken.getExpiryDate().before(cal.getTime());
        if(isTokenExpired) {
            messageResponse = new MessageResponse("User is not valid because token is expired");
            messageResponse.setStatus("EXPIRED");
            return ResponseEntity.badRequest().body(messageResponse);
        }

        messageResponse = new MessageResponse("Verify successfully");
        messageResponse.setStatus("VALID");
        return ResponseEntity.ok().body(messageResponse);
    }

    @Override
    public ResponseEntity<Object> getSuggestKeywordUsers(int fieldColumn, String keyword, FilterUserDTO filter) {
        if(keyword.trim().isBlank()) {
            return ResponseEntity.status(NOT_FOUND).body("Keyword must be non-null");
        }

        List<User> users = _userRepository.findAll();
        List<UserDTO> userList = getAllUser(users, filter);
        Set<String> keywordList = selectColumnForKeywordSuggestion(userList, keyword, fieldColumn);
        KeywordSuggestionResponse response = new KeywordSuggestionResponse();
        response.setKeywordList(keywordList);
        return new ResponseEntity<>(response, OK);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> providePermission(int userId, String permission) {
        User user = findById(userId).orElseThrow(() -> new RuntimeException("User is not found."));
        SystemRole userRole = _systemRoleRepository
                .findByName(permission)
                .orElseThrow(() -> new RuntimeException("Role is not found."));
        Set<SystemRole> roles = new HashSet<>();
        roles.add(userRole);
        user.setSystemRoles(roles);
        save(user);
        return ResponseEntity.ok(new MessageResponse("UPDATED SUCCESSFULLY!"));
    }

    @Override
    @Transactional
    public ResponseEntity<Object> updateProfile(ProfileDTO request) {
        Optional<User> user = findById(request.getId());
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("User does not exist!"));
        }

        user.get().setUserName(request.getUserName());
        user.get().setFirstName(request.getFirstName());
        user.get().setLastName(request.getLastName());
        user.get().setEmail(request.getEmail());
        user.get().setPhoneNumber(request.getPhoneNumber());
        save(user.get());
        return ResponseEntity.ok(new MessageResponse("UPDATED SUCCESSFULLY!"));
    }

    private List<UserDTO> getAllUser(List<User> users, FilterUserDTO filter) {
        formatFilter(filter); /* Remove spaces and make input text become lowercase*/
        users = fetchFilteredUsers(filter, users); /*List of devices after filtering*/
        return convertEntityToDTO(users);
    }

    private List<UserDTO> convertEntityToDTO(List<User> users) {
        return users.stream().map(UserDTO::new).collect(Collectors.toList());
    }

    private Set<String> selectColumnForKeywordSuggestion(List<UserDTO> userList, String keyword, int fieldColumn) {
        Set<String> keywordList = new HashSet<>();
        Stream<String> mappedDeviceList = null;
        switch (fieldColumn) { /*Fetch only one column*/
            case USER_BADGE_ID_COLUMN -> mappedDeviceList = userList.stream().map(UserDTO::getBadgeId);
            case USER_NAME_COLUMN -> mappedDeviceList = userList.stream().map(UserDTO::getUserName);
            case USER_FIRST_NAME_COLUMN -> mappedDeviceList = userList.stream().map(UserDTO::getFirstName);
            case USER_LAST_NAME_COLUMN -> mappedDeviceList = userList.stream().map(UserDTO::getLastName);
            case USER_EMAIL_COLUMN -> mappedDeviceList = userList.stream().map(UserDTO::getEmail);
            case USER_PHONE_NUMBER_COLUMN -> mappedDeviceList = userList.stream().map(UserDTO::getPhoneNumber);
        }
        if(mappedDeviceList != null) {
            keywordList = mappedDeviceList
                    .filter(element -> element.toLowerCase().contains(keyword.strip().toLowerCase()))
                    .limit(20)
                    .collect(Collectors.toSet());
        }
        return keywordList;
    }

    private List<UserDTO> getPage(List<UserDTO> sourceList, int pageIndex, int pageSize) {
        if(pageSize <= 0 || pageIndex <= 0) {
            throw new IllegalArgumentException("invalid page size: " + pageSize);
        }

        int fromIndex = (pageIndex - 1) * pageSize;

        if(sourceList == null || sourceList.size() <= fromIndex) {
            return Collections.emptyList();
        }

        return sourceList.subList(fromIndex, Math.min(fromIndex + pageSize, sourceList.size()));
    }

    private boolean emailExists(String email) {
        return _userRepository.existsByEmail(email);
    }

    private void formatFilter(FilterUserDTO dto) {
        if(dto.getBadgeId() != null) {
            dto.setBadgeId(dto.getBadgeId().trim().toLowerCase());
        }

        if(dto.getUserName() != null) {
            dto.setUserName(dto.getUserName().trim().toLowerCase());
        }

        if(dto.getFirstName() != null) {
            dto.setFirstName(dto.getFirstName().trim().toLowerCase());
        }

        if(dto.getLastName() != null) {
            dto.setLastName(dto.getLastName().trim().toLowerCase());
        }

        if(dto.getEmail() != null) {
            dto.setEmail(dto.getEmail().trim().toLowerCase());
        }

        if(dto.getPhoneNumber() != null) {
            dto.setPhoneNumber(dto.getPhoneNumber().trim().toLowerCase());
        }

        if(dto.getProject() != null) {
            dto.setProject(dto.getProject().trim().toLowerCase());
        }

    }

    private boolean nameExists(String email) {
        return _userRepository.existsByUserName(email);
    }

    private int getTotalPages(int pageSize, int listSize) {
        if(listSize == 0) {
            return 1;
        }

        if(listSize % pageSize == 0) {
            return listSize / pageSize;
        }

        return (listSize / pageSize) + 1;
    }

    private String generateResetPasswordToken(String existingToken) {
        PasswordResetToken rpToken = getResetPasswordToken(existingToken);
        if(rpToken == null) {
            return null;
        }
        rpToken.updateToken();
        rpToken.setToken(RandomString.make(64));
        rpToken = _passwordResetTokenRepository.save(rpToken);
        return rpToken.getToken();
    }

    @Transactional
    private void changeUserPassword(User user, String password) {
        user.setPassword(encoder.encode(password));
        _userRepository.save(user);
    }

    private void sendResetPasswordEmail(User user, String verifyURL) throws MessagingException {
        String toAddress = user.getEmail();
        String subject = "Reset password";
        String
                content
                = "Dear [[name]],<br>" + "Please click the link below to reset your password:<br>" + "<h3><a href=\"[[URL]]\" target=\"_self\">RESET PASSWORD</a></h3>";
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom(fromAddress);
        helper.setTo(toAddress);
        helper.setSubject(subject);
        content = content.replace("[[name]]", user.getFirstName().concat(" " + user.getLastName()));
        content = content.replace("[[URL]]", verifyURL);
        helper.setText(content, true);
        mailSender.send(message);
    }

    private String generateBadgeId() {
        char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        Random random = new Random();
        StringBuilder sb = new StringBuilder((100000 + random.nextInt(900000)) + "-");
        for (int i = 0; i < 5; i++)
            sb.append(chars[random.nextInt(chars.length)]);
        return sb.toString();
    }

    private List<User> fetchFilteredUsers(FilterUserDTO dto, List<User> users) {
        if(dto.getBadgeId() != null) {
            users = users
                    .stream()
                    .filter(user -> user.getBadgeId().equalsIgnoreCase(dto.getBadgeId()))
                    .collect(Collectors.toList());
        }
        if(dto.getUserName() != null) {
            users = users
                    .stream()
                    .filter(user -> user.getUserName().equalsIgnoreCase(dto.getUserName()))
                    .collect(Collectors.toList());
        }
        if(dto.getFirstName() != null) {
            users = users
                    .stream()
                    .filter(user -> user.getFirstName().equalsIgnoreCase(dto.getFirstName()))
                    .collect(Collectors.toList());
        }
        if(dto.getLastName() != null) {
            users = users
                    .stream()
                    .filter(user -> user.getLastName().equalsIgnoreCase(dto.getLastName()))
                    .collect(Collectors.toList());
        }
        if(dto.getEmail() != null) {
            users = users
                    .stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(dto.getEmail()))
                    .collect(Collectors.toList());
        }
        if(dto.getPhoneNumber() != null) {
            users = users
                    .stream()
                    .filter(user -> user.getPhoneNumber().equalsIgnoreCase(dto.getPhoneNumber()))
                    .collect(Collectors.toList());
        }
        if(dto.getProject() != null) {
            users = users
                    .stream()
                    .filter(user -> user.getProject().equalsIgnoreCase(dto.getProject()))
                    .collect(Collectors.toList());
        }
        return users;
    }

    @Transactional
    private void save(User user) {
        _userRepository.save(user);
    }

    @Transactional
    private void createVerificationToken(User user, String token) {
        VerificationToken myToken = new VerificationToken(token, user);
        _tokenRepository.save(myToken);
    }

    private VerificationToken getVerificationToken(String VerificationToken) {
        return _tokenRepository.findByToken(VerificationToken);
    }

    private PasswordResetToken getResetPasswordToken(String token) {
        return _passwordResetTokenRepository.findByToken(token);
    }

    private User findByEmail(String email) {
        return _userRepository.findByEmail(email);
    }

    private User findByToken(String token) {
        if(_passwordResetTokenRepository.findByToken(token) == null) {
            return null;
        }
        return _passwordResetTokenRepository.findByToken(token).getUser();
    }

    private PasswordResetToken findUserFromResetPasswordToken(User user) {
        return _passwordResetTokenRepository.findByUser(user);
    }

    private List<UserDTO> getUserList(FilterUserDTO dto) {
        formatFilter(dto);
        List<User> users = _userRepository.findAll();
        users = fetchFilteredUsers(dto, users);
        return users.stream().map(UserDTO::new).collect(Collectors.toList());
    }

    @Transactional
    private VerificationToken generateNewVerificationToken(String existingVerificationToken) {
        VerificationToken vToken = getVerificationToken(existingVerificationToken);
        vToken.updateToken(RandomString.make(64));
        _tokenRepository.save(vToken);
        return vToken;
    }
}

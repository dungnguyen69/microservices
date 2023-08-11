package com.fullstack.Backend.services.impl;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fullstack.Backend.dto.users.*;
import com.fullstack.Backend.models.*;
import com.fullstack.Backend.enums.Role;
import com.fullstack.Backend.repositories.interfaces.PasswordResetTokenRepository;
import com.fullstack.Backend.repositories.interfaces.SystemRoleRepository;
import com.fullstack.Backend.repositories.interfaces.VerificationTokenRepository;
import com.fullstack.Backend.responses.device.KeywordSuggestionResponse;
import com.fullstack.Backend.responses.users.JwtResponse;
import com.fullstack.Backend.responses.users.MessageResponse;
import com.fullstack.Backend.responses.users.UsersResponse;
import com.fullstack.Backend.security.JwtUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import net.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.fullstack.Backend.repositories.interfaces.UserRepository;
import com.fullstack.Backend.services.UserService;

import static com.fullstack.Backend.constant.constant.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;


@Service
@CacheConfig(cacheNames = {"user"})
public class UserServiceImp implements UserService, UserDetailsService {
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

    @Value("${app.client.baseUrl}")
    String baseUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = _userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
        return UserDetailsImpl.build(user);
    }

    @Async
    @Override
    public CompletableFuture<User> findById(int id) {
        return CompletableFuture.completedFuture(_userRepository.findById(id));
    }

    @Async
    @Override
    public CompletableFuture<Boolean> doesUserExist(int id) {
        return CompletableFuture.completedFuture(_userRepository.existsById((long) id));
    }

    @Async
    @Override
    public CompletableFuture<User> findByUsername(String username) {
        return CompletableFuture.completedFuture(_userRepository.findByUserName(username).orElse(null));
    }

    @Async
    @Override
    public CompletableFuture<ResponseEntity<Object>> authenticateUser(LoginDTO loginRequest, Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return CompletableFuture.completedFuture(ResponseEntity.ok().header(
                HttpHeaders.SET_COOKIE, jwtCookie.toString()).body(new JwtResponse(
                userDetails.getUser().getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles,
                userDetails.getUser().getBadgeId(),
                userDetails.getUser().getFirstName(),
                userDetails.getUser().getLastName(),
                userDetails.getUser().getPhoneNumber(),
                userDetails.getUser().getProject())));
    }

    @Async
    @Override
    @Transactional
    public CompletableFuture<ResponseEntity<Object>> registerUser(RegisterDTO registerRequest, String siteURL) throws MessagingException {
        if (nameExists(registerRequest.getUserName())) {
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: " + registerRequest.getUserName() + " is already taken!")));
        }

        if (emailExists(registerRequest.getEmail())) {
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: " + registerRequest.getEmail() + " is already in use!")));
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
        SystemRole userRole = _systemRoleRepository.findByName(Role.ROLE_USER.name())
                .orElseThrow(() -> new RuntimeException("Role is not found."));
        roles.add(userRole);
        user.setSystemRoles(roles);
        save(user);
        createVerificationToken(user, token);
        String verifyURL = baseUrl + "/email-verification?token=" + token;
        sendVerificationEmail(user, verifyURL);
        return CompletableFuture.completedFuture(ResponseEntity.ok(new
                MessageResponse("User registered successfully!")));
    }

    @Async
    @Override
    public void sendVerificationEmail(User user, String verifyURL) throws MessagingException {
        String toAddress = user.getEmail();
        String subject = "Please verify your registration";
        String content = "Dear [[name]],<br>"
                + "Please click the link below to verify your registration:<br>"
                + "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY</a></h3>"
                + "Thank you!<br>";

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

    @Async
    @Override
    @Transactional
    public CompletableFuture<ResponseEntity<Object>> verify(String verificationCode) {
        final VerificationToken verificationToken = getVerificationToken(verificationCode);
        MessageResponse messageResponse;
        if (verificationToken == null || verificationToken.getUser().isEnabled()) {
            messageResponse = new MessageResponse("Sorry, we could not verify account. It maybe already verified," +
                    "or verification code is incorrect.");
            messageResponse.setStatus("INVALID");
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(messageResponse));
        }

        User userByToken = verificationToken.getUser();
        Calendar cal = Calendar.getInstance();

        if ((verificationToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
            messageResponse = new MessageResponse("Verification code was expired!");
            messageResponse.setStatus("EXPIRED");
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(messageResponse));
        }

        userByToken.setEnabled(true);
        save(userByToken);
        _tokenRepository.delete(verificationToken);
        messageResponse = new MessageResponse("Verify successfully");
        messageResponse.setStatus("VALID");
        return CompletableFuture.completedFuture(ResponseEntity.ok().body(messageResponse));

    }

    @Async
    @Override
    public CompletableFuture<ResponseEntity<Object>> showUsersWithPaging(int pageIndex, int pageSize, String sortBy, String sortDir, FilterUserDTO dto) throws ExecutionException, InterruptedException {
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
        return CompletableFuture.completedFuture(new ResponseEntity<>(response, OK));
    }

    @Async
    @Override
    public CompletableFuture<ResponseEntity<Object>> resendRegistrationToken(String siteURL, String existingToken) throws ExecutionException, InterruptedException, MessagingException {
        VerificationToken newToken = generateNewVerificationToken(existingToken);
        User user = newToken.getUser();
        String verifyURL = baseUrl + "/email-verification?token=" + newToken.getToken();
        resendVerificationEmail(user, verifyURL);
        return CompletableFuture.completedFuture(ResponseEntity.ok(new MessageResponse("Resent successfully!")));
    }

    @Async
    @Override
    @Transactional
    public CompletableFuture<ResponseEntity<Object>> sendResetPasswordEmail(String siteURL, String userEmail) throws ExecutionException, InterruptedException, MessagingException {
        String token = RandomString.make(64);
        User user = findByEmail(userEmail);

        if (user == null)
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Email is not valid!")));

        PasswordResetToken existingToken = findUserFromResetPasswordToken(user);
        if (existingToken != null) {
            String newToken = generateResetPasswordToken(existingToken.getToken()); /* Change old token to new token and return it */
            String verifyURL = baseUrl + "/receive-forgot-password?token=" + newToken;
            sendResetPasswordEmail(user, verifyURL);
            return CompletableFuture.completedFuture(ResponseEntity.ok(new MessageResponse("Sent successfully!")));
        }
        PasswordResetToken myToken = new PasswordResetToken(token, user);
        _passwordResetTokenRepository.save(myToken);
        String verifyURL = baseUrl + "/receive-forgot-password?token=" + myToken.getToken();
        sendResetPasswordEmail(user, verifyURL);
        return CompletableFuture.completedFuture(ResponseEntity.ok(new MessageResponse("Sent successfully!")));
    }

    @Async
    @Override
    public CompletableFuture<ResponseEntity<Object>> saveResetPassword(ResetPasswordDTO dto) throws ExecutionException, InterruptedException, MessagingException {
        CompletableFuture<User> user = findById(dto.getId());

        if (user == null)
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("User is not existent")));

        boolean isOldPasswordSimilarToNewOne = BCrypt.checkpw(dto.getOldPassword(), user.get().getPassword());
        if (!isOldPasswordSimilarToNewOne) /* Compare old password and new password */
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Current password is incorrect!")));

        if (!Objects.equals(dto.getNewPassword(), dto.getConfirmPassword())) {
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("New password must be identical to confirm password")));
        }

        changeUserPassword(user.get(), dto.getNewPassword());
        return CompletableFuture.completedFuture(ResponseEntity.ok(new MessageResponse("Changed successfully!")));
    }

    @Async
    @Override
    @Transactional
    public CompletableFuture<ResponseEntity<Object>> saveForgotPassword(ForgotPasswordDTO dto) throws ExecutionException, InterruptedException, MessagingException {
        User user = findByToken(dto.getToken());
        if (user == null)
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("User is not existent")));

        PasswordResetToken token = _passwordResetTokenRepository.findByToken(dto.getToken());
        if (token == null)
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Token is not existent")));

        if (!Objects.equals(dto.getNewPassword(), dto.getConfirmPassword()))
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Password must be identical to confirm password")));

        changeUserPassword(user, dto.getNewPassword());
        _passwordResetTokenRepository.delete(token);
        return CompletableFuture.completedFuture(ResponseEntity
                .ok(new MessageResponse("Changed successfully!")));
    }

    @Async
    @Override
    public CompletableFuture<ResponseEntity<Object>> verifyPasswordToken(String token) {
        final Calendar cal = Calendar.getInstance();
        /* Validate token */
        final PasswordResetToken passToken = _passwordResetTokenRepository.findByToken(token);
        MessageResponse messageResponse;
        if (passToken == null) {
            messageResponse = new MessageResponse("User is not valid because token is not existent");
            messageResponse.setStatus("INVALID");
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(messageResponse));
        }

        boolean isTokenExpired = passToken.getExpiryDate().before(cal.getTime());
        if (isTokenExpired) {
            messageResponse = new MessageResponse("User is not valid because token is expired");
            messageResponse.setStatus("EXPIRED");
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(messageResponse));
        }

        messageResponse = new MessageResponse("Verify successfully");
        messageResponse.setStatus("VALID");
        return CompletableFuture.completedFuture(ResponseEntity
                .ok()
                .body(messageResponse));
    }

    @Async
    @Override
    public CompletableFuture<ResponseEntity<Object>> getSuggestKeywordUsers(int fieldColumn, String keyword, FilterUserDTO filter) throws InterruptedException, ExecutionException {
        if (keyword.trim().isBlank())
            return CompletableFuture.completedFuture(ResponseEntity.status(NOT_FOUND).body("Keyword must be non-null"));

        List<User> users = _userRepository.findAll();
        List<UserDTO> deviceList = getAllUser(users, filter);
        Set<String> keywordList = selectColumnForKeywordSuggestion(deviceList, keyword, fieldColumn);
        KeywordSuggestionResponse response = new KeywordSuggestionResponse();
        response.setKeywordList(keywordList);
        return CompletableFuture.completedFuture(new ResponseEntity<>(response, OK));
    }

    @Async
    @Override
    @Transactional
    public CompletableFuture<ResponseEntity<Object>> providePermission(int userId, String permission) throws ExecutionException, InterruptedException {
        User user = findById(userId).get();
        SystemRole userRole = _systemRoleRepository.findByName(permission).orElseThrow(() -> new RuntimeException("Role is not found."));
        Set<SystemRole> roles = new HashSet<>();
        roles.add(userRole);
        user.setSystemRoles(roles);
        save(user);
        return CompletableFuture.completedFuture(ResponseEntity.ok(new MessageResponse("UPDATED SUCCESSFULLY!")));
    }

    @Async
    @Override
    @Transactional
    public CompletableFuture<ResponseEntity<Object>> updateProfile(ProfileDTO request) throws ExecutionException, InterruptedException {
        User user = findById(request.getId()).get();
        if (user == null)
            return CompletableFuture.completedFuture(ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("User does not exist!")));

        user.setUserName(request.getUserName());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        save(user);
        return CompletableFuture.completedFuture(ResponseEntity.ok(new MessageResponse("UPDATED SUCCESSFULLY!")));
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
            case USER_BADGE_ID_COLUMN -> mappedDeviceList = userList.stream()
                    .map(UserDTO::getBadgeId);
            case USER_NAME_COLUMN -> mappedDeviceList = userList.stream()
                    .map(UserDTO::getUserName);
            case USER_FIRST_NAME_COLUMN -> mappedDeviceList = userList.stream()
                    .map(UserDTO::getFirstName);
            case USER_LAST_NAME_COLUMN -> mappedDeviceList = userList.stream()
                    .map(UserDTO::getLastName);
            case USER_EMAIL_COLUMN -> mappedDeviceList = userList.stream()
                    .map(UserDTO::getEmail);
            case USER_PHONE_NUMBER_COLUMN -> mappedDeviceList = userList.stream()
                    .map(UserDTO::getPhoneNumber);
        }
        if (mappedDeviceList != null) {
            keywordList =
                    mappedDeviceList
                            .filter(element -> element
                                    .toLowerCase()
                                    .contains(keyword
                                            .strip()
                                            .toLowerCase()))
                            .limit(20)
                            .collect(Collectors.toSet());
        }
        return keywordList;
    }

    private List<UserDTO> getPage(List<UserDTO> sourceList, int pageIndex, int pageSize) {
        if (pageSize <= 0 || pageIndex <= 0) throw new IllegalArgumentException("invalid page size: " + pageSize);

        int fromIndex = (pageIndex - 1) * pageSize;

        if (sourceList == null || sourceList.size() <= fromIndex)
            return Collections.emptyList();

        return sourceList.subList(fromIndex, Math.min(fromIndex + pageSize, sourceList.size()));
    }

    private boolean emailExists(String email) {
        return _userRepository.existsByEmail(email);
    }

    private void formatFilter(FilterUserDTO dto) {
        if (dto.getBadgeId() != null) dto.setBadgeId(dto.getBadgeId().trim().toLowerCase());

        if (dto.getUserName() != null)
            dto.setUserName(dto.getUserName().trim().toLowerCase());

        if (dto.getFirstName() != null)
            dto.setFirstName(dto.getFirstName().trim().toLowerCase());

        if (dto.getLastName() != null) dto.setLastName(dto.getLastName().trim().toLowerCase());

        if (dto.getEmail() != null)
            dto.setEmail(dto.getEmail().trim().toLowerCase());

        if (dto.getPhoneNumber() != null)
            dto.setPhoneNumber(dto.getPhoneNumber().trim().toLowerCase());

        if (dto.getProject() != null)
            dto.setProject(dto.getProject().trim().toLowerCase());

    }

    private boolean nameExists(String email) {
        return _userRepository.existsByUserName(email);
    }

    private int getTotalPages(int pageSize, int listSize) {
        if (listSize == 0) return 1;

        if (listSize % pageSize == 0) return listSize / pageSize;

        return (listSize / pageSize) + 1;
    }

    private void resendVerificationEmail(User user, String verifyURL) throws MessagingException {
        String toAddress = user.getEmail();
        String subject = "Resend Verification Email";
        String content = "Dear [[name]],<br>"
                + "Please click the link below to verify your registration:<br>"
                + "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY</a></h3>"
                + "Thank you!<br>";

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

    private String generateResetPasswordToken(String existingToken) throws ExecutionException, InterruptedException {
        PasswordResetToken rpToken = getResetPasswordToken(existingToken);
        if (rpToken == null)
            return null;
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

    @Transactional
    private void createPasswordResetTokenForUser(User user, String token) {
        PasswordResetToken myToken = new PasswordResetToken(token, user);
        _passwordResetTokenRepository.save(myToken);
    }

    private void sendResetPasswordEmail(User user, String verifyURL) throws MessagingException {
        String toAddress = user.getEmail();
        String subject = "Reset password";
        String content = "Dear [[name]],<br>"
                + "Please click the link below to reset your password:<br>"
                + "<h3><a href=\"[[URL]]\" target=\"_self\">RESET PASSWORD</a></h3>";
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
        if (dto.getBadgeId() != null)
            users
                    = users
                    .stream()
                    .filter(user -> user
                            .getBadgeId()
                            .equalsIgnoreCase(dto.getBadgeId()))
                    .collect(Collectors.toList());
        if (dto.getUserName() != null)
            users
                    = users
                    .stream()
                    .filter(user -> user
                            .getUserName()
                            .equalsIgnoreCase(dto.getUserName()))
                    .collect(Collectors.toList());
        if (dto.getFirstName() != null)
            users
                    = users
                    .stream()
                    .filter(user -> user
                            .getFirstName()
                            .equalsIgnoreCase(dto.getFirstName()))
                    .collect(Collectors.toList());
        if (dto.getLastName() != null)
            users
                    = users
                    .stream()
                    .filter(user -> user
                            .getLastName()
                            .equalsIgnoreCase(dto.getLastName()))
                    .collect(Collectors.toList());
        if (dto.getEmail() != null)
            users
                    = users
                    .stream()
                    .filter(user -> user
                            .getEmail()
                            .equalsIgnoreCase(dto.getEmail()))
                    .collect(Collectors.toList());
        if (dto.getPhoneNumber() != null)
            users
                    = users
                    .stream()
                    .filter(user -> user
                            .getPhoneNumber()
                            .equalsIgnoreCase(dto.getPhoneNumber()))
                    .collect(Collectors.toList());
        if (dto.getProject() != null)
            users
                    = users
                    .stream()
                    .filter(user -> user
                            .getProject()
                            .equalsIgnoreCase(dto.getProject()))
                    .collect(Collectors.toList());
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
        if (_passwordResetTokenRepository.findByToken(token) == null) {
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

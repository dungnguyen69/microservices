package com.fullstack.Backend.controllers;

import com.fullstack.Backend.dto.*;
import com.fullstack.Backend.models.User;
import com.fullstack.Backend.responses.users.JwtResponse;
import com.fullstack.Backend.responses.users.KeywordSuggestionResponse;
import com.fullstack.Backend.responses.users.MessageResponse;
import com.fullstack.Backend.responses.users.UsersResponse;
import com.fullstack.Backend.services.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;

import static com.fullstack.Backend.constant.constant.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final String admin = "hasRole('ADMIN')";
    private final String allUsers = "hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')";
    private final String badRequest = "Invalid request";
    private final String fieldColumnDescription = """
            BADGE_ID = 0 \n
            NAME = 1 \n
            FIRST_NAME = 2 \n
            LAST_NAME = 3 \n
            EMAIL = 4 \n
            PHONE_NUMBER = 5 \n
            """;
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserService _userService;

    @GetMapping("/user")
    @ApiOperation(value = "", notes = "REMOTE CALL in microservice: Get User Details by Name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
            })
    })
    public User getUserByName(@Parameter(description = "User name") @RequestParam String name) {
        return _userService.findByUsername(name);
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "REMOTE CALL in microservice: Get User Details by Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
            })
    })
    public ResponseEntity<User> getUserById(@Parameter(description = "User Id") @PathVariable(value = "id") int id)
            throws InterruptedException {
        return ResponseEntity.ok().body(_userService.findById(id).orElse(null));
    }

    @PostMapping("/login")
    @Operation(summary = "Login (Must be used before invoking another requests)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class))
            })
    })
    public ResponseEntity<Object> authenticateUser(@RequestBody(description = "Enter fields to login") @Valid
                                                   @org.springframework.web.bind.annotation.RequestBody
                                                   LoginDTO loginRequest) {
        /* If the authentication process is successful,
        we can get User’s information such as username, password,
            authorities from an Authentication object. */
        /* gets {username, password} from login Request, AuthenticationManager will use it to authenticate a login account */

        Authentication
                authentication
                = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        return _userService.authenticateUser(loginRequest, authentication);
    }

    @PostMapping("/register")
    @Operation(summary = "Register")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Register successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))
            }), @ApiResponse(responseCode = "400", description = badRequest, content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> registerUser(@Valid @RequestBody(description = "Enter fields to register")
                                               @org.springframework.web.bind.annotation.RequestBody
                                               RegisterDTO registerRequest, HttpServletRequest request)
            throws MessagingException, UnsupportedEncodingException {
        return _userService.registerUser(registerRequest, getSiteURL(request));
    }

    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify token in email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verified successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))
            }), @ApiResponse(responseCode = "400", description = "Token is verified, expired or incorrect", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> verifyUser(@NotEmpty @Parameter(description = "Enter token to verify") @RequestParam
                                             String token) throws ExecutionException, InterruptedException {
        return _userService.verify(token);
    }

    @GetMapping
    @PreAuthorize(admin)
    @Operation(summary = "Retrieve a list of users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieved successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = UsersResponse.class))
            })
    })
    public ResponseEntity<Object> getUsers(
            @CookieValue(name = "cookie") String cookie,
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(defaultValue = DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(defaultValue = DEFAULT_SORT_DIRECTION, required = false) String sortDir, FilterUserDTO dto)
            throws ExecutionException, InterruptedException {
        return _userService.showUsersWithPaging(pageNo, pageSize, sortBy, sortDir, dto);
    }

    @PostMapping("/resendRegistrationToken")
    @Operation(summary = "Resend email for verification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resent successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))
            })
    })
    public ResponseEntity<Object> resendRegistrationToken(HttpServletRequest request,
                                                          @Parameter(description = "Enter token to verify")
                                                          @RequestParam String existingToken)
            throws ExecutionException, InterruptedException, MessagingException {
        return _userService.resendRegistrationToken(getSiteURL(request), existingToken);
    }

    /*Send email*/
    @PostMapping("/reset_password")
    @Operation(summary = "Send email when forgetting password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Send successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))
            })
    })
    public ResponseEntity<Object> sendResetPasswordEmail(HttpServletRequest request, @NotNull @RequestParam("email")
    String userEmail) throws ExecutionException, InterruptedException, MessagingException {
        return _userService.sendResetPasswordEmail(getSiteURL(request), userEmail);
    }

    /* For reset password and forgot password   */
    @PostMapping("/verify_reset_password_token")
    @Operation(summary = "Verify token for changing password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verified successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))
            }), @ApiResponse(responseCode = "400", description = "Token is verified, expired or incorrect", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> showChangePasswordPage(@NotNull @Parameter(description = "Enter token to verify")
                                                         @RequestParam String token) {
        return _userService.verifyPasswordToken(token);
    }

    @PutMapping("/save_reset_password")
    @PreAuthorize(allUsers)
    @Operation(summary = "Save reset password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saved successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))
            }), @ApiResponse(responseCode = "400", description = "Bad Request", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> saveResetPassword(@Valid @RequestBody(description = "Enter fields to save")
                                                    @org.springframework.web.bind.annotation.RequestBody
                                                    ResetPasswordDTO dto)
            throws ExecutionException, InterruptedException, MessagingException {
        return _userService.saveResetPassword(dto);
    }

    @PutMapping("/save_forgot_password")
    @Operation(summary = "Save forgot password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saved successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))
            }), @ApiResponse(responseCode = "400", description = "Bad Request", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> saveForgotPassword(@Valid @RequestBody(description = "Enter fields to save")
                                                     @org.springframework.web.bind.annotation.RequestBody
                                                     ForgotPasswordDTO dto)
            throws ExecutionException, InterruptedException, MessagingException {
        return _userService.saveForgotPassword(dto);
    }

    @PutMapping("/authorization")
    @PreAuthorize(admin)
    @ResponseBody
    @Operation(summary = "Provide permission for users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))
            }), @ApiResponse(responseCode = "400", description = "User or Role is not found.", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> providePermission(
            @Parameter(description = "User Id") @RequestParam(name = "userId") int userId,
            @RequestParam(name = "permission") String permission) throws InterruptedException, ExecutionException {
        return _userService.providePermission(userId, permission);
    }

    @GetMapping("/suggestion")
    @ResponseBody
    @PreAuthorize(allUsers)
    @Operation(summary = "Retrieve a list of keywords")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KeywordSuggestionResponse.class))
            }), @ApiResponse(responseCode = "404", description = "Keyword is null", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> getSuggestKeywordUser(
            @Parameter(description = fieldColumnDescription) @RequestParam(name = "column") int fieldColumn,
            @RequestParam(name = "keyword") String keyword,
            @Parameter(description = "Enter fields to filter or leave it empty")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) FilterUserDTO filter)
            throws InterruptedException, ExecutionException {
        return _userService.getSuggestKeywordUsers(fieldColumn, keyword, filter);
    }

    @PutMapping("/update_profile")
    @PreAuthorize(allUsers)
    @Operation(summary = "Update user profile (Retrieve sample data from login output)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))
            }), @ApiResponse(responseCode = "400", description = "User does not exist!", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> updateProfile(@Valid @RequestBody(description = "Get user info after login to update")
                                                @org.springframework.web.bind.annotation.RequestBody ProfileDTO request)
            throws ExecutionException, InterruptedException {
        return _userService.updateProfile(request);
    }
}

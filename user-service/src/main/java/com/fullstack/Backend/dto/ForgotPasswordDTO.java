package com.fullstack.Backend.dto;

import lombok.Data;

@Data
public class ForgotPasswordDTO {
    private  String token;

    private String newPassword;

    private String confirmPassword;
}

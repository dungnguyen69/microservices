package com.fullstack.Backend.dto;

import lombok.Data;

@Data
public class ResetPasswordDTO {
    private String oldPassword;

    private  int id;

//    @ValidPassword
    private String newPassword;

//    @ValidPassword
    private String confirmPassword;
}

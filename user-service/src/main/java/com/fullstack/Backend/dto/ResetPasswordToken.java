package com.fullstack.Backend.dto;

import com.fullstack.Backend.models.User;
import lombok.Data;

import java.util.Date;

@Data
public class ResetPasswordToken {
    private Long id;
    private String token;

    private User user;

    private Date expiryDate;
}

package com.fullstack.Backend.dto;

import com.fullstack.Backend.validation.annotations.ValidEmail;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {
    private int id;
    @NotEmpty
    private String userName;
    @NotEmpty
    private String firstName;
    @NotEmpty
    private String lastName;
    @NotEmpty
    @ValidEmail
    private String email;
    @NotEmpty
    private String phoneNumber;
}

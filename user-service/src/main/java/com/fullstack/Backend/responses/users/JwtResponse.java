package com.fullstack.Backend.responses.users;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class JwtResponse {
    private int id;

    private String badgeId;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private String project;

    private String isEnable;

    private String accessToken;

    private List<String> roles;

    public JwtResponse(int id,String accessToken, String username, String email, List<String> roles, String badgeId, String firstName, String lastName, String phoneNumber, String project) {
        this.id = id;
        this.accessToken = accessToken;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.badgeId = badgeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.project = project;

    }

}

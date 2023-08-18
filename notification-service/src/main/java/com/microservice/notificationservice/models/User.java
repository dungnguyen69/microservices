package com.microservice.notificationservice.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microservice.notificationservice.validation.annotations.ValidEmail;
import jakarta.persistence.*;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "userName"),
                @UniqueConstraint(columnNames = "email")})
public class User extends BaseEntity {
    @Column(nullable = false)
    private String badgeId;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 20)
    private String userName;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 20)
    private String firstName;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 20)
    private String lastName;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 50)
    @ValidEmail
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = true)
    private String project;

    private boolean enabled;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "systemRoles_Id", nullable = false, foreignKey = @ForeignKey(name = "systemRoles_Id_FK"))
    private Set<SystemRole> systemRoles = new HashSet<>();
}

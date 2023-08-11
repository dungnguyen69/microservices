package com.fullstack.Backend.models;

import lombok.Data;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.util.Calendar;
import java.util.Date;

@Data
@Entity()
@NoArgsConstructor
@Table(name = "VerificationToken")
public class VerificationToken {
    private static final int EXPIRATION = 60 * 24; /* Last only 24h */

    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    private Date expiryDate;

    private Date calculateExpiryDate(final int expiryTimeInMinutes) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(new Date().getTime());
        cal.add(Calendar.MINUTE, expiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
    }

    public void updateToken(final String token) {
        this.token = token;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    public VerificationToken(final String token, final User user) {
        super();
        this.token = token;
        this.user = user;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

}

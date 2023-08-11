package com.fullstack.Backend.repositories.interfaces;

import com.fullstack.Backend.models.PasswordResetToken;
import com.fullstack.Backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.stream.Stream;

@Repository

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>{

    PasswordResetToken findByToken(String token);

    PasswordResetToken findByUser(User user);

    Stream<PasswordResetToken> findAllByExpiryDateLessThan(Date now);

    void deleteByExpiryDateLessThan(Date now);

}

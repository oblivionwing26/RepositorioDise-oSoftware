package edu.esi.ds.esiusuarios.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esiusuarios.model.PasswordResetToken;

public interface PasswordResetTokenDao extends JpaRepository<PasswordResetToken, Long> {
    List<PasswordResetToken> findByUsedFalseAndExpiresAtAfter(Instant now);
}
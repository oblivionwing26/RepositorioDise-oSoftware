package edu.esi.ds.esiusuarios.dao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.esi.ds.esiusuarios.model.User;

public interface UserDao extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
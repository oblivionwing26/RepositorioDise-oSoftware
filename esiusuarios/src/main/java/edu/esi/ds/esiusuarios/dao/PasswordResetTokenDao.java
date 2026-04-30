package edu.esi.ds.esiusuarios.model;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.esi.ds.esiusuarios.model.PasswordResetToken;

public interface PasswordResetTokenDao extends JpaRepository<PasswordResetToken, Long> {
    //(No hace falta un método para buscar por tokenHash porque el token se busca por su hash en el servicio, no directamente en la base de datos)
}


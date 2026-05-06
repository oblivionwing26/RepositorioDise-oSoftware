package edu.esi.ds.esiusuarios.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    //JavaMailSender lo crea spring automáticamente cuando spring.mail.host está configurado
    //Este required = false es para probar en consola

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.email.from:}")
    private String from;

    @Value("${app.reset.expiration-minutes:15}")
    private long resetExpirationMinutes;

    //Envía el token de recuperacion al correo solicitante, si SMPT no está configurado,
    //se muestra el token en consola, si lo está el flujo sigue con normalidad}

    public void sendResetEmail(String to, String token){
        String subject = "Recuperación de contraseña - ESI Usuarios";
        String body = "Hola,\n\n" 
            + "Has solicitado restablecer tu contrasena en ESI Entradas.\n"
            + "Usa este token (valido durante unos minutos) en la pantalla de 'Restablecer contrasena':\n\n"
            + token + "\n\n"
            + "El token es valido por " + resetExpirationMinutes + " minutos.\n\n";

            if (mailSender == null || from == null || from.isBlank()) {
            log.warn("SMTP no configurado (spring.mail.* / app.mail.from). Mostrando email por consola.");
            log.info("=== EMAIL RECUPERACION (modo consola) ===");
            log.info("Para:    {}", to);
            log.info("Asunto:  {}", subject);
            log.info("Cuerpo:\n{}", body);
            log.info("=========================================");
            return;
            }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email de recuperacion enviado a {}", to);
        } catch (Exception ex) {
            // No relanzamos: el endpoint /forgot-password debe seguir devolviendo 200
            // para no revelar si el email existe (anti user-enumeration).
            log.error("Fallo al enviar email a {}: {}", to, ex.getMessage());
        }
    }
}
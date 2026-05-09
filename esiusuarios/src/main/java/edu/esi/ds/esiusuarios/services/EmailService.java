package edu.esi.ds.esiusuarios.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    //JavaMailSender lo crea spring automáticamente cuando spring.mail.host está configurado
    //Este required = false es para probar en consola

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:${app.email.from:}}")
    private String from;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @Value("${app.reset.expiration-minutes:15}")
    private long resetExpirationMinutes;

    @Value("${app.reset.frontend-url:http://localhost:4000/reset-password}")
    private String resetFrontendUrl;

    // Envia el token de recuperacion al correo solicitante. Si SMTP no esta configurado,
    // no se imprime el token en consola por seguridad.

    public void sendResetEmail(String to, String token){
        String subject = "Recuperación de contraseña - ESI Usuarios";
        String resetLink = UriComponentsBuilder
            .fromUriString(resetFrontendUrl)
            .queryParam("token", token)
            .toUriString();

        String body = "Hola,\n\n"
            + "Has solicitado restablecer tu contrasena en ESI Entradas.\n"
            + "Abre este enlace para elegir una nueva contrasena:\n\n"
            + resetLink + "\n\n"
            + "Si el enlace no funciona, copia este token en la pantalla de restablecimiento:\n\n"
            + token + "\n\n"
            + "El token es valido por " + resetExpirationMinutes + " minutos.\n\n";

        if (!smtpConfigurado()) {
            log.warn("SMTP no configurado correctamente. No se envio email de recuperacion a {}.", to);
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            String remitente = remitente();
            if (!remitente.isBlank()) {
                msg.setFrom(remitente);
            }
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email de recuperacion enviado a {}", to);
        } catch (Exception ex) {
            // No relanzamos: el endpoint /forgot-password debe seguir devolviendo 200
            // para no revelar si el email existe (anti user-enumeration).
            log.error("Fallo al enviar email a {}: {}", to, ex.getMessage());
            log.warn("No se muestra el token de recuperacion por consola.");
        }
    }

    private boolean smtpConfigurado() {
        return mailSender != null
            && smtpHost != null && !smtpHost.isBlank()
            && smtpUsername != null && !smtpUsername.isBlank()
            && smtpPassword != null && !smtpPassword.isBlank();
    }

    private String remitente() {
        if (from != null && !from.isBlank()) {
            return from;
        }
        return smtpUsername == null ? "" : smtpUsername;
    }
}
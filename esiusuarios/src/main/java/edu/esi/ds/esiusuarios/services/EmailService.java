package edu.esi.ds.esiusuarios.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.mail.internet.MimeMessage;

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

        String textBody = "Hola,\n\n"
            + "Has solicitado restablecer tu contrasena en ESI Entradas.\n"
            + "Abre el enlace de recuperacion del correo para elegir una nueva contrasena.\n\n"
            + "El enlace es valido por " + resetExpirationMinutes + " minutos.\n\n";

        String safeResetLink = htmlEscape(resetLink);
        String htmlBody = """
            <div style=\"font-family: Arial, sans-serif; color: #222; line-height: 1.5;\">
              <p>Hola,</p>
              <p>Has solicitado restablecer tu contrasena en ESI Entradas.</p>
              <p>
                <a href=\"%s\" style=\"display: inline-block; padding: 12px 18px; background: #ffb703; color: #211231; font-weight: 700; text-decoration: none; border-radius: 8px;\">Restablecer contraseña</a>
              </p>
              <p>El enlace es valido por %d minutos.</p>
            </div>
            """.formatted(safeResetLink, resetExpirationMinutes);

        if (!smtpConfigurado()) {
            log.warn("SMTP no configurado correctamente. No se envio email de recuperacion a {}.", to);
            return;
        }

        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            String remitente = remitente();
            if (!remitente.isBlank()) {
                helper.setFrom(remitente);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);
            mailSender.send(msg);
            log.info("Email de recuperacion enviado a {}", to);
        } catch (Exception ex) {
            // No relanzamos: el endpoint /forgot-password debe seguir devolviendo 200
            // para no revelar si el email existe (anti user-enumeration).
            log.error("Fallo al enviar email a {}: {}", to, ex.getMessage());
            log.warn("No se muestra el token de recuperacion por consola.");
        }
    }

    private String htmlEscape(String value) {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
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
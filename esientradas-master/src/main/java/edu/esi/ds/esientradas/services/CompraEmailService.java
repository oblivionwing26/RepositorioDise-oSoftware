package edu.esi.ds.esientradas.services;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import edu.esi.ds.esientradas.model.Compra;

@Service
public class CompraEmailService {

    private static final Logger log = LoggerFactory.getLogger(CompraEmailService.class);

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

    public boolean enviarConfirmacion(Compra compra) {
        String subject = "Confirmacion de compra - ESI Entradas";
        String body = crearCuerpo(compra);

        if (!smtpConfigurado()) {
            log.warn("SMTP no configurado correctamente. No se envio confirmacion de compra a {}.", compra.getEmailUsuario());
            return false;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            String remitente = remitente();
            if (!remitente.isBlank()) {
                msg.setFrom(remitente);
            }
            msg.setTo(compra.getEmailUsuario());
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Confirmacion de compra enviada a {}", compra.getEmailUsuario());
            return true;
        } catch (Exception ex) {
            log.error("Fallo al enviar confirmacion de compra a {}: {}", compra.getEmailUsuario(), ex.getMessage());
            log.warn("La compra queda confirmada igualmente, pero no se imprime la entrada por consola.");
            return false;
        }
    }

    private String crearCuerpo(Compra compra) {
        String precio = String.format(Locale.ROOT, "%.2f EUR", compra.getPrecioCentimos() / 100.0);

        return "Entrada comprada correctamente.\n\n"
            + "Artista: " + compra.getArtista() + "\n"
            + "Fecha: " + compra.getFechaEspectaculo() + "\n"
            + "Escenario: " + compra.getEscenario() + "\n"
            + "Ubicacion: " + compra.getUbicacion() + "\n"
            + "Precio: " + precio + "\n"
            + "Codigo de entrada: " + compra.getCodigoEntrada() + "\n"
            + "Referencia de pago: " + compra.getReferenciaPago() + "\n";
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

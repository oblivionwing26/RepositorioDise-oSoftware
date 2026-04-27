package edu.esi.ds.esientradas.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import edu.esi.ds.esientradas.model.PDFEntradas;
import jakarta.activation.DataSource;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void enviarEmail(String destinatario, String asunto, PDFEntradas pdfAdjunto) {
        if (mailSender == null) {
            System.out.println("[EmailService] Mail no configurado. Simulando envío a " + destinatario + " con asunto: " + asunto);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText("Adjunto encontrarás tu entrada. ¡Disfruta del espectáculo!");

            DataSource dataSource = new ByteArrayDataSource(pdfAdjunto.getContenido(), "application/pdf");
            helper.addAttachment(pdfAdjunto.getNombreArchivo(), dataSource);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar email: " + e.getMessage());
        }
    }
}

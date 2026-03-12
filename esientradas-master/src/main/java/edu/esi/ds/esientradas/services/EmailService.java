package edu.esi.ds.esientradas.services;

import org.springframework.stereotype.Service;

import edu.esi.ds.esientradas.model.PDFEntradas;

@Service
public class EmailService {

    public void enviarEmail(String destinatario, String asunto, PDFEntradas pdfAdjunto) {
        // TODO: Implementar envío de email con el PDF adjunto
        System.out.println("Enviando email a " + destinatario + " con asunto: " + asunto);
    }
}

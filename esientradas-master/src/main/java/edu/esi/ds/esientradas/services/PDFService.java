package edu.esi.ds.esientradas.services;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.esi.ds.esientradas.dao.PDFDao;
import edu.esi.ds.esientradas.model.PDFEntradas;
import edu.esi.ds.esientradas.model.Pago;

@Service
public class PDFService {

    @Autowired
    private PDFDao pdfDao;

    @Autowired
    private EmailService emailService;

    public PDFEntradas generarPDF(Pago pago) {
        String contenidoTexto = generarContenidoEntrada(pago);

        PDFEntradas pdf = new PDFEntradas();
        pdf.setNombreArchivo("entrada_" + pago.getId() + ".pdf");
        pdf.setPago(pago);
        pdf.setContenido(contenidoTexto.getBytes(StandardCharsets.UTF_8));

        this.pdfDao.save(pdf);

        return pdf;
    }

    public void enviarPDFPorEmail(PDFEntradas pdf, String email) {
        this.emailService.enviarEmail(email, "Tu entrada - ESI Entradas", pdf);
    }

    private String generarContenidoEntrada(Pago pago) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ESI ENTRADAS - TICKET ===\n\n");
        sb.append("ID Pago: ").append(pago.getId()).append("\n");
        sb.append("Importe: ").append(pago.getCentimos() / 100.0).append(" EUR\n");
        sb.append("Estado: ").append(pago.getEstado()).append("\n");
        sb.append("Fecha: ").append(pago.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        sb.append("Payment Intent: ").append(pago.getPaymentIntentId()).append("\n\n");
        sb.append("=============================\n");
        return sb.toString();
    }
}

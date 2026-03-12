package edu.esi.ds.esientradas.services;

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
        // TODO: Generar PDF real con los datos de las entradas
        PDFEntradas pdf = new PDFEntradas();
        pdf.setNombreArchivo("entrada_" + pago.getId() + ".pdf");
        pdf.setPago(pago);
        pdf.setContenido(new byte[0]); // Placeholder

        this.pdfDao.save(pdf);

        return pdf;
    }

    public void enviarPDFPorEmail(PDFEntradas pdf, String email) {
        this.emailService.enviarEmail(email, "Tu entrada - ESI Entradas", pdf);
    }
}

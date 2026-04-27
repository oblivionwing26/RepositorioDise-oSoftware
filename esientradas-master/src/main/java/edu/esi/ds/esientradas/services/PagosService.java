package edu.esi.ds.esientradas.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.esi.ds.esientradas.dao.ConfirmarPagoDao;
import edu.esi.ds.esientradas.dao.ConfiguracionDao;
import edu.esi.ds.esientradas.dao.PagoDao;
import edu.esi.ds.esientradas.model.Confirmacion;
import edu.esi.ds.esientradas.model.Pago;

@Service
public class PagosService {

    @Autowired
    private PagoDao pagoDao;

    @Autowired
    private ConfirmarPagoDao confirmarPagoDao;

    @Autowired
    private ConfiguracionDao configuracionDao;

    @Autowired
    private PDFService pdfService;

    public String prepararPago(Long centimos) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(centimos)
                .setCurrency("eur")
                .addPaymentMethodType("card")
                .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Pago pago = new Pago();
            pago.setCentimos(centimos);
            pago.setEstado("PENDIENTE");
            pago.setClientSecret(intent.getClientSecret());
            pago.setPaymentIntentId(intent.getId());

            this.pagoDao.save(pago);

            return intent.getClientSecret();
        } catch (StripeException e) {
            throw new RuntimeException("Error al crear el PaymentIntent en Stripe", e);
        }
    }

    public String confirmarPago(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

            Confirmacion confirmacion = new Confirmacion();
            confirmacion.setPaymentIntentId(paymentIntentId);
            confirmacion.setEstado(intent.getStatus());

            this.confirmarPagoDao.save(confirmacion);

            if ("succeeded".equals(intent.getStatus())) {
                // Buscar el pago asociado y generar PDF
                return "Pago confirmado correctamente";
            }

            return "Estado del pago: " + intent.getStatus();
        } catch (StripeException e) {
            throw new RuntimeException("Error al confirmar el pago en Stripe", e);
        }
    }
}

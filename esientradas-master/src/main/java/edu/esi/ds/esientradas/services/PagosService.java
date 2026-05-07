package edu.esi.ds.esientradas.services;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.esi.ds.esientradas.dao.ConfirmarPagoDao;
import edu.esi.ds.esientradas.dao.PagoDao;
import edu.esi.ds.esientradas.dto.DtoPagoPreparado;
import edu.esi.ds.esientradas.model.Confirmacion;
import edu.esi.ds.esientradas.model.Pago;

@Service
public class PagosService {

    @Autowired
    private PagoDao pagoDao;

    @Autowired
    private ConfirmarPagoDao confirmarPagoDao;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.public-key:}")
    private String stripePublicKey;

    @Value("${stripe.currency:eur}")
    private String currency;

    public DtoPagoPreparado prepararPago(Long centimos) {
        return prepararPago(centimos, "Compra de entrada");
    }

    public DtoPagoPreparado prepararPago(Long centimos, String descripcion) {
        validarCentimos(centimos);

        Pago pago = stripeConfigurado()
            ? prepararPagoStripe(centimos, descripcion)
            : prepararPagoSimulado(centimos, descripcion);

        return toDto(pago);
    }

    public String confirmarPago(String paymentIntentId) {
        Pago pago = confirmarPagoPreparado(null, "Confirmacion de pago", paymentIntentId);
        return "Pago confirmado correctamente: " + pago.getTokenPago();
    }

    public Pago confirmarPagoPreparado(Long centimosEsperados, String descripcion, String tokenPago) {
        return stripeConfigurado()
            ? confirmarPagoStripe(centimosEsperados, tokenPago)
            : confirmarPagoSimulado(centimosEsperados, descripcion, tokenPago);
    }

    private Pago prepararPagoStripe(Long centimos, String descripcion) {
        if (stripePublicKey == null || stripePublicKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Stripe esta configurado en backend, pero falta stripe.public-key para el frontend");
        }

        Stripe.apiKey = stripeSecretKey;

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(centimos)
                .setCurrency(monedaStripe())
                .setDescription(descripcion)
                .addPaymentMethodType("card")
                .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Pago pago = new Pago();
            pago.setCentimos(centimos);
            pago.setMoneda(monedaStripe());
            pago.setMetodo("STRIPE");
            pago.setEstado(estadoLocalStripe(intent.getStatus()));
            pago.setDescripcion(descripcion);
            pago.setTokenPago(intent.getId());
            pago.setClientSecret(intent.getClientSecret());
            pago.setPaymentIntentId(intent.getId());
            return this.pagoDao.save(pago);
        } catch (StripeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo preparar el pago con Stripe", ex);
        }
    }

    private Pago prepararPagoSimulado(Long centimos, String descripcion) {

        Pago pago = new Pago();
        pago.setCentimos(centimos);
        pago.setMoneda(monedaStripe());
        pago.setMetodo("SIMULADO");
        pago.setEstado("PENDIENTE");
        pago.setDescripcion(descripcion);

        String tokenPago = "SIMULADO-" + UUID.randomUUID();
        String clientSecretSimulado = "cs_simulado_" + UUID.randomUUID();
        pago.setTokenPago(tokenPago);
        pago.setClientSecret(clientSecretSimulado);
        pago.setPaymentIntentId(tokenPago);

        return this.pagoDao.save(pago);
    }

    private Pago confirmarPagoStripe(Long centimosEsperados, String tokenPago) {
        if (tokenPago == null || tokenPago.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el identificador del PaymentIntent de Stripe");
        }

        Pago pago = this.pagoDao.findByTokenPago(tokenPago)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un pago Stripe preparado con ese token"));

        if (centimosEsperados != null && !centimosEsperados.equals(pago.getCentimos())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El importe del pago no coincide con la entrada");
        }
        if ("CONFIRMADO".equalsIgnoreCase(pago.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pago ya fue confirmado");
        }

        Stripe.apiKey = stripeSecretKey;

        try {
            PaymentIntent intent = PaymentIntent.retrieve(tokenPago);
            if (centimosEsperados != null && !centimosEsperados.equals(intent.getAmount())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El importe confirmado por Stripe no coincide con la entrada");
            }
            if (!monedaStripe().equalsIgnoreCase(intent.getCurrency())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La moneda confirmada por Stripe no coincide");
            }
            if (!"succeeded".equalsIgnoreCase(intent.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El pago de Stripe aun no esta completado");
            }

            pago.setEstado("CONFIRMADO");
            pago.setConfirmadoEn(LocalDateTime.now());
            pago.setPaymentIntentId(intent.getId());
            this.pagoDao.save(pago);

            registrarConfirmacion(pago);
            return pago;
        } catch (StripeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo validar el pago con Stripe", ex);
        }
    }

    private Pago confirmarPagoSimulado(Long centimosEsperados, String descripcion, String tokenPago) {
        Pago pago = buscarOCrearPagoSimulado(centimosEsperados, descripcion, tokenPago);
        if (centimosEsperados != null && !centimosEsperados.equals(pago.getCentimos())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El importe del pago no coincide con la entrada");
        }
        if ("CONFIRMADO".equalsIgnoreCase(pago.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pago ya fue confirmado");
        }

        pago.setEstado("CONFIRMADO");
        pago.setConfirmadoEn(LocalDateTime.now());
        pago.setPaymentIntentId(pago.getTokenPago());
        this.pagoDao.save(pago);

        registrarConfirmacion(pago);
        return pago;
    }

    private void registrarConfirmacion(Pago pago) {
        Confirmacion confirmacion = new Confirmacion();
        confirmacion.setPaymentIntentId(pago.getPaymentIntentId());
        confirmacion.setEstado("CONFIRMADO");
        confirmacion.setPago(pago);

        this.confirmarPagoDao.save(confirmacion);
    }

    private Pago buscarOCrearPagoSimulado(Long centimosEsperados, String descripcion, String tokenPago) {
        if (tokenPago != null && !tokenPago.isBlank() && !"SIMULADO".equalsIgnoreCase(tokenPago)) {
            return this.pagoDao.findByTokenPago(tokenPago)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un pago preparado con ese token"));
        }

        validarCentimos(centimosEsperados);
        Pago pago = new Pago();
        pago.setCentimos(centimosEsperados);
        pago.setMoneda(monedaStripe());
        pago.setMetodo("SIMULADO");
        pago.setEstado("PENDIENTE");
        pago.setDescripcion(descripcion);
        pago.setTokenPago("SIMULADO-" + UUID.randomUUID());
        pago.setClientSecret("cs_simulado_" + UUID.randomUUID());
        pago.setPaymentIntentId(pago.getTokenPago());
        return this.pagoDao.save(pago);
    }

    private void validarCentimos(Long centimos) {
        if (centimos == null || centimos <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe del pago debe ser positivo");
        }
    }

    private boolean stripeConfigurado() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    private String monedaStripe() {
        return currency == null || currency.isBlank() ? "eur" : currency.toLowerCase(Locale.ROOT);
    }

    private String estadoLocalStripe(String status) {
        if ("succeeded".equalsIgnoreCase(status)) {
            return "CONFIRMADO";
        }
        if ("canceled".equalsIgnoreCase(status)) {
            return "CANCELADO";
        }
        return "PENDIENTE";
    }

    private DtoPagoPreparado toDto(Pago pago) {
        return new DtoPagoPreparado(
            pago.getId(),
            pago.getCentimos(),
            pago.getMoneda(),
            pago.getMetodo(),
            pago.getEstado(),
            pago.getTokenPago(),
            pago.getClientSecret(),
            stripeConfigurado() ? stripePublicKey : ""
        );
    }
}

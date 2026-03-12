package edu.esi.ds.esientradas.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.esi.ds.esientradas.dao.ConfirmarPagoDao;
import edu.esi.ds.esientradas.dao.PagoDao;
import edu.esi.ds.esientradas.model.Confirmacion;
import edu.esi.ds.esientradas.model.Pago;

@Service
public class PagosService {

    @Autowired
    private PagoDao pagoDao;

    @Autowired
    private ConfirmarPagoDao confirmarPagoDao;

    public String prepararPago(Long centimos) {
        // Crear registro del pago en BD
        Pago pago = new Pago();
        pago.setCentimos(centimos);
        pago.setEstado("PENDIENTE");

        // TODO: Conectar con Stripe para obtener clientSecret real
        // Por ahora, guardamos el pago y devolvemos un clientSecret simulado
        String clientSecretSimulado = "cs_test_" + System.currentTimeMillis();
        pago.setClientSecret(clientSecretSimulado);

        this.pagoDao.save(pago);

        return clientSecretSimulado;
    }

    public String confirmarPago(String paymentIntentId) {
        // TODO: Verificar con Stripe que el pago se ha completado

        // Registrar confirmación en BD
        Confirmacion confirmacion = new Confirmacion();
        confirmacion.setPaymentIntentId(paymentIntentId);
        confirmacion.setEstado("CONFIRMADO");

        this.confirmarPagoDao.save(confirmacion);

        return "Pago confirmado correctamente";
    }
}

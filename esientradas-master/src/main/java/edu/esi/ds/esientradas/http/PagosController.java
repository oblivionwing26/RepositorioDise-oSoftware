package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.services.PagosService;
import edu.esi.ds.esientradas.dto.DtoPagoPreparado;

import java.util.Map;

@RestController
@RequestMapping("/pagos")
public class PagosController {

    @Autowired
    private PagosService service;

    @PostMapping("/prepararPago")
    public DtoPagoPreparado prepararPago(@RequestBody Map<String, Object> pagoData) {
        Long centimos = ((Number) pagoData.get("centimos")).longValue();
        return this.service.prepararPago(centimos);
    }

    @PostMapping("/confirmarPago")
    public String confirmarPago(@RequestBody Map<String, String> data) {
        String paymentIntentId = data.get("paymentIntentId");
        return this.service.confirmarPago(paymentIntentId);
    }
}

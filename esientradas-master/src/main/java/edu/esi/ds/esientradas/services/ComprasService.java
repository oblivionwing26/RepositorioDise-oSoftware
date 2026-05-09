package edu.esi.ds.esientradas.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.CompraDao;
import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dto.DtoCompra;
import edu.esi.ds.esientradas.model.Compra;
import edu.esi.ds.esientradas.model.DeZona;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.model.Pago;
import edu.esi.ds.esientradas.model.Precisa;
import jakarta.transaction.Transactional;

@Service
public class ComprasService {

    @Autowired
    private EntradaDao entradaDao;

    @Autowired
    private CompraDao compraDao;

    @Autowired
    private PagosService pagosService;

    @Autowired
    private CompraEmailService compraEmailService;

    @Transactional
    public DtoCompra comprar(String tokenEntrada, String emailUsuario, String tokenPago) {
        if (tokenEntrada == null || tokenEntrada.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token de la entrada es requerido");
        }
        if (emailUsuario == null || emailUsuario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no validado");
        }

        Entrada entrada = this.entradaDao.findByTokenPrerreservaForUpdate(tokenEntrada)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una prerreserva con ese token"));

        if (entrada.getEstado() != Estado.PRERRESERVADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada no está prerreservada");
        }

        if (prerreservaExpirada(entrada)) {
            liberarPrerreserva(entrada);
            this.entradaDao.save(entrada);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La prerreserva ha expirado");
        }

        if (!emailUsuario.equalsIgnoreCase(entrada.getUsuarioPrerreserva())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La prerreserva pertenece a otro usuario");
        }

        Pago pago = this.pagosService.confirmarPagoPreparado(
            entrada.getPrecio(),
            "Compra de entrada " + entrada.getId(),
            tokenPago
        );

        entrada.setEstado(Estado.VENDIDA);
        entrada.setTokenPrerreserva(null);
        entrada.setPrerreservaExpiraEn(null);
        entrada.setUsuarioPrerreserva(null);
        this.entradaDao.save(entrada);

        Compra compra = crearCompra(entrada, emailUsuario, pago);
        compra = this.compraDao.save(compra);
        boolean emailEnviado = this.compraEmailService.enviarConfirmacion(compra);

        String mensaje = "Compra completada para el usuario: " + emailUsuario
            + " con la entrada: " + entrada.getId()
            + ". Codigo: " + compra.getCodigoEntrada();
        return new DtoCompra(
            compra.getId(),
            entrada.getId(),
            entrada.getPrecio(),
            entrada.getEstado().name(),
            emailUsuario,
            compra.getCodigoEntrada(),
            compra.getReferenciaPago(),
            compra.getMetodoPago(),
            pago.getEstado(),
            emailEnviado,
            mensaje
        );
    }

    private boolean prerreservaExpirada(Entrada entrada) {
        return entrada.getPrerreservaExpiraEn() == null || !entrada.getPrerreservaExpiraEn().isAfter(LocalDateTime.now());
    }

    private void liberarPrerreserva(Entrada entrada) {
        entrada.setEstado(Estado.DISPONIBLE);
        entrada.setTokenPrerreserva(null);
        entrada.setPrerreservaExpiraEn(null);
        entrada.setUsuarioPrerreserva(null);
    }

    private Compra crearCompra(Entrada entrada, String emailUsuario, Pago pago) {
        Espectaculo espectaculo = entrada.getEspectaculo();
        Escenario escenario = espectaculo.getEscenario();

        Compra compra = new Compra();
        compra.setEntrada(entrada);
        compra.setPago(pago);
        compra.setEmailUsuario(emailUsuario);
        compra.setPrecioCentimos(entrada.getPrecio());
        compra.setCodigoEntrada(generarCodigoEntrada(entrada));
        compra.setEstado("CONFIRMADA");
        compra.setMetodoPago(pago.getMetodo());
        compra.setReferenciaPago(pago.getPaymentIntentId());
        compra.setIdEspectaculo(espectaculo.getId());
        compra.setArtista(valorSeguro(espectaculo.getArtista(), "Artista no indicado"));
        compra.setFechaEspectaculo(espectaculo.getFecha() != null ? espectaculo.getFecha() : LocalDateTime.now());
        compra.setEscenario(valorSeguro(escenario.getNombre(), "Escenario no indicado"));
        compra.setUbicacion(describirUbicacion(entrada));
        compra.setCompradaEn(LocalDateTime.now());
        return compra;
    }

    private String generarCodigoEntrada(Entrada entrada) {
        return "ENT-" + entrada.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String describirUbicacion(Entrada entrada) {
        if (entrada instanceof DeZona deZona) {
            return "Zona " + deZona.getZona();
        }
        if (entrada instanceof Precisa precisa) {
            return "Planta " + precisa.getPlanta()
                + ", fila " + precisa.getFila()
                + ", butaca " + precisa.getColumna();
        }
        return "Entrada " + entrada.getId();
    }

    private String valorSeguro(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

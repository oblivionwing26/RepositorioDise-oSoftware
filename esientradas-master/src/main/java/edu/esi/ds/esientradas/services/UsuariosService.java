package edu.esi.ds.esientradas.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuariosService {

    private static final String ESIUSUARIOS_BASE_URL = "http://localhost:8081";

    public String checkToken(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se necesita el token");
        }

        String endpoint = ESIUSUARIOS_BASE_URL + "/external/checktoken/" + userToken;
        RestTemplate restTemplate = new RestTemplate();

        try {
            String userName = restTemplate.getForObject(endpoint, String.class);
            if (userName == null || userName.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no válido");
            }
            return userName;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error al validar token externo", ex);
        }
    }
}


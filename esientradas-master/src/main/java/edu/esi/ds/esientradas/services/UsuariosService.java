package edu.esi.ds.esientradas.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuariosService {
    public String checkToken(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se necesita el token");
        }

        String endpoint = "http://localhost:8081/external/checkToken";
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.getForObject(endpoint, String.class);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al validar token externo", ex);
        }

        try{
            String userName = restTemplate.getForObject(endpoint + "/" + userToken, String.class);
            if (userName == null || userName.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no valido");
            }
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error al validar token externo", ex);
        }

        return userToken;
    }
}


package edu.esi.ds.esientradas.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class UsuariosService {

    @Value("${app.esiusuarios.url:http://localhost:8081}")
    private String esiusuariosBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String checkToken(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se necesita el token.");
        }

        String url = UriComponentsBuilder
            .fromUriString(esiusuariosBaseUrl)
            .pathSegment("external", "checktoken", userToken)
            .toUriString();

        try {
            String userEmail = restTemplate.getForObject(url, String.class);
            if (userEmail == null || userEmail.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no valido.");
            }
            return userEmail;
        } catch (HttpClientErrorException ex) {
            // 400 o 401 que devuelva esiusuarios. En Spring 6 getStatusCode() devuelve HttpStatusCode.
            throw new ResponseStatusException(ex.getStatusCode().value(), "Token no valido.", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "No se pudo contactar con esiusuarios.", ex);
        }
    }
}


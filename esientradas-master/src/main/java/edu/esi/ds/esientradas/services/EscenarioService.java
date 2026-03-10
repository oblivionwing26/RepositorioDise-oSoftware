package edu.esi.ds.esientradas.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.EscenarioDao;
import edu.esi.ds.esientradas.model.Escenario;

@Service
public class EscenarioService {
    @Autowired
    private EscenarioDao dao;

    public void insertar(Escenario escenario) {
        try{
            this.dao.save(escenario);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);  
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No sabemos que ha pasado pero hubo un error al insertar el escenario", e);
        }
    }

}

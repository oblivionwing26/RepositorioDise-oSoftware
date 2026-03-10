package edu.esi.ds.esiusuarios.http;

import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esiusuarios.services.UserService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private UserService userService;

    @PostMapping("/login")    
    public String login(@RequestBody Map<String, String> credentials){
        JSONObject jsoCredentials = new JSONObject(credentials);
        String name = jsoCredentials.optString("name");
        String password = jsoCredentials.optString("pwd");

        if(name == null || name.isEmpty() || password == null || password.isEmpty()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.");
        }
        String result = this.userService.login(name, password);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas.");
        }
        return result;
    }

}

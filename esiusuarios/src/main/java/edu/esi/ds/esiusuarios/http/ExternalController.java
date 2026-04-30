package edu.esi.ds.esiusuarios.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.annotation.RequestMapping;

import edu.esi.ds.esiusuarios.services.UserService;


@RestController
@RequestMapping("/external")
public class ExternalController {
    
    @Autowired
    private UserService userService;

    @GetMapping("/checktoken/{token}")
    public String checkToken(@PathVariable String token){ 
        return userService.checkToken(token);
    }
}

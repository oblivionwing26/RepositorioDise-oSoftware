package edu.esi.ds.esiusuarios.http;

import java.util.Map;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esiusuarios.dto.ForgotPasswordRequest;
import edu.esi.ds.esiusuarios.dto.LoginRequest;
import edu.esi.ds.esiusuarios.dto.LoginResponse;
import edu.esi.ds.esiusuarios.dto.RegisterRequest;
import edu.esi.ds.esiusuarios.dto.ResetPasswordRequest;
import edu.esi.ds.esiusuarios.services.UserService;


@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public void register(@RequestBody RegisterRequest req){
        userService.register(req);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req){
        return userService.login(req);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestBody ForgotPasswordRequest req){
        userService.forgotPassword(req);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody ResetPasswordRequest req){
        userService.resetPassword(req);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/me")
    public void cancelAccount(
        @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authHeader){
            userService.cancelAccount(authHeader);
    }

}

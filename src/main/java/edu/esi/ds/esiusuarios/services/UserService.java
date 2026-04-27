package edu.esi.ds.esiusuarios.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import edu.esi.ds.esiusuarios.dao.UserDao;
import edu.esi.ds.esiusuarios.dto.LoginRequest;
import edu.esi.ds.esiusuarios.dto.RegisterRequest;
import edu.esi.ds.esiusuarios.model.User;

@Service
public class UserService {

    private List <User> users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDao userDao;

    @Autowired
    private JwtService jwtService;

    public UserService(){
        this.users = List.of(
            new User("Pepe", "pepe123", "1234"),
            new User("Ana", "ana123", "5678"));
    }

	public String login(String name, String password) {
        for(User user : this.users){
            if(user.getName().equals(name) && user.getPassword().equals(password)){
                return "Login successful";
            }
        }
        return null;
	}

    public String checkToken(String token) {
        if (jwtService.isTokenValid(token)) {
            return jwtService.getEmailFromToken(token);
        }
        return null;
    }

    public void register(RegisterRequest req) {
        User user = new User();
        user.setEmail(req.getEmail());

        String hash = passwordEncoder.encode(req.getPassword());
        user.setPasswordHash(hash);

        userDao.save(user);
    }

    public String login(LoginRequest req) {
        User user = userDao
            .findByEmail(req.getEmail())
            .orElseThrow();

        if (!passwordEncoder.matches(
                req.getPassword(),
                user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtService.generateToken(user);
    }

}

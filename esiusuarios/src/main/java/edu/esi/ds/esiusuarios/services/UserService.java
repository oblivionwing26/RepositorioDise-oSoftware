package edu.esi.ds.esiusuarios.services;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.esi.ds.esiusuarios.model.User;

@Service
public class UserService {

    private List <User> users;

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
        for(User user : this.users){
            if(user.getToken().equals(token)){
                return user.getName();
            }
        }
        return null;
    }

}

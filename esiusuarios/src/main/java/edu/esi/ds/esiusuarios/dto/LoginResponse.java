package edu.esi.ds.esiusuarios.model;


public class LoginResponse {
    private String token;
    private long ExpiresInMs;

    public LoginResponse(){}

    public LoginResponse (String token, long ExpiresInMs) {
        this.token = token;
        this.ExpiresInMs = ExpiresInMs;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getExpiresInMs() { return ExpiresInMs; }
    public void setExpiresInMs(long expiresInMs) { this.ExpiresInMs = expiresInMs; }
}
package com.teapp.model;

public class RegisterRequest {
    public String email;
    public String password;
    public String fullName;
    public String dateOfBirth;
    public String role;

    public RegisterRequest(String email, String password, String fullName,
                           String dateOfBirth, String role) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.role = role;
    }
}

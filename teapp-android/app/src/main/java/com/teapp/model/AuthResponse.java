package com.teapp.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    public String token;
    public long expiresIn;
    public String id;
    public String email;
    public String fullName;
    public String avatarBase64;
    public String role;
    public String inviteCode;
    public String dateOfBirth;
    public String createdAt;
}

package com.teapp.model;

public class ChangePasswordRequest {
    public String currentPassword;
    public String newPassword;

    public ChangePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword     = newPassword;
    }
}

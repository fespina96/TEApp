package com.teapp.model;

import java.io.Serializable;

public class Child implements Serializable {
    public String id;
    public String name;
    public String dateOfBirth;
    public String avatarColor;
    public String avatarBase64;
    public String notes;
    public String createdAt;

    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(parts[0].charAt(0)).toUpperCase();
    }
}

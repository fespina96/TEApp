package com.teapp.entity;

import com.teapp.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad que representa a un padre/tutor registrado en el sistema.
 * Un usuario puede administrar múltiples perfiles de niños.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "avatar_base64", columnDefinition = "TEXT")
    private String avatarBase64;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.PARENT;

    @Column(name = "invite_code", unique = true, length = 10)
    private String inviteCode;

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Child> children = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "therapist_parent_links",
        joinColumns = @JoinColumn(name = "therapist_id"),
        inverseJoinColumns = @JoinColumn(name = "parent_id"))
    @Builder.Default
    private List<User> supervisedParents = new ArrayList<>();

    @ManyToMany(mappedBy = "supervisedParents")
    @Builder.Default
    private List<User> therapists = new ArrayList<>();
}

package com.teapp.entity;

import com.teapp.enums.ActivityCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Actividad que puede incluirse en la agenda de un niño.
 * Puede ser predefinida (del catálogo del sistema) o personalizada (creada por el padre).
 */
@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityCategory category;

    @Column(name = "icon_name", length = 50)
    private String iconName;

    @Column(nullable = false, length = 7)
    @Builder.Default
    private String color = "#A8D8EA";

    @Column(name = "image_base64", columnDefinition = "TEXT")
    private String imageBase64;

    @Column(name = "pictogram_url", columnDefinition = "TEXT")
    private String pictogramUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(nullable = false)
    @Builder.Default
    private boolean pausable = true;

    @Column(name = "is_predefined", nullable = false)
    @Builder.Default
    private boolean predefined = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

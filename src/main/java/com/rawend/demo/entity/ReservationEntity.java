package com.rawend.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
@Getter
@Setter
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // Lien avec l'utilisateur

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;  // Lien avec le service

    private Double prix;  // Prix final du service

    @Enumerated(EnumType.STRING)
    private ModePaiement modePaiement;  // Utilisation de l'énumération externe

    private String localisation;  // Coordonnées GPS sous forme "latitude,longitude"
    
    private LocalDateTime dateReservation;  // Date et heure choisies pour le service
    private LocalDateTime dateCreation;  // Date et heure de la création de la réservation

    private String titreService;  // Titre du service (ex : "Premium Wash")

    private String email;  // Email de l'utilisateur
    private String phone;  // Téléphone de l'utilisateur

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
        
        if (this.service != null) {
            this.titreService = this.service.getTitre(); // Assigne le titre du service
        }
        
        if (this.user != null) {
            this.email = this.user.getEmail();
            this.phone = this.user.getPhone();
        }
    }
}

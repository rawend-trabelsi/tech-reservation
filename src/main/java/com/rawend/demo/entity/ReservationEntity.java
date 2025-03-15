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
    private ServiceEntity service;  

    private Double prix; 

    @Enumerated(EnumType.STRING)
    private ModePaiement modePaiement;  

    private String localisation;  
    
    private LocalDateTime dateReservation;  
    private LocalDateTime dateCreation;  

    private String titreService;  

    private String email;  
    private String phone;
    private String duree;
    @Column(name = "technicien_id", nullable = true) 
    private Long technicienId;


    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
        
        if (this.service != null) {
            this.titreService = this.service.getTitre(); 
        }
        
        if (this.user != null) {
            this.email = this.user.getEmail();
            this.phone = this.user.getPhone();
        }
    }
}

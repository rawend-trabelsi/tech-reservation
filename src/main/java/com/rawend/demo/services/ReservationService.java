package com.rawend.demo.services;

import com.rawend.demo.dto.ReservationRequest;
import com.rawend.demo.entity.*;
import com.rawend.demo.Repository.ReservationRepository;
import com.rawend.demo.Repository.UserRepository;
import com.rawend.demo.Repository.ServiceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

 

    public Map<String, Object> createReservation(ReservationRequest request, Authentication authentication) {
        // Récupérer l'utilisateur connecté via l'authentification
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Récupérer le service concerné
        ServiceEntity service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new RuntimeException("Service non trouvé"));

        // Calcul du prix final avec vérification de la promotion
        Double prixFinal = service.getPrix();
        PromotionEntity promo = service.getPromotion();

        if (promo != null) {
            if (promo.getCodePromo() == null || promo.getCodePromo().isEmpty()) {
                LocalDate today = LocalDate.now();
                LocalDateTime startOfDay = today.atStartOfDay();
                LocalDateTime endOfDay = today.atTime(23, 59, 59);

                LocalDateTime promoStart = convertToLocalDateTime(promo.getDateDebut()).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime promoEnd = convertToLocalDateTime(promo.getDateFin()).withHour(23).withMinute(59).withSecond(59);

                if (!promoStart.isAfter(endOfDay) && !promoEnd.isBefore(startOfDay)) {
                    if (promo.getTypeReduction() == TypeReduction.POURCENTAGE) {
                        prixFinal = service.getPrix() * (1 - promo.getValeurReduction() / 100);
                    } else if (promo.getTypeReduction() == TypeReduction.MONTANT_FIXE) {
                        prixFinal = service.getPrix() - promo.getValeurReduction();
                    }
                }
            }
        }

        // Création de la réservation
        ReservationEntity reservation = new ReservationEntity();
        reservation.setUser(user);
        reservation.setService(service);
        reservation.setPrix(prixFinal);
        reservation.setModePaiement(request.modePaiement());
        reservation.setLocalisation(request.localisation());
        reservation.setDateReservation(request.dateReservation());
        reservation.setDateCreation(LocalDateTime.now());
        reservation.setEmail(email);
        reservation.setPhone(user.getPhone());
        reservation.setTitreService(service.getTitre());

        // Sauvegarde de la réservation
        ReservationEntity savedReservation = reservationRepository.save(reservation);

        // Création de la Map pour afficher les attributs
        Map<String, Object> response = new HashMap<>();
        response.put("reservationId", savedReservation.getId());
        response.put("userEmail", savedReservation.getEmail());
        response.put("userPhone", savedReservation.getPhone());
        response.put("serviceTitre", savedReservation.getTitreService());
        response.put("prixFinal", savedReservation.getPrix());
        response.put("modePaiement", savedReservation.getModePaiement());
        response.put("localisation", savedReservation.getLocalisation());
        response.put("dateReservation", savedReservation.getDateReservation());
        response.put("dateCreation", savedReservation.getDateCreation());

        // Ajouter les ids de service et utilisateur dans la Map
        response.put("serviceId", service.getId());
        response.put("userId", user.getId());

        return response;
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }


}

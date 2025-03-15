package com.rawend.demo.services;

import com.rawend.demo.dto.ReservationRequest;
import com.rawend.demo.entity.*;
import com.rawend.demo.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    
    @Autowired
    private TechnicienEmploiRepository technicienEmploiRepository;
    
    @Autowired
    private affectationTechnicienRepository affectationTechnicienRepository;

    public Map<String, Object> createReservation(ReservationRequest request, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        ServiceEntity service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new RuntimeException("Service non trouvé"));

        Double prixFinal = service.getPrix();
        String duree = service.getDuree();
        PromotionEntity promo = service.getPromotion();

        if (promo != null && (promo.getCodePromo() == null || promo.getCodePromo().isEmpty())) {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);
            LocalDateTime promoStart = convertToLocalDateTime(promo.getDateDebut()).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime promoEnd = convertToLocalDateTime(promo.getDateFin()).withHour(23).withMinute(59).withSecond(59);

            if (!promoStart.isAfter(endOfDay) && !promoEnd.isBefore(startOfDay)) {
                prixFinal = promo.getTypeReduction() == TypeReduction.POURCENTAGE ?
                        service.getPrix() * (1 - promo.getValeurReduction() / 100) :
                        service.getPrix() - promo.getValeurReduction();
            }
        }

        ReservationEntity reservation = new ReservationEntity();
        reservation.setUser(user);
        reservation.setService(service);
        reservation.setPrix(prixFinal);
        reservation.setDuree(duree);
        reservation.setModePaiement(request.modePaiement());
        reservation.setLocalisation(request.localisation());
        reservation.setDateReservation(request.dateReservation());
        reservation.setDateCreation(LocalDateTime.now());
        reservation.setEmail(email);
        reservation.setPhone(user.getPhone());
        reservation.setTitreService(service.getTitre());
        ReservationEntity savedReservation = reservationRepository.save(reservation);

        Map<String, Object> response = new HashMap<>();
        response.put("reservationId", savedReservation.getId());
        response.put("userEmail", savedReservation.getEmail());
        response.put("userPhone", savedReservation.getPhone());
        response.put("serviceTitre", savedReservation.getTitreService());
        response.put("prixFinal", savedReservation.getPrix());
        response.put("modePaiement", savedReservation.getModePaiement());
        response.put("duree", savedReservation.getDuree());
        response.put("localisation", savedReservation.getLocalisation());
        response.put("dateReservation", savedReservation.getDateReservation());
        response.put("dateCreation", savedReservation.getDateCreation());
        response.put("serviceId", service.getId());
        response.put("userId", user.getId());

        return response;
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

  
    public void affecterTechnicienAReservation(Long reservationId, Long technicienId) {
        ReservationEntity reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));

        TechnicienEmploi technicien = technicienEmploiRepository.findById(technicienId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Technicien introuvable"));

        LocalDate reservationDate = reservation.getDateReservation().toLocalDate();
        JourRepos jourReposTechnicien = technicien.getJourRepos();
        JourRepos jourReservation = convertirJourEnFrancais(reservationDate.getDayOfWeek().toString());

        if (jourReposTechnicien == jourReservation) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le technicien est en repos le " + jourReservation);
        }

        int dureeMinutes = Integer.parseInt(reservation.getDuree().replaceAll("[^0-9]", ""));
        LocalDateTime dateDebut = reservation.getDateReservation();
        LocalDateTime dateFin = dateDebut.plusMinutes(dureeMinutes);

        // Vérifier si le technicien a déjà une réservation à ce moment-là
        boolean hasConflict = affectationTechnicienRepository.existsByTechnicienIdAndDateDebutBeforeAndDateFinAfter(
            technicienId, dateFin, dateDebut);

        if (hasConflict) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Le technicien a déjà une réservation entre " + dateDebut + " et " + dateFin);
        }

        // Créer une nouvelle affectation si pas de conflit
        AffectationTechnicien affectation = new AffectationTechnicien();
        affectation.setTechnicien(technicien);
        affectation.setReservation(reservation);
        affectation.setDateDebut(dateDebut);
        affectation.setDateFin(dateFin);
        affectation.setUsername(technicien.getUser().getUsername());
        affectationTechnicienRepository.save(affectation);

        reservation.setTechnicienId(technicienId);
        reservationRepository.save(reservation);
    }

    private JourRepos convertirJourEnFrancais(String jourAnglais) {
        Map<String, JourRepos> mapping = new HashMap<>();
        mapping.put("MONDAY", JourRepos.LUNDI);
        mapping.put("TUESDAY", JourRepos.MARDI);
        mapping.put("WEDNESDAY", JourRepos.MERCREDI);
        mapping.put("THURSDAY", JourRepos.JEUDI);
        mapping.put("FRIDAY", JourRepos.VENDREDI);
        mapping.put("SATURDAY", JourRepos.SAMEDI);
        mapping.put("SUNDAY", JourRepos.DIMANCHE);
        return mapping.getOrDefault(jourAnglais, null);
    }
}

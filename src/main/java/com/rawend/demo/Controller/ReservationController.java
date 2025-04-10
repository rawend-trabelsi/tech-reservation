package com.rawend.demo.Controller;

import com.rawend.demo.Repository.affectationTechnicienRepository;
import com.rawend.demo.dto.ReservationRequest;
import com.rawend.demo.entity.AffectationTechnicien;
import com.rawend.demo.entity.ReservationEntity;
import com.rawend.demo.services.ReservationService;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;
    private affectationTechnicienRepository affectationTechnicienRepository;

    @PostMapping("/add")
    public Map<String, Object> addReservation(@RequestBody ReservationRequest request, Authentication authentication) {
        return reservationService.createReservation(request, authentication);
    }
    @PutMapping("/{reservationId}/affecter-technicien/{technicienId}")
    public ResponseEntity<String> affecterTechnicien(
            @PathVariable Long reservationId,
            @PathVariable Long technicienId) {
        
        reservationService.affecterTechnicienAReservation(reservationId, technicienId);
        return ResponseEntity.ok("Technicien affecté avec succès !");
    }
    @GetMapping("/affectations")
    public List<AffectationTechnicien> getAllAffectations() {
        return affectationTechnicienRepository.findAll();
    }

}

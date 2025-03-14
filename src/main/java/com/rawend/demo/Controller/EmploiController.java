package com.rawend.demo.Controller;


import com.rawend.demo.entity.TechnicienEmploi;
import com.rawend.demo.entity.User;
import com.rawend.demo.services.EmploiService;
import com.rawend.demo.entity.Role;
import com.rawend.demo.Repository.UserRepository;
import com.rawend.demo.dto.EmploiRequest;
import com.rawend.demo.Repository.TechnicienEmploiRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/emplois")
public class EmploiController {

    private final EmploiService emploiService;

    public EmploiController(EmploiService emploiService) {
        this.emploiService = emploiService;
    }

    @PostMapping("/technicien/ajout")
    public ResponseEntity<?> ajouterEmploiTechnicien(@RequestBody EmploiRequest request) {
        try {
            emploiService.ajouterEmploiTechnicienParEmail(request);
            return ResponseEntity.ok("Emploi ajouté pour le technicien !");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/techniciens/emails")
    public ResponseEntity<?> getEmailsTechniciens() {
        try {
            return ResponseEntity.ok(emploiService.getEmailsTechniciens());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}


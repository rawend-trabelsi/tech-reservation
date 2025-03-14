package com.rawend.demo.services;


import com.rawend.demo.entity.TechnicienEmploi;
import com.rawend.demo.entity.User;
import com.rawend.demo.entity.JourRepos;
import com.rawend.demo.entity.Role;
import com.rawend.demo.Repository.UserRepository;
import com.rawend.demo.dto.EmploiRequest;
import com.rawend.demo.Repository.TechnicienEmploiRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class EmploiService {

    private final UserRepository userRepository;
    private final TechnicienEmploiRepository technicienEmploiRepository;

    public EmploiService(UserRepository userRepository, TechnicienEmploiRepository technicienEmploiRepository) {
        this.userRepository = userRepository;
        this.technicienEmploiRepository = technicienEmploiRepository;
    }

    public void ajouterEmploiTechnicienParEmail(EmploiRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> new RuntimeException("Utilisateur avec cet email introuvable"));

        if (!user.getRole().equals(Role.TECHNICIEN)) {
            throw new RuntimeException("Cet utilisateur n'est pas un technicien !");
        }

        TechnicienEmploi emploi = new TechnicienEmploi();
        emploi.setUser(user);
        emploi.setEmail(user.getEmail());
        emploi.setPhone(user.getPhone());
        emploi.setUsername(user.getUsernameValue());
        emploi.setJourRepos(JourRepos.valueOf(request.getJourRepos().toUpperCase()));
        emploi.setHeureDebut(request.getHeureDebut());
        emploi.setHeureFin(request.getHeureFin());

        technicienEmploiRepository.save(emploi);
    }
    public List<String> getEmailsTechniciens() {
        List<User> techniciens = userRepository.findByRole(Role.TECHNICIEN);

        if (techniciens.isEmpty()) {
            throw new RuntimeException("Aucun technicien trouvé !");
        }

        return techniciens.stream().map(User::getEmail).collect(Collectors.toList());
    }

}

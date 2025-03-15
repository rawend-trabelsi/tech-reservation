package com.rawend.demo.Repository;



import com.rawend.demo.entity.AffectationTechnicien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface affectationTechnicienRepository extends JpaRepository<AffectationTechnicien, Long> {

    
    List<AffectationTechnicien> findByTechnicienId(Long technicienId);

  
    List<AffectationTechnicien> findByReservationId(Long reservationId);

    
    boolean existsByTechnicienIdAndDateDebutBetween(Long technicienId, LocalDateTime start, LocalDateTime end);
   
   
    void deleteById(Long id);
    boolean existsByTechnicienIdAndDateDebutBeforeAndDateFinAfter(Long technicienId, LocalDateTime dateFin, LocalDateTime dateDebut);
}


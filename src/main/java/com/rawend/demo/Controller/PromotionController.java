package com.rawend.demo.Controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rawend.demo.dto.PromotionDTO;
import com.rawend.demo.dto.ServiceDTO;
import com.rawend.demo.entity.PromotionEntity;
import com.rawend.demo.entity.ServiceEntity;
import com.rawend.demo.entity.TypeReduction;
import com.rawend.demo.services.PromotionService;
import com.rawend.demo.services.ServiceService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private EntityManager entityManager;
  
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> creerPromotion(@RequestBody PromotionDTO promotionDTO) {
        Logger logger = LoggerFactory.getLogger(PromotionController.class);
        try {
            // Conversion de PromotionDTO en PromotionEntity
            PromotionEntity promotionEntity = new PromotionEntity();
            promotionEntity.setActif(promotionDTO.isActif());
            promotionEntity.setValeurReduction(promotionDTO.getValeurReduction());
            promotionEntity.setDateDebut(promotionDTO.getDateDebut());
            promotionEntity.setDateFin(promotionDTO.getDateFin());
            promotionEntity.setCodePromo(promotionDTO.getCodePromo());

            // Vérification et conversion du TypeReduction
            try {
                promotionEntity.setTypeReduction(TypeReduction.valueOf(promotionDTO.getTypeReduction().toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.error("Type de réduction invalide : {}", promotionDTO.getTypeReduction(), e);
                return ResponseEntity.badRequest().body(Map.of("error", "Type de réduction invalide."));
            }

            // Récupération des services depuis la base de données
            List<ServiceEntity> services = new ArrayList<>();
            if (promotionDTO.getServicesDTO() != null) {
                for (ServiceDTO serviceDTO : promotionDTO.getServicesDTO()) {
                    ServiceEntity serviceRecupere = serviceService.obtenirServiceParId(serviceDTO.getId());
                    if (serviceRecupere != null) {
                        services.add(serviceRecupere);
                    } else {
                        logger.warn("Service non trouvé avec l'ID : {}", serviceDTO.getId());
                    }
                }
            }

            // Sauvegarde de la promotion
            PromotionEntity promotionSauvegardee = promotionService.creerPromotion(promotionEntity);

            // Associer les services à la promotion
            for (ServiceEntity service : services) {
                service.setPromotion(promotionSauvegardee);
                serviceService.mettreAJourService(service);
            }

            // Construction de la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("promotionId", promotionSauvegardee.getId());
            response.put("services", services.stream()
                .map(service -> Map.of("serviceId", service.getId(), "titre", service.getTitre()))
                .collect(Collectors.toList()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la création de la promotion : ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Une erreur est survenue : " + e.getMessage()));
        }
    }
    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    @Transactional
    public void supprimerToutesAssociationsService(Long serviceId) {
   
		Query query = entityManager.createNativeQuery(
            "DELETE FROM promotion_service WHERE service_id = :serviceId"
        );
        query.setParameter("serviceId", serviceId);
        int result = query.executeUpdate();

        if (result == 0) {
           
            System.out.println("No associations found for serviceId: " + serviceId);
        }
    }
    @DeleteMapping("/delete/{promotionId}")
    public ResponseEntity<String> supprimerPromotion(@PathVariable Long promotionId) {
        try {
            // Vérifier si la promotion existe
            PromotionEntity promotion = promotionService.obtenirPromotionParId(promotionId);
            if (promotion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("La promotion avec l'ID " + promotionId + " n'existe pas.");
            }

            // Supprimer la promotion
            promotionService.supprimerPromotion(promotionId);

            return ResponseEntity.ok("La promotion avec l'ID " + promotionId + " et ses services associés ont été supprimés avec succès.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la suppression : " + e.getMessage());
        }
    }


   /* @PostMapping("/apply")
    public ResponseEntity<?> appliquerPromo(
        @RequestParam(required = false) String codePromo, 
        @RequestParam Long serviceId
    ) {
        try {
            PromotionEntity promo = null;

            
            if (codePromo != null && !codePromo.isEmpty()) {
                promo = promotionService.trouverParCode(codePromo);
                
                if (promo == null) {
                    return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Le code promo est invalide."));
                }

                if (!promo.getActif()) {
                    return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "La promotion avec ce code n'est pas active."));
                }
            } else {
                ServiceEntity service = serviceService.obtenirServiceParId(serviceId);
                if (service == null) {
                    return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Le service avec l'ID spécifié n'existe pas."));
                }

                if (service.getPromotion() != null) {
                    promo = service.getPromotion(); 
                } else {
                    return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Aucune promotion valide pour ce service."));
                }
            }

            
            LocalDate today = LocalDate.now();
            if (convertToLocalDate(promo.getDateDebut()).isAfter(today) || 
                convertToLocalDate(promo.getDateFin()).isBefore(today)) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "La promotion n'est pas valide pour la période actuelle."));
            }

            
            ServiceEntity service = serviceService.obtenirServiceParId(serviceId);
            if (!service.getPromotions().contains(promo)) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ce service n'est pas associé à cette promotion."));
            }

           
            double prixReduit = promotionService.calculerPrixAvecReduction(service, promo);
            return ResponseEntity.ok(Map.of(
                "serviceId", service.getId(),
                "serviceName", service.getTitre(),
                "originalPrice", service.getPrix(),
                "discountedPrice", prixReduit,
                "promotion", promo.getTauxReduction() + "% de réduction"
            ));

        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Une erreur est survenue: " + e.getMessage()));
        }
    }*/

    

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        return errorResponse;
    }



   
    private LocalDate convertToLocalDate(Timestamp timestamp) {
        return timestamp.toLocalDateTime().toLocalDate();
    }

    private double calculerPrixAvecReduction(ServiceEntity service, PromotionEntity promo) {
        double prixInitial = service.getPrix();

        switch (promo.getTypeReduction()) {
            case POURCENTAGE:
                return prixInitial * (1 - promo.getValeurReduction() / 100);
            case MONTANT_FIXE:
                return prixInitial - promo.getValeurReduction();
            default:
                return prixInitial;
        }
    }


    private LocalDate convertToLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

   
    @Transactional
    public void supprimerAssociationsParServiceId(Long serviceId) {
      
        Query query = entityManager.createNativeQuery(
            "DELETE FROM promotion_service WHERE service_id = :serviceId"
        );
        query.setParameter("serviceId", serviceId);
        int result = query.executeUpdate();

        if (result == 0) {
            System.out.println("No associations found for serviceId: " + serviceId);
        }
    }

    @Transactional
    public void supprimerAssociationsParPromotionIdEtServiceId(Long promotionId, Long serviceId) {
       
        Query query = entityManager.createNativeQuery(
            "DELETE FROM promotion_service WHERE promotion_id = :promotionId AND service_id = :serviceId"
        );
        query.setParameter("promotionId", promotionId);
        query.setParameter("serviceId", serviceId);
        int result = query.executeUpdate();

        if (result == 0) {
            System.out.println("No associations found for promotionId: " + promotionId + " and serviceId: " + serviceId);
        }
    }
  
    @PutMapping("/update/{promotionId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updatePromotion(
            @PathVariable Long promotionId,
            @Valid @RequestBody PromotionDTO promotionDTO) {
        try {
            // Vérifier si la promotion existe
            PromotionEntity existingPromo = promotionService.obtenirPromotionParId(promotionId);
            if (existingPromo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "La promotion avec l'ID spécifié n'existe pas."));
            }

            // Détacher les anciens services associés à la promotion
            List<ServiceEntity> anciensServices = new ArrayList<>(existingPromo.getServices()); // Copie pour éviter ConcurrentModificationException
            for (ServiceEntity ancienService : anciensServices) {
                // Supprimer l'association dans la table de jointure (promotion_service)
                supprimerAssociationsParPromotionIdEtServiceId(promotionId, ancienService.getId());

                // Vérifier si le service est toujours associé à une autre promotion
                if (ancienService.getPromotion() != null && ancienService.getPromotion().getId().equals(promotionId)) {
                    // Si le service n'est plus associé à aucune promotion, mettre promotion_id à null
                    ancienService.setPromotion(null);
                    serviceService.mettreAJourService(ancienService);
                }
            }
            existingPromo.getServices().clear();

            // Mettre à jour les champs de la promotion existante avec les données du DTO
            existingPromo.setCodePromo(promotionDTO.getCodePromo());
            existingPromo.setActif(promotionDTO.isActif());
            existingPromo.setDateDebut(promotionDTO.getDateDebut());
            existingPromo.setDateFin(promotionDTO.getDateFin());
            existingPromo.setValeurReduction(promotionDTO.getValeurReduction());

            // Vérification et conversion du TypeReduction
            try {
                existingPromo.setTypeReduction(TypeReduction.valueOf(promotionDTO.getTypeReduction().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Type de réduction invalide."));
            }

            // Ajouter les nouveaux services
            List<ServiceDTO> nouveauxServicesDTO = (promotionDTO.getServicesDTO() != null)
                    ? promotionDTO.getServicesDTO()
                    : new ArrayList<>();

            List<Map<String, Object>> servicesResponse = new ArrayList<>();

            for (ServiceDTO nouveauServiceDTO : nouveauxServicesDTO) {
                ServiceEntity serviceRecupere = serviceService.obtenirServiceParId(nouveauServiceDTO.getId());
                if (serviceRecupere != null) {
                    // Vérifier si le service est déjà associé à une autre promotion
                    if (serviceRecupere.getPromotion() != null && !serviceRecupere.getPromotion().getId().equals(promotionId)) {
                        // Supprimer l'ancienne association dans la table de jointure
                        supprimerAssociationsParServiceId(serviceRecupere.getId());
                    }

                    // Ajouter la promotion au service
                    serviceRecupere.setPromotion(existingPromo);
                    serviceService.mettreAJourService(serviceRecupere);

                    // Ajouter le service à la promotion
                    existingPromo.getServices().add(serviceRecupere);

                    // Préparer les détails du service pour la réponse
                    Map<String, Object> serviceDetails = new HashMap<>();
                    serviceDetails.put("serviceId", serviceRecupere.getId());
                    serviceDetails.put("titre", serviceRecupere.getTitre());
                    serviceDetails.put("actif", existingPromo.getActif());
                    serviceDetails.put("dateDebut", existingPromo.getDateDebut());
                    serviceDetails.put("dateFin", existingPromo.getDateFin());
                    serviceDetails.put("typeDeReduction", existingPromo.getTypeReduction());
                    serviceDetails.put("valeurReduction", existingPromo.getValeurReduction());

                    servicesResponse.add(serviceDetails);
                }
            }

            // Mettre à jour la promotion dans la base de données
            promotionService.mettreAJourPromotion(existingPromo);

            // Retourner la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("promotionId", existingPromo.getId());
            response.put("services", servicesResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Gestion des erreurs
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Une erreur est survenue lors de la mise à jour de la promotion : " + e.getMessage()));
        }
    }
    private Double calculerPrixAvecReduction(Double prixOriginal, PromotionEntity promo) {
        if (promo.getTypeReduction() == TypeReduction.POURCENTAGE) {
            return prixOriginal - (prixOriginal * promo.getValeurReduction() / 100);
        } else if (promo.getTypeReduction() == TypeReduction.MONTANT_FIXE) {
            return Math.max(prixOriginal - promo.getValeurReduction(), 0); 
        }
        return prixOriginal;
    }
   
    @GetMapping
    public ResponseEntity<List<PromotionDTO>> obtenirToutesLesPromotions() {
        try {
            // Récupérer toutes les promotions depuis le service
            List<PromotionEntity> promotions = promotionService.obtenirToutesLesPromotions();

            // Mapper chaque PromotionEntity vers PromotionDTO
            List<PromotionDTO> promotionsDTO = promotions.stream()
                .map(promotionEntity -> {
                    PromotionDTO promotionDTO = new PromotionDTO(promotionEntity);  // Mappe l'entité en DTO

                    // Récupérer les services associés à cette promotion depuis la table `services`
                    List<ServiceEntity> services = serviceService.getServicesByPromotionId(promotionEntity.getId());

                    // Mapper les services en ServiceDTO
                    List<ServiceDTO> servicesDTO = services.stream()
                        .map(service -> new ServiceDTO(service.getId(), service.getTitre())) // Extraire ID et titre
                        .collect(Collectors.toList());

                    // Assigner les servicesDTO au DTO
                    promotionDTO.setServicesDTO(servicesDTO);

                    return promotionDTO;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(promotionsDTO);
        } catch (Exception e) {
            // Gestion des erreurs
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countPromotions() {
        long count = promotionService.countPromotions();
        return ResponseEntity.ok(count);
    }

    }

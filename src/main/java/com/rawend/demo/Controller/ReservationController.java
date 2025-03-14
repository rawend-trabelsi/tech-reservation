package com.rawend.demo.Controller;

import com.rawend.demo.dto.ReservationRequest;
import com.rawend.demo.entity.ReservationEntity;
import com.rawend.demo.services.ReservationService;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping("/add")
    public Map<String, Object> addReservation(@RequestBody ReservationRequest request, Authentication authentication) {
        return reservationService.createReservation(request, authentication);
    }
}

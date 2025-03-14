package com.rawend.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class EmploiRequest {
    private String email;
    private String jourRepos;
    private LocalTime heureDebut;
    private LocalTime heureFin;
}

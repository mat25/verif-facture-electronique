package com.projet.api_validation.controller;

import com.projet.api_validation.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/validation")
@CrossOrigin(origins = "*") // Permet au Front-End d'appeler l'API
public class ValidationController {

    private final ValidationService validationService;

    // Injection du service
    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    /**
     * Endpoint pour envoyer une facture UBL
     * URL : POST http://localhost:8080/api/v1/validation/ubl
     */
    @PostMapping("/ubl")
    public ResponseEntity<?> validerUBL(@RequestParam("file") MultipartFile fichierXml,
            @RequestParam("format") String format) {
        if (fichierXml.isEmpty()) {
            return ResponseEntity.badRequest().body(List.of("Veuillez envoyer un fichier XML."));
        }

        try {
            List<String> resultats = validationService.validerFactureUBL(fichierXml, format);
            // Retourne 200 OK avec le tableau JSON des résultats
            return ResponseEntity.ok(resultats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(List.of("Erreur serveur : " + e.getMessage()));
        }
    }
}

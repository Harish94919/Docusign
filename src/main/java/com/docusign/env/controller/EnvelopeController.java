package com.docusign.env.controller;

import com.docusign.env.service.DocusignService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/envelopes")
@CrossOrigin(origins = "http://localhost:4200")
public class EnvelopeController {

    private final DocusignService docusignService;

    public EnvelopeController(DocusignService docusignService) {
        this.docusignService = docusignService;
    }

    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> sendEnvelope(
            @RequestParam("file") MultipartFile file,
            @RequestParam("signerEmail") String signerEmail,
            @RequestParam("signerName") String signerName) {

        Map<String, String> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("message", "File is required.");
                return ResponseEntity.badRequest().body(response);
            }

            String envelopeId = docusignService.sendEnvelope(signerEmail, signerName, file);
            response.put("message", "Envelope sent successfully!");
            response.put("envelopeId", envelopeId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Failed to send envelope: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ✅ Updated method with status validation before download
    @GetMapping(value = "/{envelopeId}/document", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getCompletedDocument(@PathVariable String envelopeId) {
        try {
            byte[] documentBytes = docusignService.getCompletedDocument(envelopeId);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"completed_document.pdf\"")
                    .body(documentBytes);
        } catch (IllegalStateException e) {
            // ⛔ Not signed yet
            return ResponseEntity.status(409).build(); // 409 Conflict
        } catch (Exception e) {
            // 🛑 Other errors
            return ResponseEntity.status(500).build();
        }
    }
}

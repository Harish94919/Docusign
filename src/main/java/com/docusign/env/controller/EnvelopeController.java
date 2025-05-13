package com.docusign.env.controller;

import com.docusign.env.service.DocusignService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/envelopes")
public class EnvelopeController {


    private final DocusignService docusignService;

    public EnvelopeController(DocusignService docusignService) {
        this.docusignService = docusignService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendEnvelope(@RequestParam("file") MultipartFile file,
                                               @RequestParam("signerEmail") String signerEmail,
                                               @RequestParam("signerName") String signerName) {

        String envelopeId = docusignService.sendEnvelope(signerEmail,signerName,file);
        return ResponseEntity.ok("Envelope sent! ID: " + envelopeId);
    }
}

package com.example.docusign.service;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.*;
import com.docusign.esign.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.UUID;
import java.util.Base64;

@Service
public class DocuSignService {

    private static final String INTEGRATOR_KEY = "YOUR_INTEGRATOR_KEY";
    private static final String USER_ID = "YOUR_IMPERSONATED_USER_ID";
    private static final String AUTH_SERVER = "account.docusign.com";
    private static final String PRIVATE_KEY_PATH = "private.pem";
    private static final String REDIRECT_URI = "https://localhost"; // can be any dummy redirect
    private static final String BASE_PATH = "https://demo.docusign.net/restapi";
    private static final String ACCOUNT_ID = "YOUR_ACCOUNT_ID";

    public String sendEnvelope(MultipartFile file, String signerName, String signerEmail) throws Exception {
        // Step 1: Authenticate via JWT
        ApiClient apiClient = new ApiClient(BASE_PATH);
        byte[] privateKeyBytes = getClass().getClassLoader().getResourceAsStream(PRIVATE_KEY_PATH).readAllBytes();

        OAuth.OAuthToken token = apiClient.requestJWTUserToken(
                INTEGRATOR_KEY,
                USER_ID,
                AUTH_SERVER,
                privateKeyBytes,
                3600
        );

        apiClient.setAccessToken(token.getAccessToken(), token.getExpiresIn());

        // Step 2: Configure envelope
        byte[] fileBytes = file.getBytes();
        String base64File = Base64.getEncoder().encodeToString(fileBytes);

        Document doc = new Document();
        doc.setDocumentBase64(base64File);
        doc.setName(file.getOriginalFilename());
        doc.setFileExtension(getExtension(file.getOriginalFilename()));
        doc.setDocumentId("1");

        Signer signer = new Signer();
        signer.setEmail(signerEmail);
        signer.setName(signerName);
        signer.setRecipientId("1");

        SignHere signHere = new SignHere();
        signHere.setDocumentId("1");
        signHere.setPageNumber("1");
        signHere.setRecipientId("1");
        signHere.setTabLabel("SignHereTab");
        signHere.setXPosition("100");
        signHere.setYPosition("150");

        Tabs tabs = new Tabs();
        tabs.setSignHereTabs(Collections.singletonList(signHere));
        signer.setTabs(tabs);

        Recipients recipients = new Recipients();
        recipients.setSigners(Collections.singletonList(signer));

        EnvelopeDefinition envelope = new EnvelopeDefinition();
        envelope.setEmailSubject("Please sign this document");
        envelope.setDocuments(Collections.singletonList(doc));
        envelope.setRecipients(recipients);
        envelope.setStatus("sent");

        // Step 3: Send envelope
        EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
        EnvelopeSummary result = envelopesApi.createEnvelope(ACCOUNT_ID, envelope);

        return result.getEnvelopeId();
    }

<<<<<<< HEAD
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
=======
    private String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
>>>>>>> 21d0804be78aaf7f670df816773d29c03b84f17c
    }
}

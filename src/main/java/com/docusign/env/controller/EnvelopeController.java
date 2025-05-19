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

    private String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}

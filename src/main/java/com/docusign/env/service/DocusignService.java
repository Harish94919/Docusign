package com.example.docusign.service;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.*;
import com.docusign.esign.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.UUID;
import java.util.Base64;

@Service
public class DocuSignService {

    @Value("${docusign.integrationKey}")
    private String clientId;

    @Value("${docusign.userId}")
    private String userId;

    @Value("${docusign.auth.baseUrl}")
    private String authBaseUrl;

    @Value("${docusign.api.baseUrl}")
    private String basePath;

    @Value("${docusign.privateKey}")
    private String privateKey; // Base64-encoded string of private.pem

    @Value("${docusign.accountId}")
    private String accountId;

    public String sendEnvelope(String signerEmail, String signerName, MultipartFile file) throws Exception {
        try {
            // 1. Authenticate using JWT
            ApiClient apiClient = new ApiClient(basePath);
            apiClient.setOAuthBasePath(authBaseUrl);

            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKey);

            OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(
                    clientId, userId, Collections.singletonList("signature"),
                    privateKeyBytes, 3600);

            apiClient.setAccessToken(oAuthToken.getAccessToken(), oAuthToken.getExpiresIn());

            Configuration.setDefaultApiClient(apiClient);

            // 2. Read uploaded file as Base64
            byte[] fileBytes = file.getBytes();
            String base64Doc = Base64.getEncoder().encodeToString(fileBytes);

            // 3. Create document and signer
            Document doc = new Document();
            doc.setDocumentBase64(base64Doc);
            doc.setName(file.getOriginalFilename());
            doc.setFileExtension(getFileExtension(file.getOriginalFilename()));
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
            signHere.setXPosition("200");
            signHere.setYPosition("200");

            Tabs tabs = new Tabs();
            tabs.setSignHereTabs(Collections.singletonList(signHere));
            signer.setTabs(tabs);

            EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();
            envelopeDefinition.setEmailSubject("Please sign this document");
            envelopeDefinition.setDocuments(Collections.singletonList(doc));
            envelopeDefinition.setRecipients(new Recipients().signers(Collections.singletonList(signer)));
            envelopeDefinition.setStatus("sent");

            // 4. Send envelope
            EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
            EnvelopeSummary result = envelopesApi.createEnvelope(accountId, envelopeDefinition);

            return result.getEnvelopeId();
        } catch (ApiException ex) {
            if (ex.getMessage().contains("consent_required")) {
                throw new Exception("Consent required. Visit:\nhttps://account-d.docusign.com/oauth/auth?response_type=code&scope=impersonation%20signature&client_id="
                        + clientId + "&redirect_uri=https://developers.docusign.com/platform/auth/consent");
            } else {
                throw new Exception("DocuSign API error: " + ex.getMessage());
            }
        } catch (Exception e) {
            throw new Exception("Unexpected error: " + e.getMessage());
        }
    }

<<<<<<< HEAD
    // ✅ FINAL: Only return document if envelope is completed
    public byte[] getCompletedDocument(String envelopeId) throws Exception {
        ApiClient apiClient = new ApiClient(basePath);
        apiClient.setOAuthBasePath(authServer);
        List<String> scopes = Arrays.asList("signature", "impersonation");
        byte[] privateKeyBytes = rsaKeyFile.getInputStream().readAllBytes();
        OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(clientId, userId, scopes, privateKeyBytes, 3600);
        String accessToken = oAuthToken.getAccessToken();

        OAuth.UserInfo userInfo = apiClient.getUserInfo(accessToken);
        String accountId = userInfo.getAccounts().get(0).getAccountId();
        apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);

        EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);

        // 🔍 Check if envelope is completed before returning document
        Envelope envelope = envelopesApi.getEnvelope(accountId, envelopeId);
        if (!"completed".equalsIgnoreCase(envelope.getStatus())) {
            throw new IllegalStateException("Document is not available until all recipients have signed.");
        }

        // ✅ Only now fetch the combined document
        return envelopesApi.getDocument(accountId, envelopeId, "combined");
    }

=======
>>>>>>> 21d0804be78aaf7f670df816773d29c03b84f17c
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "pdf";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
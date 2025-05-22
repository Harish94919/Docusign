package com.docusign.env.service;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
public class DocusignService {

    @Value("${docusign.clientId}")
    private String clientId;

    @Value("${docusign.userId}")
    private String userId;

    @Value("${docusign.rsaKeyPath}")
    private Resource rsaKeyFile;

    @Value("${docusign.basePath}")
    private String basePath;

    @Value("${docusign.authServer}")
    private String authServer;

    public String sendEnvelope(String signerEmail, String signerName, MultipartFile file) {
        try {
            ApiClient apiClient = new ApiClient(basePath);  //basepath means API Endpoint * endpoint for accesing DocuSign REST features
            apiClient.setOAuthBasePath(authServer);         //authServer means Authorization Server..//authServer is the domain that handles JWT OAuth authentication. continue to next line
            List<String> scopes = Arrays.asList("signature", "impersonation");                       //You send your private key and request an access token from this server.
            byte[] privateKeyBytes = rsaKeyFile.getInputStream().readAllBytes();//impersonations the person who is sending the mail.
            OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(clientId, userId, scopes, privateKeyBytes, 3600);
            String accessToken = oAuthToken.getAccessToken();

            OAuth.UserInfo userInfo = apiClient.getUserInfo(accessToken);
            String accountId = userInfo.getAccounts().get(0).getAccountId();

            apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);

            SignHere signHere = new SignHere()
                    .documentId("1")
                    .pageNumber("1")
                    .xPosition("456")
                    .yPosition("434");

            Tabs tabs = new Tabs().signHereTabs(Arrays.asList(signHere));

            Signer signer = new Signer()
                    .email(signerEmail)
                    .name(signerName)
                    .recipientId("1")
                    .tabs(tabs);

            CarbonCopy cc = new CarbonCopy()
                    .email("harishrajaboina9491@gmail.com")
                    .name("Diligent")
                    .recipientId("2");

            Recipients recipients = new Recipients();
            recipients.signers(Arrays.asList(signer));
            recipients.carbonCopies(Arrays.asList(cc));

            String fileName = file.getOriginalFilename();
            if (fileName == null) throw new IllegalAccessException("Invalid file");

            String ext = getFileExtension(fileName);
            List<String> allowedExtensions = Arrays.asList("pdf", "doc", "docx", "ppt", "pptx", "jpg", "jpeg", "png", "txt", "xls", "xlsx");
            if (!allowedExtensions.contains(ext)) {
                throw new IllegalArgumentException("Unsupported file type: " + ext);
            }

            byte[] fileBytes = file.getBytes();
            Document doc = new Document();
            doc.setDocumentBase64(Base64.getEncoder().encodeToString(fileBytes));
            doc.setName(file.getOriginalFilename());
            doc.setFileExtension(ext);
            doc.setDocumentId("1");

            EnvelopeDefinition envelope = new EnvelopeDefinition()
                    .emailSubject("Please sign this document set")
                    .status("sent")
                    .recipients(recipients)
                    .documents(Arrays.asList(doc));

            EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
            EnvelopeSummary results = envelopesApi.createEnvelope(accountId, envelope);

            return results.getEnvelopeId();
        } catch (ApiException ex) {
            if (ex.getMessage().contains("consent_required")) {
                return "Consent required. Visit:\nhttps://account-d.docusign.com/oauth/auth?response_type=code&scope=impersonation%20signature&client_id="
                        + clientId + "&redirect_uri=" + "https://developers.docusign.com/platform/auth/consent";
            } else {
                return "API Error: " + ex.getMessage();
            }
        } catch (Exception e) {
            return "Unexpected error: " + e.getMessage();
        }
    }

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

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex != -1) ? filename.substring(dotIndex + 1).toLowerCase() : "pdf";
    }
}

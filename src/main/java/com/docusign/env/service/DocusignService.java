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
    private String clientId;                //@value we need to write because we are getting
                                            //this values from application.properties.
    @Value("${docusign.userId}")
    private String userId;

    @Value("${docusign.rsaKeyPath}")
    private Resource rsaKeyFile;

    @Value("${docusign.basePath}")
    private String basePath;

    @Value("${docusign.authServer}")
    private String authServer;

    public String sendEnvelope(String signerEmail,String signerName,MultipartFile file) {
        try {   //get the access Token and  AccountId.
            ApiClient apiClient = new ApiClient(basePath);
            apiClient.setOAuthBasePath(authServer);              //impersonation: Acts on behalf of the user identified by userId.
            List<String> scopes = Arrays.asList("signature", "impersonation");  //scopes are nothing but actions.
            byte[] privateKeyBytes = rsaKeyFile.getInputStream().readAllBytes();//impersonation is nothing but who is sending the mail.
            OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(clientId, userId, scopes, privateKeyBytes, 3600);
            String accessToken = oAuthToken.getAccessToken();
            System.out.println(accessToken);
            OAuth.UserInfo userInfo = apiClient.getUserInfo(accessToken);
            String accountId = userInfo.getAccounts().get(0).getAccountId();

            apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);

            // Create tabs object
            SignHere signHere = new SignHere()  //where sign has to keep.
                    .documentId("1")
                    .pageNumber("1")
                    .xPosition("456")
                    .yPosition("434");

            Tabs tabs = new Tabs().signHereTabs(Arrays.asList(signHere));

            Signer signer = new Signer()
                    .email(signerEmail)             //details of recipient/customer
                    .name(signerName)
                    .recipientId("1")
                    .tabs(tabs);

            CarbonCopy cc = new CarbonCopy()
                    .email("harishrajaboina9491@gmail.com")   //meaning of carbon copy is
                    .name("Diligent")                        //details of the person who is sending the document to customer
                    .recipientId("2");

            Recipients recipients = new Recipients();
            recipients.signers(Arrays.asList(signer));
            recipients.carbonCopies(Arrays.asList(cc));

            byte[] fileBytes =file.getBytes();
            System.out.println(fileBytes);
            Document doc = new Document();   //Add Document
            doc.setDocumentBase64(Base64.getEncoder().encodeToString(fileBytes));
            doc.setName(file.getOriginalFilename());
            doc.setFileExtension("pdf");
            doc.setDocumentId("1");


            EnvelopeDefinition envelope = new EnvelopeDefinition()
                    .emailSubject("Please sign this document set")
                    .status("sent")
                    .recipients(recipients)
                    .documents(Arrays.asList(doc));

            EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
            EnvelopeSummary results = envelopesApi.createEnvelope(accountId, envelope);

            return "Envelope sent successfully! ID: " + results.getEnvelopeId();
        } catch (ApiException ex) {
            if (ex.getMessage().contains("consent_required")) {
                return "Consent required. Visit:\nhttps://account-d.docusign.com/oauth/auth?response_type=code&scope=impersonation%20signature&client_id="
                        + clientId + "&redirect_uri=" + "https://developers.docusign.com/platform/auth/consent";
            }
               //here client_id=integration key.we need to open it in browser and paste the integration key and redirect_uri.

            else {
                return "API Error: " + ex.getMessage();
            }
        } catch (Exception e) {
            return "Unexpected error: " + e.getMessage();
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex != -1) ? filename.substring(dotIndex + 1) : "pdf";
    }
}



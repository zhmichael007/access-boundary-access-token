package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.CredentialAccessBoundary;
import com.google.auth.oauth2.DownscopedCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.iam.credentials.v1.GenerateAccessTokenRequest;
import com.google.cloud.iam.credentials.v1.GenerateAccessTokenResponse;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.IamCredentialsSettings;
import com.google.iam.admin.v1.ServiceAccountName;
import com.google.protobuf.Duration;

/** Demonstrates how to use Downscoping with Credential Access Boundaries. */
public class AccessBoundaryToken {
    static String serviceAccountKeyFile = "";
    static String serviceAccountTokenValue = "";

    /**
     * set the service account key file
     * @param keyfile  the path of service account key file
     */
    public static void setServiceAccountKeyFile(String keyfile) {
        serviceAccountKeyFile = keyfile;
    }

    /**
     * Refresh the service account token, The downscope token for GCS will generated
     * from this token and copy the expire time.
     * So must refresh the service account token each N minutes, Ex, N=5 or N=15
     * The token value will be saved in static variable serviceAccountTokenValue
     *
     * @param keyfile  the path of service account key file
     * @param lifetime the duration of the expire time in second, for
     *                 example: 3600 (1hour), 43200 (12 hour)
     * @return The token value
     */
    public static String RefreshServiceAccountToken(int lifetime) throws Exception {
        if ("" == serviceAccountKeyFile) {
            throw new IOException(
                    "AccessBoundaryToken.serviceAccountKeyFile is empty, please call setServiceAccountKeyFile first to set the service account key file");
        }
        File credentialsPath = new File(serviceAccountKeyFile);
        FileInputStream serviceAccountStream = new FileInputStream(credentialsPath);
        GoogleCredentials sourceCredentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
        if (sourceCredentials.createScopedRequired()) {
            sourceCredentials = sourceCredentials
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
        }

        // get the service account name from the key file
        JSONParser jsonParser = new JSONParser();
        String client_email = "";
        FileReader reader = new FileReader(serviceAccountKeyFile);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
        client_email = (String) jsonObject.get("client_email");

        IamCredentialsSettings iamCredentialsSettings = IamCredentialsSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(sourceCredentials))
                .build();
        IamCredentialsClient iamCredentialsClient = IamCredentialsClient.create(iamCredentialsSettings);

        String name = ServiceAccountName.of("-", client_email).toString();
        // List<String> delegates = new ArrayList<>();
        List<String> scope = new ArrayList<>();
        Duration duration = Duration.newBuilder().setSeconds(lifetime).build();
        scope.add("https://www.googleapis.com/auth/cloud-platform");
        GenerateAccessTokenRequest request = GenerateAccessTokenRequest
                .newBuilder()
                .setName(name)
                .addAllScope(scope)
                .setLifetime(duration)
                .build();
        GenerateAccessTokenResponse response = iamCredentialsClient.generateAccessToken(request);

        serviceAccountTokenValue = response.getAccessToken();

        iamCredentialsClient.close();

        return serviceAccountTokenValue;
    }

    /**
     * generate the access token according to the bucket name and object prefix 
     *
     * @param bucketName  the name of the bucket
     * @param objectPrefix the object prefix (like folder)
     * 
     */
    public static AccessToken generateAccessBoundaryToken(String bucketName,
            String objectPrefix)
            throws IOException {
        String availableResource = "//storage.googleapis.com/projects/_/buckets/" + bucketName;
        String availablePermission = "inRole:roles/storage.objectAdmin";
        String expression = "resource.name.startsWith('projects/_/buckets/"
                + bucketName
                + "/objects/"
                + objectPrefix
                + "')";

        if ("" == serviceAccountTokenValue) {
            throw new IOException(
                    "AccessBoundaryToken.serviceAccountTokenValue is empty, please call RefreshServiceAccountToken to generate the service account access token");
        }

        AccessToken serviceAccountAccessToken = new AccessToken(serviceAccountTokenValue, null);
        GoogleCredentials serviceAccountCredentials = new GoogleCredentials(serviceAccountAccessToken);

        // Build the AvailabilityCondition.
        CredentialAccessBoundary.AccessBoundaryRule.AvailabilityCondition availabilityCondition = CredentialAccessBoundary.AccessBoundaryRule.AvailabilityCondition
                .newBuilder()
                .setExpression(expression)
                .build();

        // Define the single access boundary rule using the above properties.
        CredentialAccessBoundary.AccessBoundaryRule rule = CredentialAccessBoundary.AccessBoundaryRule.newBuilder()
                .setAvailableResource(availableResource)
                .addAvailablePermission(availablePermission)
                .setAvailabilityCondition(availabilityCondition)
                .build();

        // Define the Credential Access Boundary with all the relevant rules.
        CredentialAccessBoundary credentialAccessBoundary = CredentialAccessBoundary.newBuilder().addRule(rule).build();
        // [END auth_downscoping_rules]

        DownscopedCredentials downscopedCredentials = DownscopedCredentials.newBuilder()
                .setSourceCredential(serviceAccountCredentials)
                .setCredentialAccessBoundary(credentialAccessBoundary)
                .build();

        // Retrieve the token.
        // This will need to be passed to the Token Consumer.
        AccessToken accessToken = downscopedCredentials.refreshAccessToken();
        return accessToken;
    }
}

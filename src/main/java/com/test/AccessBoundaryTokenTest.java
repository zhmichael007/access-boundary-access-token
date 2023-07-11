package com.test;

import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import com.google.auth.oauth2.AccessToken;

/** Demonstrates how to use Downscoping with Credential Access Boundaries. */
public class AccessBoundaryTokenTest {
    public static void main(String[] args) throws IOException {
        // service account key file
        String saKeyFile = "./gcs_sa_key.json";
        // The Cloud Storage bucket name.
        String bucketName = "gcp-sts-test";
        // The Cloud Storage object prefix that resides in the specified bucket.
        String objectPrefix = "device1/1/2/3/";
        // token expire-in, seconds, max 12 hours, 43200 seconds
        int lifetime = 43200;
        // service account token refersh interval, ms
        long saTokenRefreshInterval = 1000 * 60;

        AccessBoundaryToken.setServiceAccountKeyFile(saKeyFile);
        try {
            AccessBoundaryToken.RefreshServiceAccountToken(lifetime);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // set a timer to refresh service account access token intervally
        Timer timer = new Timer();
        TimerTask ServiceAccountTokenRefreshTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    String token = AccessBoundaryToken.RefreshServiceAccountToken(lifetime);
                    System.out.println("refresh service account token");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.scheduleAtFixedRate(ServiceAccountTokenRefreshTask,
                Calendar.getInstance().getTime(),
                saTokenRefreshInterval);

        // generate the access boundary access token every 5 seconds
        while (true) {
            try {
                AccessToken accessToken = AccessBoundaryToken.generateAccessBoundaryToken(bucketName, objectPrefix);
                System.out.println(accessToken.getTokenValue());
                System.out.println("Expiration Time: " + accessToken.getExpirationTime());
            System.out.println("");
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

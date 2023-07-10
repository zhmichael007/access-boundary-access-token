package com.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;

import com.google.auth.oauth2.AccessToken;

class MyThreadTest extends Thread {
    private int i = 0;
    private String bucketName = "";
    private String objectPrefix = "";

    MyThreadTest(String bucketName, String objectPrefix) {
        this.bucketName = bucketName;
        this.objectPrefix = objectPrefix;
    }

    @Override
    public void run() {
        System.out.println("MyThreadTest " + Thread.currentThread().getName().toString());
        int i = 0;
        // System.out.print(token_value);
        long d1 = System.currentTimeMillis();
        int num = 200000;
        while (i < num) {
            try {
                i++;
                AccessToken accessToken = AccessBoundaryToken.generateAccessBoundaryToken(bucketName, objectPrefix);
                if (i == 1 || i == num) {
                    System.out.println(accessToken.getTokenValue());
                    // System.out.println("Expire Time " + i + ": " +
                    // accessToken.getExpirationTime());
                }
                System.out
                        .println(Thread.currentThread().getName().toString() + ": " + accessToken.getExpirationTime());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long d2 = System.currentTimeMillis();
        System.out.println("time used: " + (d2 - d1) / 1000);
    }

}

/** Demonstrates how to use Downscoping with Credential Access Boundaries. */
public class AccessBoundaryTokenTest {
    public static void main(String[] args) throws IOException {
        // String saKeyFile = "./gcs-key-2.json";
        String saKeyFile = "./gcs_sa_key.json";
        // The Cloud Storage bucket name.
        String bucketName = "gcs-token";
        // The Cloud Storage object prefix that resides in the specified bucket.
        String objectPrefix = "device1/1/2/3/";
        int lifetime = 43200; // token expire-in, seconds, max 12 hours, 43200 seconds
        long saTokenRefreshInterval = 1000 * 60; // ms

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

        //generate the access boundary access token every 5 second
        while (true) {
            AccessToken accessToken = AccessBoundaryToken.generateAccessBoundaryToken(bucketName, objectPrefix);
            System.out.println(accessToken.getTokenValue());
            System.out.println("Expiration Time: "+accessToken.getExpirationTime());
            System.out.println("");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

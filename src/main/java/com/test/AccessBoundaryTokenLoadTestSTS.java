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
        int num = 100000;
        while (i < num) {
            try {
                i++;
                AccessToken accessToken = AccessBoundaryToken.generateAccessBoundaryToken(bucketName, objectPrefix);
                if (i == 1 || i == num) {
                    System.out.println(accessToken.getTokenValue());
                    System.out.println(Thread.currentThread().getName().toString() + ": Expire Time: "+accessToken.getExpirationTime());
                }
                // System.out.println(Thread.currentThread().getName().toString() + ": "+accessToken.getExpirationTime());
                System.out.println(Thread.currentThread().getName().toString() + " : "+i);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(250);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long d2 = System.currentTimeMillis();
        System.out.println(Thread.currentThread().getName().toString() + ": Time Used: " + (d2 - d1) / 1000 + " Start Time: "+d1+", Current time: "+d2);
    }

}

/** Demonstrates how to use Downscoping with Credential Access Boundaries. */
public class AccessBoundaryTokenLoadTestSTS {
    public static void main(String[] args) throws IOException {
        String saKeyFile = "./gcs_sa_key.json";
        // The Cloud Storage bucket name.
        String bucketName = "gcp-sts-test";
        // The Cloud Storage object prefix that resides in the specified bucket.
        String objectPrefix = "device1/1/2/3/";
        // token expire-in, seconds, max 12 hours, 43200 seconds
        int lifetime = 43200;
        long saTokenRefreshInterval = 1000*60; //ms

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
        timer.scheduleAtFixedRate(ServiceAccountTokenRefreshTask, Calendar.getInstance().getTime(), saTokenRefreshInterval);

        AccessToken accessToken = AccessBoundaryToken.generateAccessBoundaryToken(bucketName, objectPrefix);
        System.out.println(accessToken.getTokenValue());

        int threadNum = 50;
        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < threadNum; i++) {
            Thread myThread = new MyThreadTest(bucketName, objectPrefix);
            threadList.add(myThread);
            myThread.start();
        }

        boolean allTerminated = false;
        while (false == allTerminated) {
            allTerminated = true;
            for (int i = 0; i < threadList.size(); i++) {
                Thread thread = threadList.get(i);
                if (!(thread.getState().toString().equals("TERMINATED"))) {
                    allTerminated = false;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (false == allTerminated) {
                System.out.println("Thread not all terminated!");
            }
        }

        System.out.println("All token thread terminated");
        timer.cancel();
    }
}
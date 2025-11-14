package com.photospot.fotospotapp;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import java.util.Arrays;
import java.util.List;

public class AdmobHelper {

    private static boolean initialized = false;

    public static void initAdmob(Context context) {
        if (initialized) return;

        // 1) Test-Geräte eintragen (DEINE ID aus dem Logcat)
        List<String> testDevices = Arrays.asList(
                "75FE8AC7A2779C3FBE59CAAB0DC63BF5"
        );

        RequestConfiguration config = new RequestConfiguration.Builder()
                .setTestDeviceIds(testDevices)
                .build();
        MobileAds.setRequestConfiguration(config);

        // 2) AdMob initialisieren
        MobileAds.initialize(context, initializationStatus -> {
            Log.d("ADMOB", "MobileAds initialized mit TestDevices: " + testDevices);
        });

        initialized = true;
    }
}
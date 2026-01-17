package com.wesaphzt.privatelock.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.wesaphzt.privatelock.service.LockService;

import java.util.HashSet;
import java.util.Set;

public class WifiReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiReceiver";
    public static final String TRUSTED_WIFI_KEY = "TRUSTED_WIFI_LIST";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && 
            (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION) ||
             intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))) {
            
            checkWifiConnection(context);
        }
    }

    private void checkWifiConnection(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (wifiManager == null || connectivityManager == null) {
            return;
        }

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnectedToWifi = networkInfo != null && 
                                    networkInfo.getType() == ConnectivityManager.TYPE_WIFI && 
                                    networkInfo.isConnected();

        if (isConnectedToWifi) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                // Remove quotes from SSID
                if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }

                Log.d(TAG, "Connected to WiFi: " + ssid);

                if (isTrustedWifi(context, ssid)) {
                    Log.d(TAG, "Connected to trusted WiFi, disabling lock service");
                    LockService.disabled = true;
                } else {
                    Log.d(TAG, "Connected to untrusted WiFi, enabling lock service");
                    LockService.disabled = false;
                }
            }
        } else {
            Log.d(TAG, "Not connected to WiFi, enabling lock service");
            LockService.disabled = false;
        }
    }

    private boolean isTrustedWifi(Context context, String ssid) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> trustedWifis = prefs.getStringSet(TRUSTED_WIFI_KEY, new HashSet<String>());
        return trustedWifis.contains(ssid);
    }

    public static void checkInitialWifiState(Context context) {
        WifiReceiver receiver = new WifiReceiver();
        receiver.checkWifiConnection(context);
    }
}

package com.wesaphzt.privatelock.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.annotation.Nullable;

import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.wesaphzt.privatelock.R;
import com.wesaphzt.privatelock.receivers.WifiReceiver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FragmentSettings extends PreferenceFragmentCompat {

    private Context context;

    private CheckBoxPreference cbRunConstant;
    private Preference prefAddTrustedWifi;
    private Preference prefManageTrustedWifi;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        setHasOptionsMenu(true);
        context = getContext();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        //this static call will reset default values only on the first read
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

        cbRunConstant = (CheckBoxPreference) findPreference("RUN_CONSTANT");
        prefAddTrustedWifi = findPreference("TRUSTED_WIFI_ADD");
        prefManageTrustedWifi = findPreference("TRUSTED_WIFI_MANAGE");

        if (prefAddTrustedWifi != null) {
            prefAddTrustedWifi.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    addCurrentWifi();
                    return true;
                }
            });
        }

        if (prefManageTrustedWifi != null) {
            prefManageTrustedWifi.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showManageTrustedWifiDialog();
                    return true;
                }
            });
        }
    }

    private void addCurrentWifi() {
        String currentSsid = getCurrentWifiSsid();
        if (currentSsid == null) {
            Toast.makeText(context, R.string.trusted_wifi_not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> trustedWifis = sharedPreferences.getStringSet(WifiReceiver.TRUSTED_WIFI_KEY, new HashSet<String>());
        if (trustedWifis == null) {
            trustedWifis = new HashSet<>();
        }
        Set<String> updatedWifis = new HashSet<>(trustedWifis);

        if (updatedWifis.contains(currentSsid)) {
            Toast.makeText(context, R.string.trusted_wifi_already_added, Toast.LENGTH_SHORT).show();
            return;
        }

        updatedWifis.add(currentSsid);
        sharedPreferences.edit().putStringSet(WifiReceiver.TRUSTED_WIFI_KEY, updatedWifis).apply();
        String message = getString(R.string.trusted_wifi_added) + ": " + currentSsid;
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        
        // Re-check WiFi state
        WifiReceiver.checkInitialWifiState(context);
    }

    private void showManageTrustedWifiDialog() {
        Set<String> trustedWifis = sharedPreferences.getStringSet(WifiReceiver.TRUSTED_WIFI_KEY, new HashSet<String>());
        if (trustedWifis == null) {
            trustedWifis = new HashSet<>();
        }
        
        if (trustedWifis.isEmpty()) {
            Toast.makeText(context, R.string.trusted_wifi_none, Toast.LENGTH_SHORT).show();
            return;
        }

        final List<String> wifiList = new ArrayList<>(trustedWifis);
        final boolean[] checkedItems = new boolean[wifiList.size()];

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.trusted_wifi_manage);
        builder.setMultiChoiceItems(wifiList.toArray(new CharSequence[0]), checkedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checkedItems[which] = isChecked;
                    }
                });
        builder.setPositiveButton(R.string.trusted_wifi_remove, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Set<String> updatedWifis = new HashSet<>(wifiList);
                for (int i = 0; i < checkedItems.length; i++) {
                    if (checkedItems[i]) {
                        updatedWifis.remove(wifiList.get(i));
                    }
                }
                sharedPreferences.edit().putStringSet(WifiReceiver.TRUSTED_WIFI_KEY, updatedWifis).apply();
                Toast.makeText(context, R.string.trusted_wifi_removed, Toast.LENGTH_SHORT).show();
                
                // Re-check WiFi state
                WifiReceiver.checkInitialWifiState(context);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private String getCurrentWifiSsid() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return null;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return null;
        }

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                // Remove quotes from SSID
                if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                return ssid;
            }
        }
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //set title
        getActivity().setTitle("Settings");

        //bg color
        view.setBackgroundColor(getResources().getColor(R.color.white));

        sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if(key.equals("RUN_CONSTANT") && cbRunConstant.isChecked()) {
                    Toast.makeText(context, getString(R.string.settings_restart_service_toast), Toast.LENGTH_LONG).show();
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        //hide action bar menu
        menu.setGroupVisible(R.id.menu_main, false);

        super.onPrepareOptionsMenu(menu);
    }
}
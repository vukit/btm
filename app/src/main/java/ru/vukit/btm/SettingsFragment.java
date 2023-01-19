package ru.vukit.btm;

import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

@Keep
public class SettingsFragment extends PreferenceFragmentCompat {

    final SettingsModel model = SettingsModel.getInstance();

    public static final String KEY_STARTUP_SCREEN = "STARTUP_SCREEN";
    public static final String KEY_DISCOVERY_DURATION = "DISCOVERY_DURATION";
    public static final String KEY_SEARCH_DEFAULT_MODE = "SEARCH_DEFAULT_MODE";
    public static final String KEY_ABOUT_DEVICE = "ABOUT_DEVICE";
    public static final String KEY_RECEIVING_SERVER_REPORT_URL = "RECEIVING_SERVER_REPORT_URL";
    public static final String KEY_RECEIVING_SERVER_REPORT_USERNAME = "RECEIVING_SERVER_REPORT_USERNAME";
    public static final String KEY_RECEIVING_SERVER_REPORT_PASSWORD = "RECEIVING_SERVER_REPORT_PASSWORD";
    public static final String KEY_RECEIVING_SERVER_REPORT_PROTOCOLS = "RECEIVING_SERVER_REPORT_PROTOCOLS";
    public static final String KEY_DASHBOARD_SEARCH_AREA_SIZE = "DASHBOARD_SEARCH_AREA_SIZE";
    public static final String KEY_DEVICE_DETAILS_STARTUP_SCREEN = "DEVICE_DETAILS_STARTUP_SCREEN";
    public static final String KEY_TERMINAL_CONNECTION_MODE = "TERMINAL_CONNECTION_MODE";
    public static final String KEY_TERMINAL_SEND_CR = "TERMINAL_SEND_CR";
    public static final String KEY_TERMINAL_SEND_LF = "TERMINAL_SEND_LF";
    public static final String KEY_TERMINAL_INPUT_FORMAT = "TERMINAL_INPUT_FORMAT";
    public static final String KEY_TERMINAL_OUTPUT_FORMAT = "TERMINAL_OUTPUT_FORMAT";
    public static final String KEY_TERMINAL_MY_UUID = "TERMINAL_MY_UUID";
    public static final String KEY_TERMINAL_COMMAND_1 = "TERMINAL_COMMAND_1";
    public static final String KEY_TERMINAL_COMMAND_2 = "TERMINAL_COMMAND_2";
    public static final String KEY_TERMINAL_COMMAND_3 = "TERMINAL_COMMAND_3";
    public static final String KEY_TERMINAL_COMMAND_4 = "TERMINAL_COMMAND_4";
    public static final String KEY_TERMINAL_COMMAND_5 = "TERMINAL_COMMAND_5";
    public static final String KEY_GATT_READING_TIMEOUT = "GATT_READING_TIMEOUT";
    public static final String KEY_GATT_AUTO_CONNECT = "GATT_AUTO_CONNECT";
    public static final String KEY_DEVICES_DETAILS_READING_GATT_CHARACTERISTICS = "DEVICES_DETAILS_READING_GATT_CHARACTERISTICS";
    public static final String KEY_DEVICES_DETAILS_SERVICES_LIST_STATE = "DEVICES_DETAILS_SERVICES_LIST_STATE";
    public static final String KEY_LOCATION_PROVIDER = "LOCATION_PROVIDER";
    public static final String KEY_BLUETOOTH_TURN_OFF = "BLUETOOTH_TURN_OFF";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {

    }

    @Override
    public void onResume() {
        super.onResume();
        model.connectController(this);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getResources().getString(R.string.app_name_short) + " - " + getResources().getString(R.string.action_settings));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        model.disconnectController();
    }

}

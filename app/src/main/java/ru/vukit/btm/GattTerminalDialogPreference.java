package ru.vukit.btm;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import static ru.vukit.btm.SettingsFragment.KEY_DEVICES_DETAILS_READING_GATT_CHARACTERISTICS;
import static ru.vukit.btm.SettingsFragment.KEY_DEVICES_DETAILS_SERVICES_LIST_STATE;
import static ru.vukit.btm.SettingsFragment.KEY_GATT_READING_TIMEOUT;
import static ru.vukit.btm.SettingsFragment.KEY_GATT_AUTO_CONNECT;

import androidx.annotation.Keep;

@Keep
public class GattTerminalDialogPreference {

    private final Context context;
    private View view;

    public GattTerminalDialogPreference(Context context) {
        this.context = context;
    }

    View getView(SharedPreferences sharedPreferences) {
        view = View.inflate(context, R.layout.gatt_terminal_preferences, null);
        EditText editText;
        CheckBox checkBox;
        checkBox = view.findViewById(R.id.read_all_characteristics_at_once);
        checkBox.setChecked(sharedPreferences.getBoolean(KEY_DEVICES_DETAILS_READING_GATT_CHARACTERISTICS, true));
        checkBox = view.findViewById(R.id.expand_list_of_services);
        checkBox.setChecked(sharedPreferences.getBoolean(KEY_DEVICES_DETAILS_SERVICES_LIST_STATE, true));
        checkBox = view.findViewById(R.id.auto_connect);
        checkBox.setChecked(sharedPreferences.getBoolean(KEY_GATT_AUTO_CONNECT, false));
        editText = view.findViewById(R.id.reading_timeout);
        editText.setText(sharedPreferences.getString(KEY_GATT_READING_TIMEOUT, "200"));
        return view;
    }

    void savePreference(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_DEVICES_DETAILS_READING_GATT_CHARACTERISTICS, ((CheckBox) view.findViewById(R.id.read_all_characteristics_at_once)).isChecked());
        editor.putBoolean(KEY_DEVICES_DETAILS_SERVICES_LIST_STATE, ((CheckBox) view.findViewById(R.id.expand_list_of_services)).isChecked());
        editor.putBoolean(KEY_GATT_AUTO_CONNECT, ((CheckBox) view.findViewById(R.id.auto_connect)).isChecked());
        int reading_timeout = Integer.parseInt(((EditText) view.findViewById(R.id.reading_timeout)).getText().toString().trim());
        if (reading_timeout <= 0) editor.putString(KEY_GATT_READING_TIMEOUT, "200");
        else editor.putString(KEY_GATT_READING_TIMEOUT, String.valueOf(reading_timeout));
        editor.apply();
    }
}

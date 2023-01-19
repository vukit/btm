package ru.vukit.btm;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_CONNECTION_MODE;
import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_MY_UUID;
import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_COMMAND_1;
import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_COMMAND_2;
import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_COMMAND_3;
import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_COMMAND_4;
import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_COMMAND_5;
import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_INPUT_FORMAT;
import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_OUTPUT_FORMAT;
import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_SEND_CR;
import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_SEND_LF;

import androidx.annotation.Keep;

@Keep
public class TerminalDialogPreference {

    private View view;
    private final Context context;

    public TerminalDialogPreference(Context context) {
        this.context = context;
    }

    View getView(SharedPreferences sharedPreferences) {
        view = View.inflate(context, R.layout.terminal_preferences, null);
        EditText editText;
        RadioButton radioButton;
        CheckBox checkBox;
        if (sharedPreferences.getBoolean(KEY_TERMINAL_CONNECTION_MODE, true)) {
            radioButton = view.findViewById(R.id.connection_mode_insecure);
            radioButton.setChecked(true);
        } else {
            radioButton = view.findViewById(R.id.connection_mode_secure);
            radioButton.setChecked(true);
        }
        checkBox = view.findViewById(R.id.output_send_cr);
        checkBox.setChecked(sharedPreferences.getBoolean(KEY_TERMINAL_SEND_CR, true));
        checkBox = view.findViewById(R.id.output_send_lf);
        checkBox.setChecked(sharedPreferences.getBoolean(KEY_TERMINAL_SEND_LF, false));
        if (sharedPreferences.getString(KEY_TERMINAL_OUTPUT_FORMAT, "Text").equals("Text")) {
            radioButton = view.findViewById(R.id.output_text_format);
            radioButton.setChecked(true);
        } else {
            radioButton = view.findViewById(R.id.output_hex_format);
            radioButton.setChecked(true);
        }
        if (sharedPreferences.getString(KEY_TERMINAL_INPUT_FORMAT, "Text").equals("Text")) {
            radioButton = view.findViewById(R.id.input_text_format);
            radioButton.setChecked(true);
        } else {
            radioButton = view.findViewById(R.id.input_hex_format);
            radioButton.setChecked(true);
        }
        editText = view.findViewById(R.id.terminal_my_uuid);
        editText.setText(sharedPreferences.getString(KEY_TERMINAL_MY_UUID, ""));
        editText = view.findViewById(R.id.terminal_command_1);
        editText.setText(sharedPreferences.getString(KEY_TERMINAL_COMMAND_1, ""));
        editText = view.findViewById(R.id.terminal_command_2);
        editText.setText(sharedPreferences.getString(KEY_TERMINAL_COMMAND_2, ""));
        editText = view.findViewById(R.id.terminal_command_3);
        editText.setText(sharedPreferences.getString(KEY_TERMINAL_COMMAND_3, ""));
        editText = view.findViewById(R.id.terminal_command_4);
        editText.setText(sharedPreferences.getString(KEY_TERMINAL_COMMAND_4, ""));
        editText = view.findViewById(R.id.terminal_command_5);
        editText.setText(sharedPreferences.getString(KEY_TERMINAL_COMMAND_5, ""));
        return view;
    }

    void savePreference(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_TERMINAL_CONNECTION_MODE, ((RadioButton) view.findViewById(R.id.connection_mode_insecure)).isChecked());
        editor.putBoolean(KEY_TERMINAL_SEND_CR, ((CheckBox) view.findViewById(R.id.output_send_cr)).isChecked());
        editor.putBoolean(KEY_TERMINAL_SEND_LF, ((CheckBox) view.findViewById(R.id.output_send_lf)).isChecked());
        if (((RadioButton) view.findViewById(R.id.output_text_format)).isChecked())
            editor.putString(KEY_TERMINAL_OUTPUT_FORMAT, "Text");
        if (((RadioButton) view.findViewById(R.id.output_hex_format)).isChecked())
            editor.putString(KEY_TERMINAL_OUTPUT_FORMAT, "Hex");
        if (((RadioButton) view.findViewById(R.id.input_text_format)).isChecked())
            editor.putString(KEY_TERMINAL_INPUT_FORMAT, "Text");
        if (((RadioButton) view.findViewById(R.id.input_hex_format)).isChecked())
            editor.putString(KEY_TERMINAL_INPUT_FORMAT, "Hex");
        String my_uuid = ((EditText) view.findViewById(R.id.terminal_my_uuid)).getText().toString().trim();
        if (!my_uuid.equals("00001101-0000-1000-8000-00805f9b34fb") && !my_uuid.equals("00001103-0000-1000-8000-00805f9b34fb"))
            editor.putString(KEY_TERMINAL_MY_UUID, my_uuid);
        else
            editor.putString(KEY_TERMINAL_MY_UUID, "");
        editor.putString(KEY_TERMINAL_COMMAND_1, ((EditText) view.findViewById(R.id.terminal_command_1)).getText().toString());
        editor.putString(KEY_TERMINAL_COMMAND_2, ((EditText) view.findViewById(R.id.terminal_command_2)).getText().toString());
        editor.putString(KEY_TERMINAL_COMMAND_3, ((EditText) view.findViewById(R.id.terminal_command_3)).getText().toString());
        editor.putString(KEY_TERMINAL_COMMAND_4, ((EditText) view.findViewById(R.id.terminal_command_4)).getText().toString());
        editor.putString(KEY_TERMINAL_COMMAND_5, ((EditText) view.findViewById(R.id.terminal_command_5)).getText().toString());
        editor.apply();
    }
}

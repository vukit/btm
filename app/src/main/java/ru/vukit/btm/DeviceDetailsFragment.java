package ru.vukit.btm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static ru.vukit.btm.SettingsFragment.KEY_TERMINAL_MY_UUID;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;

import ru.vukit.btm.bluetooth.BluetoothDriver;
import ru.vukit.btm.bluetooth.BluetoothScanRecord;
import ru.vukit.btm.bluetooth.BluetoothGattUUIDs;
import ru.vukit.btm.bluetooth.BluetoothSDP;
import ru.vukit.btm.database.DatabaseDriver;

@Keep
public class DeviceDetailsFragment extends Fragment implements OnMapReadyCallback {

    final DeviceDetailsModel model = DeviceDetailsModel.getInstance();
    final BluetoothDriver btDriver = BluetoothDriver.getInstance();
    final DatabaseDriver dbDatabaseDriver = DatabaseDriver.getInstance();
    SharedPreferences sharedPreferences;
    MapView mapViewHome;
    GoogleMap map;
    Marker marker;
    FloatingActionButton fab_main, fab_home, fab_services, fab_terminal;
    RunFragment runFragment;
    ExpandableListView device_details_services_view_gatt_services;
    ScrollView devices_details_services_view_gatt_characteristic;
    ImageButton device_details_services_settings, device_details_services_list_commands;
    ScrollView device_details_services_view_gatt_log;
    TextView device_details_services_gatt_log;
    TextView device_details_services_scan_record;
    TextView device_details_services_from_sdp_empty;
    ListView device_details_services_from_sdp;
    EditText device_details_terminal_command;
    ImageButton device_details_terminal_settings, device_details_terminal_list_commands, device_details_terminal_command_send;
    TextView device_details_terminal_log;
    RadioButton radio_button_spp, radio_button_dial_up, radio_button_my_service;
    WebSocket wsSocket;
    ServicesListAdapter servicesListAdapter;

    public DeviceDetailsFragment() {
    }

    private class ServicesListAdapter extends BaseExpandableListAdapter {

        final Context context;
        final ArrayList<HashMap<String, String>> services;
        final ArrayList<ArrayList<HashMap<String, String>>> characteristics;

        ServicesListAdapter(Context context, ArrayList<HashMap<String, String>> services, ArrayList<ArrayList<HashMap<String, String>>> characteristics) {
            this.context = context;
            this.services = services;
            this.characteristics = characteristics;
        }

        @Override
        public int getGroupCount() {
            return services.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return characteristics.get(groupPosition).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return services.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return characteristics.get(groupPosition).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            CharSequence allContent = "";
            SpannableString content;
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = convertView;
            if (inflater != null) {
                if (view == null)
                    view = inflater.inflate(R.layout.device_details_gatt_services_view, parent, false);
                TextView textView = view.findViewById(R.id.device_details_gatt_service);
                if (services.size() > 0 && services.get(groupPosition).get("UUID") != null) {
                    content = new SpannableString(services.get(groupPosition).get("UUID"));
                    content.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.color_service_uuid))), 0, content.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    allContent = TextUtils.concat(content);
                    if (BluetoothGattUUIDs.servicesUUIDs.containsKey(services.get(groupPosition).get("UUID"))) {
                        allContent = TextUtils.concat(allContent, "\n" + BluetoothGattUUIDs.servicesUUIDs.get(services.get(groupPosition).get("UUID")));
                    }
                }
                textView.setText(allContent);
            }
            return view;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            StringBuilder allContent = new StringBuilder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = convertView;
            if (inflater != null) {
                if (view == null)
                    view = inflater.inflate(R.layout.device_details_gatt_characteristics_view, parent, false);
                TextView textView = view.findViewById(R.id.device_details_gatt_characteristics);
                if (characteristics.size() > 0 && !Objects.requireNonNull(characteristics.get(groupPosition).get(childPosition).get("UUID")).isEmpty()) {
                    final String characteristicUUID = characteristics.get(groupPosition).get(childPosition).get("UUID");
                    allContent.append("\t");
                    allContent.append(characteristicUUID);
                    allContent.append("\n");
                    if (BluetoothGattUUIDs.characteristicsUUIDs.containsKey(characteristicUUID)) {
                        allContent.append("\t");
                        allContent.append(BluetoothGattUUIDs.characteristicsUUIDs.get(characteristicUUID));
                        allContent.append("\n");
                    }
                    if (!Objects.requireNonNull(characteristics.get(groupPosition).get(childPosition).get("PROPERTIES")).isEmpty()) {
                        allContent.append("\t\t");
                        allContent.append(getString(R.string.gatt_properties));
                        allContent.append(characteristics.get(groupPosition).get(childPosition).get("PROPERTIES"));
                        allContent.append("\n");
                    }
                    if (!Objects.requireNonNull(characteristics.get(groupPosition).get(childPosition).get("VALUE")).isEmpty()) {
                        allContent.append("\t\t");
                        allContent.append(getString(R.string.gatt_value));
                        allContent.append(characteristics.get(groupPosition).get(childPosition).get("VALUE"));
                        allContent.append("\n");
                    }
                    if (model.DescriptorsList.containsKey(characteristicUUID)) {
                        ArrayList<HashMap<String, String>> arrayListDescriptors = model.DescriptorsList.get(characteristicUUID);
                        if (arrayListDescriptors != null && arrayListDescriptors.size() > 0) {
                            allContent.append("\t\t");
                            allContent.append(getString(R.string.gatt_descriptors));
                            allContent.append("\n");
                            for (HashMap<String, String> descriptor : arrayListDescriptors) {
                                allContent.append("\t\t\t");
                                allContent.append(descriptor.get("UUID"));
                                allContent.append("\n");
                                if (!Objects.requireNonNull(descriptor.get("NAME")).isEmpty()) {
                                    allContent.append("\t\t\t");
                                }
                                allContent.append(descriptor.get("NAME"));
                                allContent.append("\n");
                                if (!Objects.requireNonNull(descriptor.get("VALUE")).isEmpty()) {
                                    allContent.append("\t\t\t\t");
                                }
                                allContent.append(getString(R.string.gatt_value));
                                allContent.append(descriptor.get("VALUE"));
                                allContent.append("\n");
                            }
                        }
                    }
                    textView.setOnClickListener((v) -> {
                        model.selectedServiceNumber = groupPosition;
                        model.selectedCharacteristicNumber = childPosition;
                        model.selectedDescriptorNumber = -1;
                        model.getGattCharacteristic(services.get(groupPosition).get("UUID"), characteristicUUID);
                        model.device_details_services_view = getString(R.string.device_details_services_radio_button_gatt_characteristic);
                        updateViewServices();
                    });
                }
                textView.setText(allContent.toString());
            }
            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new DeviceDetailsFragment.FragmentMenuProvider(), getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        View view = inflater.inflate(R.layout.fragment_device_details, container, false);
        mapViewHome = view.findViewById(R.id.device_details_home_mapView);
        device_details_services_from_sdp_empty = view.findViewById(R.id.device_details_services_from_sdp_empty);
        device_details_services_from_sdp = view.findViewById(R.id.device_details_services_from_sdp);
        device_details_services_scan_record = view.findViewById(R.id.device_details_services_scan_record);
        mapViewHome.onCreate(null);
        setHomeRadioGroup(view);
        device_details_services_view_gatt_services = view.findViewById(R.id.device_details_services_view_gatt_services);
        servicesListAdapter = new ServicesListAdapter(getActivity(), model.ServicesList, model.CharacteristicsList);
        device_details_services_view_gatt_services.setAdapter(servicesListAdapter);
        fab_main = view.findViewById(R.id.device_details_fab_main);
        fab_home = view.findViewById(R.id.device_details_fab_home);
        fab_services = view.findViewById(R.id.device_details_fab_services);
        fab_terminal = view.findViewById(R.id.device_details_fab_terminal);
        fab_main.setOnClickListener(onClickListenerMainFAB);
        fab_home.setOnClickListener(onClickListenerHomeFAB);
        fab_services.setOnClickListener(onClickListenerServicesFAB);
        fab_terminal.setOnClickListener(onClickListenerTerminalFAB);
        switch (model.viewMode) {
            case DeviceDetailsModel.VIEW_HOME:
                setHomeLayout(view);
                break;
            case DeviceDetailsModel.VIEW_SERVICES:
                setServicesLayout(view);
                break;
            case DeviceDetailsModel.VIEW_TERMINAL:
                setTerminalLayout(view);
                break;
        }
        view.findViewById(R.id.device_details_home_header).setOnLongClickListener(editHeader);
        view.findViewById(R.id.device_details_services_header).setOnLongClickListener(editHeader);
        view.findViewById(R.id.device_details_terminal_header).setOnLongClickListener(editHeader);
        device_details_services_view_gatt_log = view.findViewById(R.id.device_details_services_view_gatt_log);
        device_details_services_gatt_log = view.findViewById(R.id.device_details_services_gatt_log);
        devices_details_services_view_gatt_characteristic = view.findViewById(R.id.devices_details_services_view_gatt_characteristic);
        device_details_services_list_commands = view.findViewById(R.id.device_details_services_list_commands);
        device_details_services_list_commands.setOnClickListener(servicesListCommandsListener);
        device_details_services_settings = view.findViewById(R.id.device_details_gatt_terminal_settings);
        device_details_services_settings.setOnClickListener(servicesSettingsListener);
        view.findViewById(R.id.device_details_gatt_characteristics_send_value).setOnClickListener(servicesSendNewCharacteristicValue);
        view.findViewById(R.id.device_details_gatt_characteristics_enable_notify).setOnClickListener(servicesEnableNotifyCharacteristic);
        setServicesRadioGroup(view);
        device_details_terminal_command = view.findViewById(R.id.device_details_terminal_command);
        device_details_terminal_settings = view.findViewById(R.id.device_details_terminal_settings);
        device_details_terminal_list_commands = view.findViewById(R.id.device_details_terminal_list_commands);
        device_details_terminal_command_send = view.findViewById(R.id.device_details_terminal_command_send);
        device_details_terminal_log = view.findViewById(R.id.device_details_terminal_log);
        setTerminalRadioGroup(view);
        device_details_terminal_command_send.setOnClickListener(terminalCommandSendListener);
        device_details_terminal_settings.setOnClickListener(terminalSettingsListener);
        device_details_terminal_list_commands.setOnClickListener(terminalListCommandsListener);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(StartApplication.getInstance());
        model.connectController(this);
        if (mapViewHome != null) mapViewHome.onResume();
        try {
            runFragment = (RunFragment) getActivity();
        } catch (ClassCastException e) {
            runFragment = null;
        }
        switch (model.viewMode) {
            case DeviceDetailsModel.VIEW_HOME:
                updateViewHome();
                break;
            case DeviceDetailsModel.VIEW_SERVICES:
                updateViewServices();
                break;
            case DeviceDetailsModel.VIEW_TERMINAL:
                updateViewTerminal();
                break;
        }
        String server_url = sharedPreferences.getString(SettingsFragment.KEY_RECEIVING_SERVER_REPORT_URL, "");
        String server_username = sharedPreferences.getString(SettingsFragment.KEY_RECEIVING_SERVER_REPORT_USERNAME, "");
        String server_password = sharedPreferences.getString(SettingsFragment.KEY_RECEIVING_SERVER_REPORT_PASSWORD, "");
        String server_protocols = sharedPreferences.getString(SettingsFragment.KEY_RECEIVING_SERVER_REPORT_PROTOCOLS, "");
        switch (server_url.split(":")[0]) {
            case "ws":
                try {
                    WebSocketFactory factory = new WebSocketFactory();
                    wsSocket = factory.createSocket(server_url, 2000);
                    if (wsSocket != null) {
                        if (!server_protocols.isEmpty()) {
                            for (String protocol : server_protocols.split(",")) {
                                wsSocket.addProtocol(protocol.trim());
                            }
                        }
                        if (!server_username.isEmpty() && !server_password.isEmpty()) {
                            wsSocket.setUserInfo(server_username, server_password);
                        }
                        wsSocket.connectAsynchronously();
                    }
                } catch (IOException e) {
                    break;
                }
                break;
            case "wss":
                try {
                    SSLContext context = NaiveSSLContext.getInstance("TLS");
                    WebSocketFactory factory = new WebSocketFactory();
                    factory.setSSLContext(context);
                    factory.setVerifyHostname(false);
                    try {
                        wsSocket = factory.createSocket(server_url, 2000);
                        if (wsSocket != null) {
                            if (!server_protocols.isEmpty()) {
                                for (String protocol : server_protocols.split(",")) {
                                    wsSocket.addProtocol(protocol.trim());
                                }
                            }
                            if (!server_username.isEmpty() && !server_password.isEmpty()) {
                                wsSocket.setUserInfo(server_username, server_password);
                            }
                            wsSocket.connectAsynchronously();
                        }
                    } catch (IOException e) {
                        break;
                    }
                } catch (NoSuchAlgorithmException e) {
                    break;
                }
                break;
        }
    }

    @Override
    public void onPause() {
        model.disconnectController();
        String server_url = sharedPreferences.getString(SettingsFragment.KEY_RECEIVING_SERVER_REPORT_URL, "");
        switch (server_url.split(":")[0]) {
            case "ws":
            case "wss":
                wsSocket.disconnect();
                break;
        }
        if (mapViewHome != null) mapViewHome.onPause();
        super.onPause();
    }

    void setDeviceTerminalLog(String log) {
        SpannableString spannableString = new SpannableString(log);
        spannableString.setSpan(new TypefaceSpan("monospace"), 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        device_details_terminal_log.setText(spannableString);
    }

    @SuppressLint("NonConstantResourceId")
    void setTerminalRadioGroup(View view) {
        RadioGroup radioGroup = view.findViewById(R.id.device_details_terminal_radio_group);
        radio_button_spp = view.findViewById(R.id.radio_button_spp);
        radio_button_dial_up = view.findViewById(R.id.radio_button_dial_up);
        radio_button_my_service = view.findViewById(R.id.radio_button_my_service);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.radio_button_spp:
                    model.terminalServiceUUID = "00001101-0000-1000-8000-00805f9b34fb";
                    updateViewTerminal();
                    break;
                case R.id.radio_button_dial_up:
                    model.terminalServiceUUID = "00001103-0000-1000-8000-00805f9b34fb";
                    updateViewTerminal();
                    break;
                case R.id.radio_button_my_service:
                    model.terminalServiceUUID = sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_MY_UUID, "");
                    updateViewTerminal();
                    break;
                default:
                case -1:
                    break;
            }
        });
    }

    final View.OnClickListener terminalSettingsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            final TerminalDialogPreference terminalDialogPreference = new TerminalDialogPreference(requireActivity());
            builder.setView(terminalDialogPreference.getView(sharedPreferences));
            builder.setTitle(getString(R.string.device_details_terminal_dialog_settings_title));
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                terminalDialogPreference.savePreference(sharedPreferences);
                if (radio_button_my_service.isChecked()) {
                    model.terminalServiceUUID = sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_MY_UUID, "");
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create();
            builder.show();
        }
    };

    final View.OnClickListener terminalListCommandsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(getString(R.string.device_details_terminal_dialog_list_commands));
            int allCommands = 0;
            if (!sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_1, "").isEmpty())
                allCommands++;
            if (!sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_2, "").isEmpty())
                allCommands++;
            if (!sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_3, "").isEmpty())
                allCommands++;
            if (!sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_4, "").isEmpty())
                allCommands++;
            if (!sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_5, "").isEmpty())
                allCommands++;
            final CharSequence[] list_commands = new CharSequence[allCommands + 1];
            allCommands = 0;
            if (!sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_1, "").isEmpty()) {
                list_commands[allCommands] = sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_1, "");
                allCommands++;
            }
            if (!sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_2, "").isEmpty()) {
                list_commands[allCommands] = sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_2, "");
                allCommands++;
            }
            if (!sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_3, "").isEmpty()) {
                list_commands[allCommands] = sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_3, "");
                allCommands++;
            }
            if (!sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_4, "").isEmpty()) {
                list_commands[allCommands] = sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_4, "");
                allCommands++;
            }
            if (!sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_5, "").isEmpty()) {
                list_commands[allCommands] = sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_COMMAND_5, "");
                allCommands++;
            }
            list_commands[allCommands] = getString(R.string.device_details_terminal_clear_log);
            builder.setItems(list_commands, (dialog, which) -> {
                        if (list_commands[which].equals(getString(R.string.device_details_terminal_clear_log))) {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(requireActivity());
                            builder1.setTitle(getString(R.string.device_details_terminal_clear_log));
                            builder1.setMessage(getString(R.string.are_you_sure));
                            builder1.setPositiveButton(android.R.string.ok, (dialog1, which1) -> {
                                switch (model.terminalServiceUUID) {
                                    case "00001101-0000-1000-8000-00805f9b34fb":
                                        model.terminalLogSPP = "";
                                        setDeviceTerminalLog(model.terminalLogSPP);
                                        break;
                                    case "00001103-0000-1000-8000-00805f9b34fb":
                                        model.terminalLogDialUp = "";
                                        setDeviceTerminalLog(model.terminalLogDialUp);
                                        break;
                                    default:
                                        model.terminalLogMyService = "";
                                        setDeviceTerminalLog(model.terminalLogMyService);
                                        break;
                                }
                            });
                            builder1.setNegativeButton(android.R.string.cancel, null);
                            builder1.show();
                        } else {
                            device_details_terminal_command.setText(list_commands[which]);
                            terminalSendCommand();
                        }
                    }
            );
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create();
            builder.show();
        }
    };

    void terminalSendCommand() {
        if (btDriver.isRfCommValid() && model.terminalConnectState == 2) {
            StringBuilder command = new StringBuilder(device_details_terminal_command.getText().toString());
            String displayCommand;
            if (sharedPreferences.getBoolean(SettingsFragment.KEY_TERMINAL_SEND_CR, true))
                command.append("\r");
            if (sharedPreferences.getBoolean(SettingsFragment.KEY_TERMINAL_SEND_LF, false))
                command.append("\n");
            if (command.length() != 0) {
                if (sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_OUTPUT_FORMAT, "Text").equals("Text")) {
                    btDriver.rfCommWrite(command.toString().getBytes());
                    displayCommand = command.toString().replaceAll("\r", "<CR>").replaceAll("\n", "<LF>");
                } else {
                    String commandSend = command.toString().toUpperCase().replaceAll("0X", "").replaceAll(" ", "").replaceAll("\r", "0D").replaceAll("\n", "0A");
                    command = new StringBuilder();
                    if (commandSend.length() != 0 && (commandSend.length() % 2) == 0) {
                        byte[] commandBytes = new byte[commandSend.length() / 2];
                        String code;
                        int byteCount = 0;
                        for (int c = 0; c < commandSend.length(); c = c + 2) {
                            code = commandSend.substring(c, c + 1);
                            switch (code) {
                                case "0":
                                    commandBytes[byteCount] = (byte) 0x00;
                                    break;
                                case "1":
                                    commandBytes[byteCount] = (byte) 0x10;
                                    break;
                                case "2":
                                    commandBytes[byteCount] = (byte) 0x20;
                                    break;
                                case "3":
                                    commandBytes[byteCount] = (byte) 0x30;
                                    break;
                                case "4":
                                    commandBytes[byteCount] = (byte) 0x40;
                                    break;
                                case "5":
                                    commandBytes[byteCount] = (byte) 0x50;
                                    break;
                                case "6":
                                    commandBytes[byteCount] = (byte) 0x60;
                                    break;
                                case "7":
                                    commandBytes[byteCount] = (byte) 0x70;
                                    break;
                                case "8":
                                    commandBytes[byteCount] = (byte) 0x80;
                                    break;
                                case "9":
                                    commandBytes[byteCount] = (byte) 0x90;
                                    break;
                                case "A":
                                    commandBytes[byteCount] = (byte) 0xA0;
                                    break;
                                case "B":
                                    commandBytes[byteCount] = (byte) 0xB0;
                                    break;
                                case "C":
                                    commandBytes[byteCount] = (byte) 0xC0;
                                    break;
                                case "D":
                                    commandBytes[byteCount] = (byte) 0xD0;
                                    break;
                                case "E":
                                    commandBytes[byteCount] = (byte) 0xE0;
                                    break;
                                case "F":
                                    commandBytes[byteCount] = (byte) 0xF0;
                                    break;
                                default:
                                    new SnackBar().ShowLong(getString(R.string.terminal_invalid_hex_format_command));
                                    return;
                            }
                            code = commandSend.substring(c + 1, c + 2);
                            switch (code) {
                                case "0":
                                    commandBytes[byteCount] += (byte) 0x00;
                                    break;
                                case "1":
                                    commandBytes[byteCount] += (byte) 0x01;
                                    break;
                                case "2":
                                    commandBytes[byteCount] += (byte) 0x02;
                                    break;
                                case "3":
                                    commandBytes[byteCount] += (byte) 0x03;
                                    break;
                                case "4":
                                    commandBytes[byteCount] += (byte) 0x04;
                                    break;
                                case "5":
                                    commandBytes[byteCount] += (byte) 0x05;
                                    break;
                                case "6":
                                    commandBytes[byteCount] += (byte) 0x06;
                                    break;
                                case "7":
                                    commandBytes[byteCount] += (byte) 0x07;
                                    break;
                                case "8":
                                    commandBytes[byteCount] += (byte) 0x08;
                                    break;
                                case "9":
                                    commandBytes[byteCount] += (byte) 0x09;
                                    break;
                                case "A":
                                    commandBytes[byteCount] += (byte) 0x0A;
                                    break;
                                case "B":
                                    commandBytes[byteCount] += (byte) 0x0B;
                                    break;
                                case "C":
                                    commandBytes[byteCount] += (byte) 0x0C;
                                    break;
                                case "D":
                                    commandBytes[byteCount] += (byte) 0x0D;
                                    break;
                                case "E":
                                    commandBytes[byteCount] += (byte) 0x0E;
                                    break;
                                case "F":
                                    commandBytes[byteCount] += (byte) 0x0F;
                                    break;
                                default:
                                    new SnackBar().ShowLong(getString(R.string.terminal_invalid_hex_format_command));
                                    return;
                            }
                            byteCount++;
                            command.append(commandSend.substring(c, c + 2));
                            command.append(" ");
                        }
                        btDriver.rfCommWrite(commandBytes);
                        displayCommand = command.toString().trim();
                    } else {
                        new SnackBar().ShowLong(getString(R.string.terminal_invalid_hex_format_command));
                        return;
                    }
                }
                switch (model.terminalServiceUUID) {
                    case "00001101-0000-1000-8000-00805f9b34fb":
                        model.terminalLogSPP = "out> " + displayCommand + "\n" + model.terminalLogSPP;
                        setDeviceTerminalLog(model.terminalLogSPP);
                        break;
                    case "00001103-0000-1000-8000-00805f9b34fb":
                        model.terminalLogDialUp = "out> " + displayCommand + "\n" + model.terminalLogDialUp;
                        setDeviceTerminalLog(model.terminalLogDialUp);
                        break;
                    default:
                        model.terminalLogMyService = "out> " + displayCommand + "\n" + model.terminalLogMyService;
                        setDeviceTerminalLog(model.terminalLogMyService);
                        break;
                }
            }
        } else {
            switch (model.terminalServiceUUID) {
                case "00001101-0000-1000-8000-00805f9b34fb":
                    model.terminalLogSPP = "log> " + getString(R.string.terminal_message_no_connection) + "\n" + model.terminalLogSPP;
                    setDeviceTerminalLog(model.terminalLogSPP);
                    break;
                case "00001103-0000-1000-8000-00805f9b34fb":
                    model.terminalLogDialUp = "log> " + getString(R.string.terminal_message_no_connection) + "\n" + model.terminalLogDialUp;
                    setDeviceTerminalLog(model.terminalLogDialUp);
                    break;
                default:
                    model.terminalLogMyService = "log> " + getString(R.string.terminal_message_no_connection) + "\n" + model.terminalLogMyService;
                    setDeviceTerminalLog(model.terminalLogMyService);
                    break;
            }
        }
    }

    final View.OnClickListener terminalCommandSendListener = v -> terminalSendCommand();

    void updateViewTerminal() {
        updateViewHeader(R.id.device_details_terminal_header);
        switch (model.terminalServiceUUID) {
            case "00001101-0000-1000-8000-00805f9b34fb":
                setDeviceTerminalLog(model.terminalLogSPP);
                break;
            case "00001103-0000-1000-8000-00805f9b34fb":
                setDeviceTerminalLog(model.terminalLogDialUp);
                break;
            default:
                setDeviceTerminalLog(model.terminalLogMyService);
                break;
        }
    }

    final View.OnClickListener servicesSettingsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            final GattTerminalDialogPreference gattTerminalDialogPreference = new GattTerminalDialogPreference(requireActivity());
            builder.setView(gattTerminalDialogPreference.getView(sharedPreferences));
            builder.setTitle(getString(R.string.device_details_gatt_terminal_dialog_settings_title));
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                gattTerminalDialogPreference.savePreference(sharedPreferences);
                updateViewServices();
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create();
            builder.show();
        }
    };

    final View.OnClickListener servicesListCommandsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CharSequence[] list_commands = new CharSequence[5];
            final AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(getString(R.string.device_details_terminal_dialog_list_commands));
            list_commands[0] = getString(R.string.device_details_services_read_remote_rssi);
            list_commands[1] = getString(R.string.device_details_services_request_new_mtu_value);
            list_commands[2] = getString(R.string.device_details_services_request_connection_priority);
            list_commands[3] = getString(R.string.device_details_services_show_gatt_log);
            list_commands[4] = getString(R.string.device_details_services_clear_gatt_log);
            builder.setItems(list_commands, (dialog, which) -> {
                switch (which) {
                    case 0:
                        model.readRemoteRssi();
                        break;
                    case 1:
                        AlertDialog.Builder builderMTU = new AlertDialog.Builder(requireActivity());
                        builderMTU.setTitle(getString(R.string.device_details_services_request_new_mtu_value));
                        View viewMTU = View.inflate(getActivity(), R.layout.request_new_mtu_value, null);
                        final EditText editText = viewMTU.findViewById(R.id.new_mtu_value);
                        builderMTU.setView(viewMTU);
                        builderMTU.setPositiveButton(android.R.string.ok, (dialog1, which1) -> {
                            try {
                                model.requestNewMtu(Integer.parseInt(editText.getText().toString()));
                            } catch (NumberFormatException ex) {
                                new SnackBar().ShowLong(getString(R.string.invalid_mtu_value));
                            }
                        });
                        builderMTU.setNegativeButton(android.R.string.cancel, null);
                        builderMTU.show();
                        break;
                    case 2:
                        CharSequence[] list_priorities = new CharSequence[3];
                        list_priorities[0] = getString(R.string.device_details_services_balanced_connection);
                        list_priorities[1] = getString(R.string.device_details_services_high_priority);
                        list_priorities[2] = getString(R.string.device_details_services_low_power);
                        AlertDialog.Builder builderPriority = new AlertDialog.Builder(requireActivity());
                        builderPriority.setTitle(getString(R.string.device_details_services_request_connection_priority));
                        builderPriority.setItems(list_priorities, (dialog12, which12) -> model.requestConnectionPriority(which12));
                        builderPriority.setNegativeButton(android.R.string.cancel, null);
                        builderPriority.create();
                        builderPriority.show();
                        break;
                    case 3:
                        if (model.gattConnectionLog.isEmpty()) {
                            new SnackBar().ShowShort(getString(R.string.device_details_services_log_connection_is_empty));
                        } else {
                            model.device_details_services_view = getString(R.string.device_details_services_show_gatt_log);
                            updateViewServices();
                        }
                        break;
                    case 4:
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(requireActivity());
                        builder1.setTitle(getString(R.string.device_details_services_clear_gatt_log));
                        builder1.setMessage(getString(R.string.are_you_sure));
                        builder1.setPositiveButton(android.R.string.ok, (dialog13, which13) -> {
                            model.gattConnectionLog = "";
                            device_details_services_gatt_log.setText(model.gattConnectionLog);
                        });
                        builder1.setNegativeButton(android.R.string.cancel, null);
                        builder1.show();
                        break;
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create();
            builder.show();
        }
    };

    @SuppressLint("NonConstantResourceId")
    void setServicesRadioGroup(View view) {
        RadioGroup RadioGroup = view.findViewById(R.id.device_details_services_radio_group);
        if (model.device_details_services_view.equals(getString(R.string.device_details_services_radio_button_gatt_services))) {
            ((RadioButton) view.findViewById(R.id.radio_button_gatt_services)).setChecked(true);
        }
        if (model.device_details_services_view.equals(getString(R.string.device_details_services_radio_button_gatt_characteristic))) {
            ((RadioButton) view.findViewById(R.id.radio_button_gatt_characteristic)).setChecked(true);
        }
        RadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.radio_button_gatt_services:
                    model.device_details_services_view = getString(R.string.device_details_services_radio_button_gatt_services);
                    updateViewServices();
                    break;
                case R.id.radio_button_gatt_characteristic:
                    model.device_details_services_view = getString(R.string.device_details_services_radio_button_gatt_characteristic);
                    updateViewServices();
                    break;
                default:
                case -1:
                    ((RadioButton) requireActivity().findViewById(R.id.radio_button_gatt_services)).setChecked(false);
                    ((RadioButton) requireActivity().findViewById(R.id.radio_button_gatt_characteristic)).setChecked(false);
                    break;
            }
        });
    }

    final View.OnClickListener servicesSendNewCharacteristicValue = v -> {
        String serviceUUID = model.ServicesList.get(model.selectedServiceNumber).get("UUID");
        String characteristicUUID = model.CharacteristicsList.get(model.selectedServiceNumber).get(model.selectedCharacteristicNumber).get("UUID");
        String value = ((EditText) requireActivity().findViewById(R.id.device_details_gatt_characteristics_new_value)).getText().toString();
        if (((RadioButton) requireActivity().findViewById(R.id.device_details_gatt_characteristics_type_hex)).isChecked()) {
            value = value.toUpperCase().replaceAll("0X", "").replaceAll(" ", "");
            if (value.length() != 0 && (value.length() % 2) == 0) {
                byte[] valueBytes = new byte[value.length() / 2];
                String code;
                int byteCount = 0;
                for (int c = 0; c < value.length(); c = c + 2) {
                    code = value.substring(c, c + 1);
                    switch (code) {
                        case "0":
                            valueBytes[byteCount] = (byte) 0x00;
                            break;
                        case "1":
                            valueBytes[byteCount] = (byte) 0x10;
                            break;
                        case "2":
                            valueBytes[byteCount] = (byte) 0x20;
                            break;
                        case "3":
                            valueBytes[byteCount] = (byte) 0x30;
                            break;
                        case "4":
                            valueBytes[byteCount] = (byte) 0x40;
                            break;
                        case "5":
                            valueBytes[byteCount] = (byte) 0x50;
                            break;
                        case "6":
                            valueBytes[byteCount] = (byte) 0x60;
                            break;
                        case "7":
                            valueBytes[byteCount] = (byte) 0x70;
                            break;
                        case "8":
                            valueBytes[byteCount] = (byte) 0x80;
                            break;
                        case "9":
                            valueBytes[byteCount] = (byte) 0x90;
                            break;
                        case "A":
                            valueBytes[byteCount] = (byte) 0xA0;
                            break;
                        case "B":
                            valueBytes[byteCount] = (byte) 0xB0;
                            break;
                        case "C":
                            valueBytes[byteCount] = (byte) 0xC0;
                            break;
                        case "D":
                            valueBytes[byteCount] = (byte) 0xD0;
                            break;
                        case "E":
                            valueBytes[byteCount] = (byte) 0xE0;
                            break;
                        case "F":
                            valueBytes[byteCount] = (byte) 0xF0;
                            break;
                        default:
                            new SnackBar().ShowLong(getString(R.string.services_invalid_hex_format_value));
                            return;
                    }
                    code = value.substring(c + 1, c + 2);
                    switch (code) {
                        case "0":
                            valueBytes[byteCount] += (byte) 0x00;
                            break;
                        case "1":
                            valueBytes[byteCount] += (byte) 0x01;
                            break;
                        case "2":
                            valueBytes[byteCount] += (byte) 0x02;
                            break;
                        case "3":
                            valueBytes[byteCount] += (byte) 0x03;
                            break;
                        case "4":
                            valueBytes[byteCount] += (byte) 0x04;
                            break;
                        case "5":
                            valueBytes[byteCount] += (byte) 0x05;
                            break;
                        case "6":
                            valueBytes[byteCount] += (byte) 0x06;
                            break;
                        case "7":
                            valueBytes[byteCount] += (byte) 0x07;
                            break;
                        case "8":
                            valueBytes[byteCount] += (byte) 0x08;
                            break;
                        case "9":
                            valueBytes[byteCount] += (byte) 0x09;
                            break;
                        case "A":
                            valueBytes[byteCount] += (byte) 0x0A;
                            break;
                        case "B":
                            valueBytes[byteCount] += (byte) 0x0B;
                            break;
                        case "C":
                            valueBytes[byteCount] += (byte) 0x0C;
                            break;
                        case "D":
                            valueBytes[byteCount] += (byte) 0x0D;
                            break;
                        case "E":
                            valueBytes[byteCount] += (byte) 0x0E;
                            break;
                        case "F":
                            valueBytes[byteCount] += (byte) 0x0F;
                            break;
                        default:
                            new SnackBar().ShowLong(getString(R.string.services_invalid_hex_format_value));
                            return;
                    }
                    byteCount++;
                }
                model.setGattCharacteristic(serviceUUID, characteristicUUID, valueBytes);
            } else {
                new SnackBar().ShowLong(getString(R.string.services_invalid_hex_format_value));
                return;
            }
        }
        if (((RadioButton) requireActivity().findViewById(R.id.device_details_gatt_characteristics_type_text)).isChecked()) {
            model.setGattCharacteristic(serviceUUID, characteristicUUID, value.getBytes());
        }
    };

    final View.OnClickListener servicesSendNewDescriptorValue = v -> {
        int selectedDescriptor = ((Spinner) requireActivity().findViewById(R.id.device_details_gatt_descriptor_spinner)).getSelectedItemPosition();
        String serviceUUID = model.ServicesList.get(model.selectedServiceNumber).get("UUID");
        String characteristicUUID = model.CharacteristicsList.get(model.selectedServiceNumber).get(model.selectedCharacteristicNumber).get("UUID");
        String descriptorUUID = Objects.requireNonNull(model.DescriptorsList.get(characteristicUUID)).get(selectedDescriptor).get("UUID");
        String value = ((EditText) requireActivity().findViewById(R.id.device_details_gatt_descriptor_new_value)).getText().toString();
        if (((RadioButton) requireActivity().findViewById(R.id.device_details_gatt_descriptor_type_hex)).isChecked()) {
            value = value.toUpperCase().replaceAll("0X", "").replaceAll(" ", "");
            if (value.length() != 0 && (value.length() % 2) == 0) {
                byte[] valueBytes = new byte[value.length() / 2];
                String code;
                int byteCount = 0;
                for (int c = 0; c < value.length(); c = c + 2) {
                    code = value.substring(c, c + 1);
                    switch (code) {
                        case "0":
                            valueBytes[byteCount] = (byte) 0x00;
                            break;
                        case "1":
                            valueBytes[byteCount] = (byte) 0x10;
                            break;
                        case "2":
                            valueBytes[byteCount] = (byte) 0x20;
                            break;
                        case "3":
                            valueBytes[byteCount] = (byte) 0x30;
                            break;
                        case "4":
                            valueBytes[byteCount] = (byte) 0x40;
                            break;
                        case "5":
                            valueBytes[byteCount] = (byte) 0x50;
                            break;
                        case "6":
                            valueBytes[byteCount] = (byte) 0x60;
                            break;
                        case "7":
                            valueBytes[byteCount] = (byte) 0x70;
                            break;
                        case "8":
                            valueBytes[byteCount] = (byte) 0x80;
                            break;
                        case "9":
                            valueBytes[byteCount] = (byte) 0x90;
                            break;
                        case "A":
                            valueBytes[byteCount] = (byte) 0xA0;
                            break;
                        case "B":
                            valueBytes[byteCount] = (byte) 0xB0;
                            break;
                        case "C":
                            valueBytes[byteCount] = (byte) 0xC0;
                            break;
                        case "D":
                            valueBytes[byteCount] = (byte) 0xD0;
                            break;
                        case "E":
                            valueBytes[byteCount] = (byte) 0xE0;
                            break;
                        case "F":
                            valueBytes[byteCount] = (byte) 0xF0;
                            break;
                        default:
                            new SnackBar().ShowLong(getString(R.string.services_invalid_hex_format_value));
                            return;
                    }
                    code = value.substring(c + 1, c + 2);
                    switch (code) {
                        case "0":
                            valueBytes[byteCount] += (byte) 0x00;
                            break;
                        case "1":
                            valueBytes[byteCount] += (byte) 0x01;
                            break;
                        case "2":
                            valueBytes[byteCount] += (byte) 0x02;
                            break;
                        case "3":
                            valueBytes[byteCount] += (byte) 0x03;
                            break;
                        case "4":
                            valueBytes[byteCount] += (byte) 0x04;
                            break;
                        case "5":
                            valueBytes[byteCount] += (byte) 0x05;
                            break;
                        case "6":
                            valueBytes[byteCount] += (byte) 0x06;
                            break;
                        case "7":
                            valueBytes[byteCount] += (byte) 0x07;
                            break;
                        case "8":
                            valueBytes[byteCount] += (byte) 0x08;
                            break;
                        case "9":
                            valueBytes[byteCount] += (byte) 0x09;
                            break;
                        case "A":
                            valueBytes[byteCount] += (byte) 0x0A;
                            break;
                        case "B":
                            valueBytes[byteCount] += (byte) 0x0B;
                            break;
                        case "C":
                            valueBytes[byteCount] += (byte) 0x0C;
                            break;
                        case "D":
                            valueBytes[byteCount] += (byte) 0x0D;
                            break;
                        case "E":
                            valueBytes[byteCount] += (byte) 0x0E;
                            break;
                        case "F":
                            valueBytes[byteCount] += (byte) 0x0F;
                            break;
                        default:
                            new SnackBar().ShowLong(getString(R.string.services_invalid_hex_format_value));
                            return;
                    }
                    byteCount++;
                }
                model.setGattDescriptor(serviceUUID, characteristicUUID, descriptorUUID, valueBytes);
            } else {
                new SnackBar().ShowLong(getString(R.string.services_invalid_hex_format_value));
                return;
            }
        }
        if (((RadioButton) requireActivity().findViewById(R.id.device_details_gatt_descriptor_type_text)).isChecked()) {
            model.setGattDescriptor(serviceUUID, characteristicUUID, descriptorUUID, value.getBytes());
        }
    };

    final View.OnClickListener servicesEnableNotifyCharacteristic = v -> {
        String serviceUUID = model.ServicesList.get(model.selectedServiceNumber).get("UUID");
        String characteristicUUID = model.CharacteristicsList.get(model.selectedServiceNumber).get(model.selectedCharacteristicNumber).get("UUID");
        model.setCharacteristicNotification(serviceUUID, characteristicUUID, ((CheckBox) v.findViewById(R.id.device_details_gatt_characteristics_enable_notify)).isChecked());
    };

    void updateViewServices() {
        updateViewHeader(R.id.device_details_services_header);
        if (model.device_details_services_view.equals(getString(R.string.device_details_services_radio_button_gatt_services))) {
            ((RadioButton) requireActivity().findViewById(R.id.radio_button_gatt_services)).setChecked(true);
            devices_details_services_view_gatt_characteristic.setVisibility(View.GONE);
            device_details_services_view_gatt_log.setVisibility(View.GONE);
            device_details_services_view_gatt_services.setVisibility(View.VISIBLE);
            servicesListAdapter.notifyDataSetChanged();
            if (sharedPreferences.getBoolean(SettingsFragment.KEY_DEVICES_DETAILS_SERVICES_LIST_STATE, true)) {
                for (int g = 0; g < servicesListAdapter.getGroupCount(); g++)
                    device_details_services_view_gatt_services.expandGroup(g);
            }
        }
        if (model.device_details_services_view.equals(getString(R.string.device_details_services_radio_button_gatt_characteristic))) {
            ((RadioButton) requireActivity().findViewById(R.id.radio_button_gatt_characteristic)).setChecked(true);
            device_details_services_view_gatt_log.setVisibility(View.GONE);
            device_details_services_view_gatt_services.setVisibility(View.GONE);
            if (model.selectedServiceNumber != -1 && model.selectedCharacteristicNumber != -1) {
                devices_details_services_view_gatt_characteristic.setVisibility(View.VISIBLE);
                TextView info = requireActivity().findViewById(R.id.device_details_gatt_characteristics_info);
                CharSequence infoContent = "";
                SpannableString blackString;
                String characteristicUUID = model.CharacteristicsList.get(model.selectedServiceNumber).get(model.selectedCharacteristicNumber).get("UUID");
                blackString = new SpannableString(characteristicUUID + "\n");
                blackString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, blackString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                infoContent = TextUtils.concat(infoContent, blackString);
                if (BluetoothGattUUIDs.characteristicsUUIDs.containsKey(characteristicUUID)) {
                    infoContent = TextUtils.concat(infoContent, BluetoothGattUUIDs.characteristicsUUIDs.get(characteristicUUID) + "\n");
                }
                if (!Objects.requireNonNull(model.CharacteristicsList.get(model.selectedServiceNumber).get(model.selectedCharacteristicNumber).get("PROPERTIES")).isEmpty()) {
                    blackString = new SpannableString(model.CharacteristicsList.get(model.selectedServiceNumber).get(model.selectedCharacteristicNumber).get("PROPERTIES") + "\n");
                    blackString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, blackString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    infoContent = TextUtils.concat(infoContent, "\n\t" + getString(R.string.gatt_properties));
                    infoContent = TextUtils.concat(infoContent, blackString);
                }
                if (!Objects.requireNonNull(model.CharacteristicsList.get(model.selectedServiceNumber).get(model.selectedCharacteristicNumber).get("VALUE")).isEmpty()) {
                    blackString = new SpannableString(model.CharacteristicsList.get(model.selectedServiceNumber).get(model.selectedCharacteristicNumber).get("VALUE") + "\n");
                    blackString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, blackString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    infoContent = TextUtils.concat(infoContent, "\n\t" + getString(R.string.gatt_value));
                    infoContent = TextUtils.concat(infoContent, blackString);
                } else {
                    infoContent = TextUtils.concat(infoContent, "\n\n\t");
                }
                info.setText(infoContent);
                ((CheckBox) requireActivity().findViewById(R.id.device_details_gatt_characteristics_enable_notify)).setChecked(Objects.equals(model.CharacteristicsList.get(model.selectedServiceNumber).get(model.selectedCharacteristicNumber).get("NOTIFY"), "enable"));
                ((EditText) requireActivity().findViewById(R.id.device_details_gatt_characteristics_new_value)).setText("");
                if (model.DescriptorsList.containsKey(characteristicUUID)) {
                    ArrayList<HashMap<String, String>> arrayListDescriptors = model.DescriptorsList.get(characteristicUUID);
                    if (arrayListDescriptors != null && arrayListDescriptors.size() > 0) {
                        (requireActivity().findViewById(R.id.device_details_gatt_descriptors)).setVisibility(View.VISIBLE);
                        Spinner spinner = requireActivity().findViewById(R.id.device_details_gatt_descriptor_spinner);
                        List<String> descriptorList = new ArrayList<>();
                        for (HashMap<String, String> descriptor : arrayListDescriptors)
                            descriptorList.add(descriptor.get("UUID"));
                        ArrayAdapter<String> descriptorAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_item, descriptorList);
                        spinner.setAdapter(descriptorAdapter);
                        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                CharSequence content = "";
                                SpannableString blackString;
                                model.selectedDescriptorNumber = position;
                                String characteristicUUID = model.CharacteristicsList.get(model.selectedServiceNumber).get(model.selectedCharacteristicNumber).get("UUID");
                                if (!Objects.requireNonNull(Objects.requireNonNull(model.DescriptorsList.get(characteristicUUID)).get(position).get("NAME")).isEmpty()) {
                                    content = TextUtils.concat(content, "\t" + Objects.requireNonNull(model.DescriptorsList.get(characteristicUUID)).get(position).get("NAME") + "\n\n");
                                }
                                if (!Objects.requireNonNull(Objects.requireNonNull(model.DescriptorsList.get(characteristicUUID)).get(position).get("VALUE")).isEmpty()) {
                                    blackString = new SpannableString(Objects.requireNonNull(model.DescriptorsList.get(characteristicUUID)).get(position).get("VALUE"));
                                    blackString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, blackString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    content = TextUtils.concat(content, "\t" + getString(R.string.gatt_value));
                                    content = TextUtils.concat(content, blackString);
                                    content = TextUtils.concat(content, "\n");
                                }
                                ((TextView) requireActivity().findViewById(R.id.device_details_gatt_descriptor_info)).setText(content);
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                        if (model.selectedDescriptorNumber != -1)
                            spinner.setSelection(model.selectedDescriptorNumber);
                        ((EditText) requireActivity().findViewById(R.id.device_details_gatt_descriptor_new_value)).setText("");
                        (requireActivity().findViewById(R.id.device_details_gatt_descriptor_send_value)).setOnClickListener(servicesSendNewDescriptorValue);
                    }
                } else
                    (requireActivity().findViewById(R.id.device_details_gatt_descriptors)).setVisibility(View.GONE);
            }
        }
        if (model.device_details_services_view.equals(getString(R.string.device_details_services_show_gatt_log))) {
            ((RadioButton) requireActivity().findViewById(R.id.radio_button_gatt_no_select)).setChecked(true);
            device_details_services_view_gatt_services.setVisibility(View.GONE);
            devices_details_services_view_gatt_characteristic.setVisibility(View.GONE);
            device_details_services_view_gatt_log.setVisibility(View.VISIBLE);
            if (!model.gattConnectionLog.isEmpty()) {
                SpannableString gattConnectionLogTitle = new SpannableString(getString(R.string.device_details_services_log_connection_title) + "\n");
                gattConnectionLogTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_details_services_log_connection_title).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_details_services_gatt_log.setText(TextUtils.concat(gattConnectionLogTitle, model.gattConnectionLog));
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    void setHomeRadioGroup(View view) {
        RadioGroup radioGroup = view.findViewById(R.id.device_details_home_radio_group);
        if (model.device_details_home_radio_button.equals(getString(R.string.device_details_home_radio_button_map))) {
            ((RadioButton) view.findViewById(R.id.radio_button_map)).setChecked(true);
        }
        if (model.device_details_home_radio_button.equals(getString(R.string.device_details_home_radio_button_sdp))) {
            ((RadioButton) view.findViewById(R.id.radio_button_sdp)).setChecked(true);
        }
        if (model.device_details_home_radio_button.equals(getString(R.string.device_details_home_radio_button_scan_record))) {
            ((RadioButton) view.findViewById(R.id.radio_button_scan_record)).setChecked(true);
        }
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.radio_button_map:
                    model.device_details_home_radio_button = getString(R.string.device_details_home_radio_button_map);
                    updateViewHome();
                    break;
                case R.id.radio_button_sdp:
                    model.device_details_home_radio_button = getString(R.string.device_details_home_radio_button_sdp);
                    updateViewHome();
                    break;
                case R.id.radio_button_scan_record:
                    model.device_details_home_radio_button = getString(R.string.device_details_home_radio_button_scan_record);
                    updateViewHome();
                    break;
                default:
                case -1:
                    break;
            }
        });
    }

    void updateViewHome() {
        updateViewHeader(R.id.device_details_home_header);
        TextView header = requireActivity().findViewById(R.id.device_details_home_header);
        CharSequence deviceHeader = TextUtils.concat(header.getText(), "\n");
        SpannableString device_parameter;
        if (!model.device_class.equals("UNKNOWN")) {
            device_parameter = new SpannableString(getString(R.string.device_class) + " " + model.device_class + "\n");
            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_class).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_class).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            deviceHeader = TextUtils.concat(deviceHeader, device_parameter);
        }
        if (!model.device_type.equals("UNKNOWN")) {
            device_parameter = new SpannableString(getString(R.string.device_type) + " " + model.device_type + "\n");
            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_type).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_type).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            deviceHeader = TextUtils.concat(deviceHeader, device_parameter);
        }
        header.setText(deviceHeader);
        if (!model.device_manufacturer.equals(getString(R.string.unknown)) && !model.device_manufacturer.isEmpty()) {
            device_parameter = new SpannableString(getString(R.string.device_manufacturer) + " " + model.device_manufacturer + "\n");
            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_manufacturer).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_manufacturer).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            deviceHeader = TextUtils.concat(deviceHeader, device_parameter);
        }
        String device_latitude;
        if (model.device_latitude == MathModule.notLatitude)
            device_latitude = getString(R.string.undefined);
        else device_latitude = String.valueOf(model.device_latitude);
        device_parameter = new SpannableString(getString(R.string.device_latitude) + " " + device_latitude + "\n");
        device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_latitude).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_latitude).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        deviceHeader = TextUtils.concat(deviceHeader, device_parameter);
        String device_longitude;
        if (model.device_longitude == MathModule.notLongitude)
            device_longitude = getString(R.string.undefined);
        else device_longitude = String.valueOf(model.device_longitude);
        device_parameter = new SpannableString(getString(R.string.device_longitude) + " " + device_longitude + "\n");
        device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_longitude).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_longitude).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        deviceHeader = TextUtils.concat(deviceHeader, device_parameter);
        header.setText(deviceHeader);
        if (model.device_details_home_radio_button.equals(getString(R.string.device_details_home_radio_button_map))) {
            device_details_services_from_sdp.setVisibility(View.GONE);
            device_details_services_from_sdp_empty.setVisibility(View.GONE);
            device_details_services_scan_record.setVisibility(View.GONE);
            mapViewHome.setVisibility(View.VISIBLE);
            if (map == null) {
                mapViewHome.getMapAsync(this);
            } else {
                LatLng center_point;
                if (!(model.device_latitude == MathModule.notLatitude || model.device_longitude == MathModule.notLongitude)) {
                    center_point = new LatLng(model.device_latitude, model.device_longitude);
                } else {
                    center_point = new LatLng(0.0, 0.0);
                }
                map.moveCamera(CameraUpdateFactory.newLatLng(center_point));
                marker.setPosition(center_point);
            }
        }
        if (model.device_details_home_radio_button.equals(getString(R.string.device_details_home_radio_button_scan_record))) {
            device_details_services_from_sdp.setVisibility(View.GONE);
            device_details_services_from_sdp_empty.setVisibility(View.GONE);
            mapViewHome.setVisibility(View.GONE);
            device_details_services_scan_record.setVisibility(View.VISIBLE);
            if (!model.device_scan_record.trim().isEmpty()) {
                CharSequence scan_record_message = "";
                SpannableString scan_record_line;
                HashMap<String, String> scanRecord = BluetoothScanRecord.decode(model.device_scan_record);
                for (int i = 0; i < BluetoothScanRecord.dataTypeValueName.size(); i++) {
                    String dataType = BluetoothScanRecord.dataTypeValueName.valueAt(i);
                    if (scanRecord.containsKey(dataType)) {
                        scan_record_line = new SpannableString(dataType + ": " + scanRecord.get(dataType) + "\n");
                        scan_record_line.setSpan(new StyleSpan(Typeface.BOLD), 0, dataType.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        scan_record_line.setSpan(new TypefaceSpan("monospace"), dataType.length(), scan_record_line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        scan_record_message = TextUtils.concat(scan_record_message, scan_record_line);
                    }
                }
                StringBuilder scanRecordString = new StringBuilder("\n");
                int charCounter = 0;
                for (int b = 0; b < model.device_scan_record.length(); ++b) {
                    scanRecordString.append(model.device_scan_record.charAt(b));
                    if (!(b % 2 == 0)) scanRecordString.append(" ");
                    if (++charCounter == 20) {
                        charCounter = 0;
                        scanRecordString.append("\n");
                    }
                }
                scan_record_line = new SpannableString(getString(R.string.device_scan_record) + scanRecordString + "\n");
                scan_record_line.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_scan_record).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                scan_record_line.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_scan_record).length() + 1, scan_record_line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                scan_record_message = TextUtils.concat(scan_record_message, scan_record_line);
                device_details_services_scan_record.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                device_details_services_scan_record.setPadding(0, 5, 0, 0);
                device_details_services_scan_record.setText(scan_record_message);
            } else {
                device_details_services_scan_record.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                device_details_services_scan_record.setPadding(0, 5, 0, 0);
                device_details_services_scan_record.setText(getString(R.string.no_data));
            }
        }
        if (model.device_details_home_radio_button.equals(getString(R.string.device_details_home_radio_button_sdp))) {
            mapViewHome.setVisibility(View.GONE);
            device_details_services_scan_record.setVisibility(View.GONE);
            if (!model.device_service_uuids.isEmpty()) {
                device_details_services_from_sdp_empty.setVisibility(View.GONE);
                device_details_services_from_sdp.setVisibility(View.VISIBLE);
                List<HashMap<String, String>> serviceUUIDsList = new ArrayList<>();
                String[] serviceUUIDs = model.device_service_uuids.trim().split(";");
                String serviceItem;
                for (String UUID : serviceUUIDs) {
                    if (!UUID.isEmpty()) {
                        serviceItem = "\n" + UUID + "\n";
                        if (BluetoothSDP.servicesUUIDs.containsKey(UUID)) {
                            serviceItem += "\t" + BluetoothSDP.servicesUUIDs.get(UUID) + "\n";
                        }
                        HashMap<String, String> serviceItemHashMap = new HashMap<>();
                        serviceItemHashMap.put("item", serviceItem);
                        serviceUUIDsList.add(serviceItemHashMap);
                    }
                }
                ListAdapter serviceUUIDsListAdapter = new SimpleAdapter(getActivity(), serviceUUIDsList, R.layout.device_details_sdp_services_view, new String[]{"item"}, new int[]{R.id.device_details_sdp_service});
                device_details_services_from_sdp.setAdapter(serviceUUIDsListAdapter);
                device_details_services_from_sdp.setOnItemClickListener((parent, view, position, id) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    String my_uuid = model.device_service_uuids.split(";")[position].trim();
                    if (!my_uuid.equals("00001101-0000-1000-8000-00805f9b34fb") && !my_uuid.equals("00001103-0000-1000-8000-00805f9b34fb") && !my_uuid.isEmpty()) {
                        editor.putString(KEY_TERMINAL_MY_UUID, my_uuid);
                        editor.apply();
                        new SnackBar().ShowLong(getString(R.string.save_as_my_service));
                    }
                });
            } else {
                device_details_services_from_sdp_empty.setVisibility(View.VISIBLE);
                device_details_services_from_sdp.setVisibility(View.GONE);
                device_details_services_from_sdp_empty.setText(getString(R.string.no_data));
                device_details_services_from_sdp_empty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                device_details_services_from_sdp_empty.setPadding(0, 5, 0, 0);
            }
        }
    }

    void updateViewHeader(int idHeader) {
        requireActivity().invalidateOptionsMenu();
        TextView header = requireActivity().findViewById(idHeader);
        CharSequence deviceHeader;
        SpannableString device_parameter;
        device_parameter = new SpannableString(getString(R.string.device_name) + " " + model.device_name + "\n");
        device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 1, getString(R.string.device_name).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        device_parameter.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.color_device_name))), getString(R.string.device_name).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_name).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        deviceHeader = TextUtils.concat(device_parameter);
        device_parameter = new SpannableString(getString(R.string.device_mac_address) + " " + model.device_address + "\n");
        device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_mac_address).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_mac_address).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        deviceHeader = TextUtils.concat(deviceHeader, device_parameter);
        String device_paired = btDriver.getBondState(model.device_address) ? getString(R.string.yes) : getString(R.string.no);
        device_parameter = new SpannableString(getString(R.string.device_paired) + " " + device_paired);
        device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_paired).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_paired).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        deviceHeader = TextUtils.concat(deviceHeader, device_parameter);
        header.setText(deviceHeader);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);
        uiSettings.setZoomControlsEnabled(false);
        map.setOnMapClickListener(latLng -> {
            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            marker.setPosition(latLng);
            model.device_latitude = Math.round(latLng.latitude * 1e7) / 1e7;
            model.device_longitude = Math.round(latLng.longitude * 1e7) / 1e7;
            if (dbDatabaseDriver.isDeviceExist(model.device_address)) {
                Cursor cursor;
                cursor = dbDatabaseDriver.rawQuery("SELECT * FROM " + DatabaseDriver.DatabaseContract.Devices.TABLE_NAME + " WHERE " + DatabaseDriver.DatabaseContract.Devices.ADDRESS + " = '" + model.device_address + "';");
                cursor.moveToFirst();
                long deviceId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices._ID));
                cursor.close();
                dbDatabaseDriver.updateDeviceLocation(deviceId, model.device_longitude, model.device_latitude);
            }
            if (model.viewMode == DeviceDetailsModel.VIEW_HOME) {
                updateViewHome();
            }
        });
        LatLng center_point;
        if (model.viewMode == DeviceDetailsModel.VIEW_HOME) {
            if (!(model.device_latitude == MathModule.notLatitude || model.device_longitude == MathModule.notLongitude)) {
                center_point = new LatLng(model.device_latitude, model.device_longitude);
            } else {
                center_point = new LatLng(0.0, 0.0);
            }
            map.moveCamera(CameraUpdateFactory.newLatLng(center_point));
            marker = map.addMarker(new MarkerOptions().position(center_point));
            if (marker != null) {
                marker.setIcon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_myplaces));
            }
        }
    }

    final View.OnClickListener onClickListenerMainFAB = v -> setFabMenu(true);

    final View.OnClickListener onClickListenerHomeFAB = v -> {
        model.stopGetGattServices(false);
        btDriver.rfCommDisconnect(false);
        setFabMenu(false);
        setHomeLayout(requireView());
        model.viewMode = DeviceDetailsModel.VIEW_HOME;
        updateViewHome();
    };

    final View.OnClickListener onClickListenerServicesFAB = v -> {
        btDriver.rfCommDisconnect(false);
        setFabMenu(false);
        setServicesLayout(requireView());
        model.viewMode = DeviceDetailsModel.VIEW_SERVICES;
        updateViewServices();
    };

    final View.OnClickListener onClickListenerTerminalFAB = v -> {
        model.stopGetGattServices(false);
        setFabMenu(false);
        setTerminalLayout(requireView());
        model.viewMode = DeviceDetailsModel.VIEW_TERMINAL;
        updateViewTerminal();
    };

    void setFabMenu(boolean state) {
        if (state) {
            fab_main.setVisibility(View.GONE);
            fab_home.setVisibility(View.VISIBLE);
            fab_services.setVisibility(View.VISIBLE);
            fab_terminal.setVisibility(View.VISIBLE);
        } else {
            fab_main.setVisibility(View.VISIBLE);
            fab_home.setVisibility(View.GONE);
            fab_services.setVisibility(View.GONE);
            fab_terminal.setVisibility(View.GONE);
        }
    }

    void setHomeLayout(View view) {
        LinearLayout linearLayout;
        linearLayout = view.findViewById(R.id.device_details_services);
        linearLayout.setVisibility(View.GONE);
        linearLayout = view.findViewById(R.id.device_details_terminal);
        linearLayout.setVisibility(View.GONE);
        linearLayout = view.findViewById(R.id.device_details_home);
        linearLayout.setVisibility(View.VISIBLE);
        fab_main.setImageResource(R.drawable.ic_fab_home);
    }

    void setServicesLayout(View view) {
        LinearLayout linearLayout;
        linearLayout = view.findViewById(R.id.device_details_home);
        linearLayout.setVisibility(View.GONE);
        linearLayout = view.findViewById(R.id.device_details_terminal);
        linearLayout.setVisibility(View.GONE);
        linearLayout = view.findViewById(R.id.device_details_services);
        linearLayout.setVisibility(View.VISIBLE);
        fab_main.setImageResource(R.drawable.ic_fab_services);
    }

    void setTerminalLayout(View view) {
        LinearLayout linearLayout;
        linearLayout = view.findViewById(R.id.device_details_home);
        linearLayout.setVisibility(View.GONE);
        linearLayout = view.findViewById(R.id.device_details_services);
        linearLayout.setVisibility(View.GONE);
        linearLayout = view.findViewById(R.id.device_details_terminal);
        linearLayout.setVisibility(View.VISIBLE);
        fab_main.setImageResource(R.drawable.ic_fab_terminal);
    }

    void pairDevices(final String mac_address) {
        if (btDriver.getBondState(mac_address)) {
            Snackbar.make(requireActivity().findViewById(R.id.cl), R.string.bluetooth_unpair_device, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.yes, v -> {
                        model.timeoutPairDevice();
                        btDriver.unPairDevice(mac_address);
                    }).show();
        } else {
            model.timeoutPairDevice();
            btDriver.pairDevice(mac_address);
        }
    }

    final View.OnLongClickListener editHeader = v -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View view = View.inflate(getActivity(), R.layout.change_device_name, null);
        final EditText nameInput = view.findViewById(R.id.change_device_name);
        nameInput.setText(model.device_name);
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
            model.device_name = nameInput.getText().toString();
            if (dbDatabaseDriver.isDeviceExist(model.device_address)) {
                dbDatabaseDriver.updateDeviceName(model.deviceId, model.device_name);
            }
            switch (model.viewMode) {
                case DeviceDetailsModel.VIEW_HOME:
                    updateViewHome();
                    break;
                case DeviceDetailsModel.VIEW_SERVICES:
                    updateViewServices();
                    break;
                case DeviceDetailsModel.VIEW_TERMINAL:
                    updateViewTerminal();
                    break;
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setTitle(R.string.change_device_name);
        builder.create();
        builder.show();
        return true;
    };

    void sendReport() {
        sendReportServer(getActivity());
    }


    private void sendReportServer(final Context context) {
        final String report;
        final String bodyContentType;
        String sendingMessage;
        final String server_url = sharedPreferences.getString(SettingsFragment.KEY_RECEIVING_SERVER_REPORT_URL, "");
        final String server_username = sharedPreferences.getString(SettingsFragment.KEY_RECEIVING_SERVER_REPORT_USERNAME, "");
        final String server_password = sharedPreferences.getString(SettingsFragment.KEY_RECEIVING_SERVER_REPORT_PASSWORD, "");
        final String errorTimeout = server_url + ": " + getString(R.string.search_send_server_error_timeout);
        final String errorConnection = server_url + ": " + getString(R.string.search_send_server_error_connection);
        final String errorSSL = server_url + ": " + getString(R.string.search_send_server_error_ssl);
        final String errorAuth = server_url + ": " + getString(R.string.search_send_server_error_authentication);
        final String errorServer = server_url + ": " + getString(R.string.search_send_server_error);
        final String successfullySent;
        final String notSent;

        report = jsonReportFormat();
        bodyContentType = "application/json; charset=utf-8";
        sendingMessage = getString(R.string.search_send_json_report_server) + " " + server_url;
        successfullySent = getString(R.string.search_successfully_send_json_report_server) + " " + server_url;
        notSent = getString(R.string.search_unsuccessfully_send_json_report_server) + " " + server_url;

        switch (server_url.split(":")[0]) {
            case "ws":
            case "wss":
                if (wsSocket != null) {
                    new SnackBar().ShowIndefinite(sendingMessage);
                    if (report != null && !report.isEmpty()) {
                        wsSocket.sendBinary(report.getBytes(StandardCharsets.UTF_8));
                    }
                    if (wsSocket.getState() != WebSocketState.OPEN) {
                        new SnackBar().ShowLong(notSent);
                    } else {
                        new SnackBar().ShowLong(successfullySent);
                    }
                } else {
                    new SnackBar().ShowLong(notSent);
                }
                break;
            case "https":
                @SuppressLint("CustomX509TrustManager") TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }

                            @SuppressLint("TrustAllX509TrustManager")
                            @Override
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            @SuppressLint("TrustAllX509TrustManager")
                            @Override
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };
                try {
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, trustAllCerts, new SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                    HttpsURLConnection.setDefaultHostnameVerifier((arg0, arg1) -> true);
                } catch (KeyManagementException | NoSuchAlgorithmException e) {
                    new SnackBar().ShowLong(notSent);
                    break;
                }
            case "http":
                if (!server_url.isEmpty()) {
                    RequestQueue queue = Volley.newRequestQueue(context);
                    final StringRequest stringRequest = new StringRequest(Request.Method.POST, server_url,
                            new Response.Listener<>() {
                                String responseMessage = "";

                                @Override
                                public void onResponse(String response) {
                                    if (Integer.parseInt(response) == HttpURLConnection.HTTP_OK) {
                                        responseMessage = successfullySent;
                                    } else {
                                        responseMessage = notSent;
                                    }
                                    new SnackBar().ShowLong(responseMessage);
                                }
                            },
                            new Response.ErrorListener() {
                                String errorMessage = "";

                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    if (error.toString().contains("Timeout"))
                                        errorMessage = errorTimeout;
                                    else if (error.toString().contains("NoConnection")) {
                                        if (error.toString().contains("SSLHandshakeException")) {
                                            errorMessage = errorSSL;
                                        } else {
                                            errorMessage = errorConnection;
                                        }
                                    } else if (error.toString().contains("AuthFailure")) {
                                        errorMessage = errorAuth;
                                    } else errorMessage = errorServer;
                                    new SnackBar().ShowLong(errorMessage);
                                }
                            }) {
                        int statusCode;

                        @Override
                        public String getBodyContentType() {
                            return bodyContentType;
                        }

                        @Override
                        public byte[] getBody() {
                            return report == null ? null : report.getBytes(StandardCharsets.UTF_8);
                        }

                        @Override
                        protected Response<String> parseNetworkResponse(NetworkResponse response) {
                            statusCode = response.statusCode;
                            return super.parseNetworkResponse(response);
                        }

                        @Override
                        protected void deliverResponse(String response) {
                            super.deliverResponse(String.valueOf(statusCode));
                        }

                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            if (!server_username.isEmpty() && !server_password.isEmpty()) {
                                HashMap<String, String> headers = new HashMap<>();
                                String credentials = server_username + ":" + server_password;
                                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                                headers.put("Authorization", auth);
                                return headers;
                            } else return super.getHeaders();
                        }
                    };
                    new SnackBar().ShowIndefinite(sendingMessage);
                    queue.add(stringRequest);
                }
                break;
        }
    }

    String jsonReportFormat() {
        String characteristicUUID;
        JSONObject report = new JSONObject();
        JSONObject deviceObject = new JSONObject();
        try {
            deviceObject.put("name", model.device_name);
            deviceObject.put("address", model.device_address);
            String device_paired = btDriver.getBondState(model.device_address) ? getString(R.string.yes) : getString(R.string.no);
            deviceObject.put("paired", device_paired);
            if (!model.device_class.equals("UNKNOWN"))
                deviceObject.put("class", model.device_class);
            if (!model.device_type.equals("UNKNOWN")) deviceObject.put("type", model.device_type);
            if (!model.device_manufacturer.isEmpty() && !model.device_manufacturer.equals(getString(R.string.unknown)))
                deviceObject.put("manufacturer", model.device_manufacturer);
            String device_latitude;
            if (model.device_latitude == MathModule.notLatitude)
                device_latitude = getString(R.string.undefined);
            else device_latitude = String.valueOf(model.device_latitude);
            deviceObject.put("latitude", device_latitude);
            String device_longitude;
            if (model.device_longitude == MathModule.notLongitude)
                device_longitude = getString(R.string.undefined);
            else device_longitude = String.valueOf(model.device_longitude);
            deviceObject.put("longitude", device_longitude);
            if (!model.terminalLogSPP.isEmpty()) deviceObject.put("sppLog", model.terminalLogSPP);
            if (!model.terminalLogDialUp.isEmpty())
                deviceObject.put("dialUpLog", model.terminalLogDialUp);
            if (!model.terminalLogMyService.isEmpty())
                deviceObject.put("dialUpLog", model.terminalLogMyService);
            if (!model.device_scan_record.trim().isEmpty()) {
                HashMap<String, String> scanRecord = BluetoothScanRecord.decode(model.device_scan_record);
                for (int i = 0; i < BluetoothScanRecord.dataTypeValueName.size(); i++) {
                    String dataType = BluetoothScanRecord.dataTypeValueName.valueAt(i);
                    if (scanRecord.containsKey(dataType)) {
                        deviceObject.put(dataType, Objects.requireNonNull(scanRecord.get(dataType)).replaceFirst("\n", "").replaceAll("\n", ", "));
                    }
                }
                deviceObject.put("scanRecord", model.device_scan_record);
            }
            if (!model.device_service_uuids.isEmpty()) {
                String[] serviceUUIDs = model.device_service_uuids.split(";");
                JSONArray deviceSdpServices = new JSONArray();
                for (String UUID : serviceUUIDs) {
                    JSONObject deviceSdpService = new JSONObject();
                    deviceSdpService.put("uuid", UUID);
                    if (BluetoothSDP.servicesUUIDs.containsKey(UUID)) {
                        deviceSdpService.put("name", BluetoothSDP.servicesUUIDs.get(UUID));
                    }
                    deviceSdpServices.put(deviceSdpService);
                }
                deviceObject.put("sdpServices", deviceSdpServices);
            }
            if (model.ServicesList.size() > 0) {
                JSONArray deviceGattServices = new JSONArray();
                for (int s = 0; s < model.ServicesList.size(); s++) {
                    JSONObject deviceGattService = new JSONObject();
                    deviceGattService.put("uuid", model.ServicesList.get(s).get("UUID"));
                    if (BluetoothGattUUIDs.servicesUUIDs.containsKey(model.ServicesList.get(s).get("UUID"))) {
                        deviceGattService.put("name", BluetoothGattUUIDs.servicesUUIDs.get(model.ServicesList.get(s).get("UUID")));
                    }
                    if (model.CharacteristicsList.get(s).size() > 0) {
                        JSONArray deviceGattCharacteristics = new JSONArray();
                        for (int c = 0; c < model.CharacteristicsList.get(s).size(); c++) {
                            characteristicUUID = model.CharacteristicsList.get(s).get(c).get("UUID");
                            if (characteristicUUID != null && !characteristicUUID.isEmpty()) {
                                JSONObject deviceGattCharacteristic = new JSONObject();
                                deviceGattCharacteristic.put("uuid", characteristicUUID);
                                if (BluetoothGattUUIDs.characteristicsUUIDs.containsKey(characteristicUUID)) {
                                    deviceGattCharacteristic.put("name", BluetoothGattUUIDs.characteristicsUUIDs.get(characteristicUUID));
                                }
                                if (!Objects.requireNonNull(model.CharacteristicsList.get(s).get(c).get("PROPERTIES")).isEmpty()) {
                                    deviceGattCharacteristic.put("properties", model.CharacteristicsList.get(s).get(c).get("PROPERTIES"));
                                }
                                if (!Objects.requireNonNull(model.CharacteristicsList.get(s).get(c).get("VALUE")).isEmpty()) {
                                    deviceGattCharacteristic.put("value", model.CharacteristicsList.get(s).get(c).get("VALUE"));
                                }
                                if (model.DescriptorsList.containsKey(characteristicUUID)) {
                                    ArrayList<HashMap<String, String>> arrayListDescriptors = model.DescriptorsList.get(characteristicUUID);
                                    if (arrayListDescriptors != null && arrayListDescriptors.size() > 0) {
                                        JSONArray deviceGattDescriptors = new JSONArray();
                                        for (HashMap<String, String> descriptor : arrayListDescriptors) {
                                            JSONObject jsonDescriptor = new JSONObject();
                                            jsonDescriptor.put("uuid", descriptor.get("UUID"));
                                            if (!Objects.requireNonNull(descriptor.get("NAME")).isEmpty())
                                                jsonDescriptor.put("name", descriptor.get("NAME"));
                                            if (!Objects.requireNonNull(descriptor.get("VALUE")).isEmpty())
                                                jsonDescriptor.put("value", descriptor.get("VALUE"));
                                            deviceGattDescriptors.put(jsonDescriptor);
                                        }
                                        deviceGattCharacteristic.put("descriptors", deviceGattDescriptors);
                                    }
                                }
                                deviceGattCharacteristics.put(deviceGattCharacteristic);
                            }
                        }
                        deviceGattService.put("gattCharacteristics", deviceGattCharacteristics);
                    }
                    deviceGattServices.put(deviceGattService);
                }
                deviceObject.put("gattServices", deviceGattServices);
            }
        } catch (JSONException ex) {
            return "";
        }
        try {
            report.put("report", deviceObject);
        } catch (JSONException ex) {
            report = null;
        }
        if (report != null) return report.toString();
        return "";
    }

    private class FragmentMenuProvider implements MenuProvider {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle(getResources().getString(R.string.app_name_short) + " - " + getResources().getString(R.string.action_device_details));
            }
            menuInflater.inflate(R.menu.menu_device_details, menu);
            menu.findItem(R.id.device_details_add_to_dashboard).setVisible(true);
            switch (model.viewMode) {
                case DeviceDetailsModel.VIEW_HOME:
                    menu.findItem(R.id.device_details_action).setActionView(null).setTitle(R.string.device_details_pair).setIcon(R.drawable.ic_bluetooth_pair).setVisible(true);
                    break;
                case DeviceDetailsModel.VIEW_SERVICES:
                    switch (model.gattConnectionState) {
                        case 0:
                            menu.findItem(R.id.device_details_action).setActionView(null).setTitle(R.string.device_details_connect).setIcon(R.drawable.ic_bluetooth_connect).setVisible(true);
                            menu.findItem(R.id.device_details_send_report).setEnabled(true);
                            menu.findItem(R.id.device_details_add_to_dashboard).setEnabled(true);
                            break;
                        case 1:
                            menu.findItem(R.id.device_details_action).setActionView(R.layout.actionbar_indeterminate_progress).setTitle(R.string.device_details_disconnect).setIcon(R.drawable.ic_bluetooth_connect).setVisible(true);
                            menu.findItem(R.id.device_details_action)
                                    .getActionView()
                                    .setOnClickListener((v) -> {
                                        model.stopGetGattServices(true);
                                        model.device_details_services_view = getString(R.string.device_details_services_show_gatt_log);
                                        updateViewServices();
                                    });
                            menu.findItem(R.id.device_details_send_report).setEnabled(false);
                            menu.findItem(R.id.device_details_add_to_dashboard).setEnabled(false);
                            break;
                        case 2:
                            menu.findItem(R.id.device_details_action).setActionView(null).setTitle(R.string.device_details_disconnect).setIcon(R.drawable.ic_bluetooth_disconnect).setVisible(true);
                            menu.findItem(R.id.device_details_send_report).setEnabled(true);
                            menu.findItem(R.id.device_details_add_to_dashboard).setEnabled(true);
                            break;
                    }
                    break;
                case DeviceDetailsModel.VIEW_TERMINAL:
                    switch (model.terminalConnectState) {
                        case 0:
                            menu.findItem(R.id.device_details_action).setActionView(null).setTitle(R.string.device_details_connect).setIcon(R.drawable.ic_bluetooth_connect).setVisible(true);
                            menu.findItem(R.id.device_details_send_report).setEnabled(true);
                            menu.findItem(R.id.device_details_add_to_dashboard).setEnabled(true);
                            radio_button_spp.setEnabled(true);
                            radio_button_dial_up.setEnabled(true);
                            radio_button_my_service.setEnabled(true);
                            device_details_terminal_settings.setEnabled(true);
                            device_details_terminal_settings.setImageResource(R.drawable.ic_action_settings);
                            break;
                        case 1:
                            menu.findItem(R.id.device_details_action).setActionView(R.layout.actionbar_indeterminate_progress).setTitle(R.string.device_details_disconnect).setIcon(R.drawable.ic_bluetooth_connect).setVisible(true);
                            menu.findItem(R.id.device_details_send_report).setEnabled(false);
                            menu.findItem(R.id.device_details_add_to_dashboard).setEnabled(false);
                            radio_button_spp.setEnabled(false);
                            radio_button_dial_up.setEnabled(false);
                            radio_button_my_service.setEnabled(false);
                            device_details_terminal_settings.setEnabled(false);
                            device_details_terminal_settings.setImageResource(R.drawable.ic_action_settings_disable);
                            menu.findItem(R.id.device_details_action).getActionView().setOnClickListener(v -> btDriver.rfCommDisconnect(true));
                            break;
                        case 2:
                            menu.findItem(R.id.device_details_action).setActionView(null).setTitle(R.string.device_details_disconnect).setIcon(R.drawable.ic_bluetooth_disconnect).setVisible(true);
                            menu.findItem(R.id.device_details_send_report).setEnabled(false);
                            menu.findItem(R.id.device_details_add_to_dashboard).setEnabled(false);
                            radio_button_spp.setEnabled(false);
                            radio_button_dial_up.setEnabled(false);
                            radio_button_my_service.setEnabled(false);
                            device_details_terminal_settings.setEnabled(false);
                            device_details_terminal_settings.setImageResource(R.drawable.ic_action_settings_disable);
                            break;
                    }
                    break;
            }
        }

        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == android.R.id.home) {
                model.stopGetGattServices(false);
                btDriver.rfCommDisconnect(false);
                if (runFragment != null)
                    runFragment.Parent(MainActivityModel.getInstance().parentFragment, MainActivityModel.getInstance().childFragment);
                return true;
            }
            String selectedAction = (String) item.getTitle();
            if (selectedAction != null) {
                if (selectedAction.equals(getString(R.string.device_details_send_report))) {
                    sendReport();
                    return true;
                }
                if (selectedAction.equals(getString(R.string.device_details_add_to_dashboard))) {
                    HashMap<String, String> new_device = new HashMap<>();
                    new_device.put("name", model.device_name);
                    new_device.put("address", model.device_address);
                    new_device.put("class", model.device_class);
                    new_device.put("type", model.device_type);
                    new_device.put("latitude", String.valueOf(model.device_latitude));
                    new_device.put("longitude", String.valueOf(model.device_longitude));
                    new_device.put("scanRecord", model.device_scan_record);
                    new_device.put("serviceUUIDs", model.device_service_uuids);
                    new_device.put("manufacturer", model.device_manufacturer);
                    dbDatabaseDriver.insertDevice(new_device);
                    return true;
                }
                if (selectedAction.equals(getString(R.string.device_details_pair))) {
                    new Worker(getActivity(), btDriver, () -> pairDevices(model.device_address)).start();
                    return true;
                }
                if (selectedAction.equals(getString(R.string.device_details_connect))) {
                    new Worker(getActivity(), btDriver, () -> {
                        switch (model.viewMode) {
                            case DeviceDetailsModel.VIEW_SERVICES:
                                model.getGattServices();
                                model.device_details_services_view = getString(R.string.device_details_services_show_gatt_log);
                                updateViewServices();
                                break;
                            case DeviceDetailsModel.VIEW_TERMINAL:
                                switch (model.terminalServiceUUID) {
                                    case "00001101-0000-1000-8000-00805f9b34fb":
                                        model.terminalLogSPP = "log> " + getString(R.string.terminal_message_connection) + "\n";
                                        model.terminalLogSPP = "log> " + "00001101-0000-1000-8000-00805f9b34fb" + "\n" + model.terminalLogSPP;
                                        break;
                                    case "00001103-0000-1000-8000-00805f9b34fb":
                                        model.terminalLogDialUp = "log> " + getString(R.string.terminal_message_connection) + "\n";
                                        model.terminalLogDialUp = "log> " + "00001103-0000-1000-8000-00805f9b34fb" + "\n" + model.terminalLogDialUp;
                                        break;
                                    default:
                                        model.terminalLogMyService = "log> " + getString(R.string.terminal_message_connection) + "\n";
                                        if (!model.terminalServiceUUID.isEmpty())
                                            model.terminalLogMyService = "log> " + model.terminalServiceUUID + "\n" + model.terminalLogMyService;
                                        break;
                                }
                                model.terminalConnectState = 1;
                                updateViewTerminal();
                                btDriver.rfCommConnect(model.device_address, model.terminalServiceUUID, sharedPreferences.getBoolean(SettingsFragment.KEY_TERMINAL_CONNECTION_MODE, false));
                                break;
                        }
                    }).start();
                    return true;
                }
                if (selectedAction.equals(getString(R.string.device_details_disconnect))) {
                    new Worker(getActivity(), btDriver, () -> {
                        switch (model.viewMode) {
                            case DeviceDetailsModel.VIEW_SERVICES:
                                model.stopGetGattServices(true);
                                break;
                            case DeviceDetailsModel.VIEW_TERMINAL:
                                btDriver.rfCommDisconnect(true);
                                break;
                        }
                    }).start();
                }
            }
            return true;
        }
    }

}

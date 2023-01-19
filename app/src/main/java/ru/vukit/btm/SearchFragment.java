package ru.vukit.btm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import ru.vukit.btm.bluetooth.BluetoothDriver;
import ru.vukit.btm.bluetooth.BluetoothScanRecord;

@Keep
public class SearchFragment extends ListFragment {

    final SearchModel model = SearchModel.getInstance();
    final BluetoothDriver btDriver = BluetoothDriver.getInstance();
    SharedPreferences sharedPreferences;
    RadioGroup searchRadioGroup;
    RadioButton searchRadioButtonCLC, searchRadioButtonLE;
    BaseAdapter bluetoothDevicesAdapterCLC;
    BaseAdapter bluetoothDevicesAdapterLE;
    FloatingActionButton fabSearchFilter;
    RunFragment runFragment;
    WebSocket wsSocket;

    class BluetoothDevicesAdapter extends ArrayAdapter<HashMap<String, String>> {

        final Context context;
        final int resource;
        final int bluetoothType;
        final List<HashMap<String, String>> devices;

        BluetoothDevicesAdapter(Context context, int resource, List<HashMap<String, String>> objects, int type) {
            super(context, resource, objects);
            this.context = context;
            this.resource = resource;
            this.devices = objects;
            this.bluetoothType = type;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = convertView;
            if (inflater != null) {
                if (row == null) row = inflater.inflate(resource, parent, false);
                TextView deviceItem = row.findViewById(R.id.search_device_list_item);
                ImageView selectDevice = row.findViewById(R.id.search_select_device);
                HashMap<String, String> device = devices.get(position);
                switch (bluetoothType) {
                    case BluetoothDriver.BLUETOOTH_CLC_MODE:
                        if (sharedPreferences.getString(SettingsFragment.KEY_ABOUT_DEVICE, "").equals(getString(R.string.about_device_details))) {
                            CharSequence device_info;
                            SpannableString device_parameter = new SpannableString("\n" + getString(R.string.device_name) + " " + device.get("name") + "\n");
                            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 1, getString(R.string.device_name).length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.color_device_name))), getString(R.string.device_name).length() + 2, device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_name).length() + 1, device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_info = TextUtils.concat(device_parameter);
                            device_parameter = new SpannableString(getString(R.string.device_mac_address) + " " + device.get("address") + "\n");
                            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_mac_address).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_mac_address).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_info = TextUtils.concat(device_info, device_parameter);
                            device_parameter = new SpannableString(getString(R.string.device_rssi) + " " + device.get("rssi") + " " + getString(R.string.dBm) + "\n");
                            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_rssi).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_rssi).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_info = TextUtils.concat(device_info, device_parameter);
                            if (!getDeviceClass(device).isEmpty()) {
                                device_parameter = new SpannableString(getString(R.string.device_class) + " " + getDeviceClass(device) + "\n");
                                device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_class).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_class).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                device_info = TextUtils.concat(device_info, device_parameter);
                            }
                            if (!Objects.equals(device.get("type"), "UNKNOWN")) {
                                device_parameter = new SpannableString(getString(R.string.device_type) + " " + device.get("type") + "\n");
                                device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_type).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_type).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                device_info = TextUtils.concat(device_info, device_parameter);
                            }
                            deviceItem.setText(device_info);
                            if (Objects.equals(device.get("isSelected"), "true"))
                                selectDevice.setVisibility(View.VISIBLE);
                            else selectDevice.setVisibility(View.INVISIBLE);
                        } else if (sharedPreferences.getString(SettingsFragment.KEY_ABOUT_DEVICE, "").equals(getString(R.string.about_device_brief))) {
                            CharSequence device_info;
                            SpannableString device_parameter = new SpannableString("\n" + getString(R.string.device_name) + " " + device.get("name") + "\n");
                            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 1, getString(R.string.device_name).length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.color_device_name))), getString(R.string.device_name).length() + 2, device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_name).length() + 1, device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_info = TextUtils.concat(device_parameter);
                            device_parameter = new SpannableString(getString(R.string.device_mac_address) + " " + device.get("address") + "\n");
                            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_mac_address).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_mac_address).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_info = TextUtils.concat(device_info, device_parameter);
                            deviceItem.setText(device_info);
                            if (Objects.equals(device.get("isSelected"), "true"))
                                selectDevice.setVisibility(View.VISIBLE);
                            else selectDevice.setVisibility(View.INVISIBLE);
                        }
                        break;
                    case BluetoothDriver.BLUETOOTH_LE_MODE:
                        if (sharedPreferences.getString(SettingsFragment.KEY_ABOUT_DEVICE, "").equals(getString(R.string.about_device_details))) {
                            CharSequence device_info;
                            SpannableString device_parameter = new SpannableString("\n" + getString(R.string.device_name) + " " + device.get("name") + "\n");
                            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 1, getString(R.string.device_name).length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.color_device_name))), getString(R.string.device_name).length() + 2, device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_name).length() + 1, device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_info = TextUtils.concat(device_parameter);
                            device_parameter = new SpannableString(getString(R.string.device_mac_address) + " " + device.get("address") + "\n");
                            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_mac_address).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_mac_address).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_info = TextUtils.concat(device_info, device_parameter);
                            device_parameter = new SpannableString(getString(R.string.device_rssi) + " " + device.get("rssi") + " " + getString(R.string.dBm) + "\n");
                            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_rssi).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_rssi).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_info = TextUtils.concat(device_info, device_parameter);
                            if (!getDeviceClass(device).isEmpty()) {
                                device_parameter = new SpannableString(getString(R.string.device_class) + getDeviceClass(device) + "\n");
                                device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_class).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_class).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                device_info = TextUtils.concat(device_info, device_parameter);
                            }
                            for (int i = 0; i < BluetoothScanRecord.dataTypeValueName.size(); i++) {
                                String dataType = BluetoothScanRecord.dataTypeValueName.valueAt(i);
                                if (device.containsKey(dataType)) {
                                    device_parameter = new SpannableString(dataType + ": " + device.get(dataType) + "\n");
                                    device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, dataType.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    device_parameter.setSpan(new TypefaceSpan("monospace"), dataType.length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    device_info = TextUtils.concat(device_info, device_parameter);
                                }
                            }
                            device_parameter = new SpannableString(getString(R.string.device_scan_record) + " " + device.get("scanRecord") + "\n");
                            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_scan_record).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_scan_record).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_info = TextUtils.concat(device_info, device_parameter);
                            deviceItem.setText(device_info);
                            if (Objects.equals(device.get("isSelected"), "true"))
                                selectDevice.setVisibility(View.VISIBLE);
                            else selectDevice.setVisibility(View.INVISIBLE);
                        } else if (sharedPreferences.getString(SettingsFragment.KEY_ABOUT_DEVICE, "").equals(getString(R.string.about_device_brief))) {
                            CharSequence device_info;
                            SpannableString device_parameter = new SpannableString("\n" + getString(R.string.device_name) + " " + device.get("name") + "\n");
                            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 1, getString(R.string.device_name).length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.color_device_name))), getString(R.string.device_name).length() + 2, device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_name).length() + 1, device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_info = TextUtils.concat(device_parameter);
                            device_parameter = new SpannableString(getString(R.string.device_mac_address) + " " + device.get("address") + "\n");
                            device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_mac_address).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_mac_address).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            device_info = TextUtils.concat(device_info, device_parameter);
                            deviceItem.setText(device_info);
                            if (Objects.equals(device.get("isSelected"), "true"))
                                selectDevice.setVisibility(View.VISIBLE);
                            else selectDevice.setVisibility(View.INVISIBLE);
                        }
                        break;
                }
            }
            return row;
        }
    }

    private String getDeviceClass(Map<String, String> device) {
        if (Objects.equals(device.get("class"), "UNKNOWN")) return "";
        else return device.get("class");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new SearchFragment.FragmentMenuProvider(), getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        fabSearchFilter = view.findViewById(R.id.search_filter_fab);
        fabSearchFilter.setOnClickListener(onClickListenerSetSearchFilterFAB);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(StartApplication.getInstance());
        model.connectController(this);
        if (btDriver.isLeSupported()) {
            bluetoothDevicesAdapterCLC = new BluetoothDevicesAdapter(getActivity(), R.layout.fragment_search_device_list_item, model.bluetoothDevicesCLC, BluetoothDriver.BLUETOOTH_CLC_MODE);
            bluetoothDevicesAdapterLE = new BluetoothDevicesAdapter(getActivity(), R.layout.fragment_search_device_list_item, model.bluetoothDevicesLE, BluetoothDriver.BLUETOOTH_LE_MODE);
        } else {
            bluetoothDevicesAdapterCLC = new BluetoothDevicesAdapter(getActivity(), R.layout.fragment_search_device_list_item, model.bluetoothDevicesCLC, BluetoothDriver.BLUETOOTH_CLC_MODE);
        }
        searchRadioGroup = requireActivity().findViewById(R.id.search_fragment_radio_group);
        searchRadioButtonCLC = requireActivity().findViewById(R.id.search_fragment_radio_button_clc);
        searchRadioButtonLE = requireActivity().findViewById(R.id.search_fragment_radio_button_le);
        if (!btDriver.isLeSupported()) {
            searchRadioButtonLE.setEnabled(false);
            searchRadioButtonLE.setClickable(false);
        }
        searchRadioGroup.setOnCheckedChangeListener(searchRadioGroupListener);
        if (model.bluetoothModeSelected == -1) {
            if (sharedPreferences.getString(SettingsFragment.KEY_SEARCH_DEFAULT_MODE, "").equals(getString(R.string.bluetooth_clc)))
                model.bluetoothModeSelected = BluetoothDriver.BLUETOOTH_CLC_MODE;
            if (btDriver.isLeSupported()) {
                if (sharedPreferences.getString(SettingsFragment.KEY_SEARCH_DEFAULT_MODE, "").equals(getString(R.string.bluetooth_le)))
                    model.bluetoothModeSelected = BluetoothDriver.BLUETOOTH_LE_MODE;
            } else model.bluetoothModeSelected = BluetoothDriver.BLUETOOTH_CLC_MODE;
        }

        switch (model.bluetoothModeSelected) {
            case BluetoothDriver.BLUETOOTH_CLC_MODE:
                setListAdapter(bluetoothDevicesAdapterCLC);
                getListView().setSelection(model.currentPositionCLC);
                searchRadioButtonCLC.setChecked(true);
                break;
            case BluetoothDriver.BLUETOOTH_LE_MODE:
                setListAdapter(bluetoothDevicesAdapterLE);
                getListView().setSelection(model.currentPositionLE);
                searchRadioButtonLE.setChecked(true);
                break;
        }
        updateControlDiscovery();
        getListView().setOnItemClickListener(onItemClickListener);
        getListView().setOnItemLongClickListener(onItemLongClickListener);
        if (btDriver.isSetSearchFilter()) {
            fabSearchFilter.setImageResource(R.drawable.ic_search_filter_on);
        } else {
            fabSearchFilter.setImageResource(R.drawable.ic_search_filter_off);
        }
        try {
            runFragment = (RunFragment) getActivity();
        } catch (ClassCastException e) {
            runFragment = null;
        }
        requireActivity().invalidateOptionsMenu();
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
        super.onPause();
    }

    final RadioGroup.OnCheckedChangeListener searchRadioGroupListener = new RadioGroup.OnCheckedChangeListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.search_fragment_radio_button_clc:
                    model.bluetoothModeSelected = BluetoothDriver.BLUETOOTH_CLC_MODE;
                    setListAdapter(bluetoothDevicesAdapterCLC);
                    getListView().setSelection(model.currentPositionCLC);
                    break;
                case R.id.search_fragment_radio_button_le:
                    model.bluetoothModeSelected = BluetoothDriver.BLUETOOTH_LE_MODE;
                    setListAdapter(bluetoothDevicesAdapterLE);
                    getListView().setSelection(model.currentPositionLE);
                    break;
                default:
                case -1:
                    break;
            }
            requireActivity().invalidateOptionsMenu();
        }
    };


    public void updateControlDiscovery() {
        if (model.isDiscoveryRunning) {
            if (btDriver.isLeSupported()) {
                searchRadioButtonCLC.setEnabled(false);
                searchRadioButtonLE.setEnabled(false);
            } else {
                searchRadioButtonCLC.setEnabled(false);
            }
            fabSearchFilter.setVisibility(View.INVISIBLE);
        } else {
            if (btDriver.isLeSupported()) {
                searchRadioButtonCLC.setEnabled(true);
                searchRadioButtonLE.setEnabled(true);
            } else {
                searchRadioButtonCLC.setEnabled(true);
            }
            fabSearchFilter.setVisibility(View.VISIBLE);
            switch (model.bluetoothModeSelected) {
                case BluetoothDriver.BLUETOOTH_CLC_MODE:
                    if (!model.lastDateSearchCLC.isEmpty()) {
                        new SnackBar().ShowLong(getString(R.string.detected_devices) + ": " + bluetoothDevicesAdapterCLC.getCount());
                        model.lastDateSearchCLC = "";
                    }
                    bluetoothDevicesAdapterCLC.notifyDataSetChanged();
                    break;
                case BluetoothDriver.BLUETOOTH_LE_MODE:
                    if (!model.lastDateSearchLE.isEmpty()) {
                        new SnackBar().ShowLong(getString(R.string.detected_devices) + ": " + bluetoothDevicesAdapterLE.getCount());
                        model.lastDateSearchLE = "";
                    }
                    bluetoothDevicesAdapterLE.notifyDataSetChanged();
                    break;
            }
        }
        requireActivity().invalidateOptionsMenu();
    }

    public void updateView() {
        switch (model.bluetoothModeSelected) {
            case BluetoothDriver.BLUETOOTH_CLC_MODE:
                bluetoothDevicesAdapterCLC.notifyDataSetChanged();
                break;
            case BluetoothDriver.BLUETOOTH_LE_MODE:
                bluetoothDevicesAdapterLE.notifyDataSetChanged();
                break;
        }
    }

    private boolean isVisibleSendReportMenuItem() {
        boolean result = false;
        switch (model.bluetoothModeSelected) {
            case BluetoothDriver.BLUETOOTH_CLC_MODE:
                if (!model.isDiscoveryRunning) {
                    if (!bluetoothDevicesAdapterCLC.isEmpty()) result = true;
                }
                break;
            case BluetoothDriver.BLUETOOTH_LE_MODE:
                if (!model.isDiscoveryRunning) {
                    if (!bluetoothDevicesAdapterLE.isEmpty()) result = true;
                }
                break;
        }
        return result;
    }

    private boolean isVisibleCancelCheckDevicesMenuItem() {
        boolean result = false;
        switch (model.bluetoothModeSelected) {
            case BluetoothDriver.BLUETOOTH_CLC_MODE:
                if (!model.isDiscoveryRunning) {
                    if (model.counterSelectedBluetoothDevicesCLC != 0) result = true;
                }
                break;
            case BluetoothDriver.BLUETOOTH_LE_MODE:
                if (!model.isDiscoveryRunning) {
                    if (model.counterSelectedBluetoothDevicesLE != 0) result = true;
                }
                break;
        }
        return result;
    }


    @SuppressLint("CustomX509TrustManager")
    private void sendReportServer(final Context context) {
        final String deviceList;
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

        deviceList = jsonReportFormat();
        bodyContentType = "application/json; charset=utf-8";
        sendingMessage = getString(R.string.search_send_json_report_server) + " " + server_url;
        successfullySent = getString(R.string.search_successfully_send_json_report_server) + " " + server_url;
        notSent = getString(R.string.search_unsuccessfully_send_json_report_server) + " " + server_url;
        switch (server_url.split(":")[0]) {
            case "ws":
            case "wss":
                if (wsSocket != null) {
                    new SnackBar().ShowIndefinite(sendingMessage);
                    if (!deviceList.isEmpty()) {
                        wsSocket.sendBinary(deviceList.getBytes(StandardCharsets.UTF_8));
                        if (wsSocket.getState() != WebSocketState.OPEN) {
                            new SnackBar().ShowLong(notSent);
                        } else {
                            new SnackBar().ShowLong(successfullySent);
                        }
                    } else {
                        new SnackBar().ShowLong(notSent);
                    }
                }
                break;
            case "https":
                TrustManager[] trustAllCerts = new TrustManager[]{
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
                if (server_url.isEmpty()) {
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
                            return deviceList.getBytes(StandardCharsets.UTF_8);
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

    private String jsonReportFormat() {
        JSONObject deviceList = new JSONObject();
        JSONArray deviceArray = new JSONArray();
        switch (model.bluetoothModeSelected) {
            case BluetoothDriver.BLUETOOTH_CLC_MODE:
                for (HashMap<String, String> device : model.bluetoothDevicesCLC) {
                    JSONObject deviceObject = new JSONObject();
                    if (model.counterSelectedBluetoothDevicesCLC > 0 && Objects.equals(device.get("isSelected"), "true")) {
                        try {
                            deviceObject.put("name", device.get("name"));
                            deviceObject.put("address", device.get("address"));
                            deviceObject.put("rssi", device.get("rssi"));
                            if (!getDeviceClass(device).isEmpty())
                                deviceObject.put("class", getDeviceClass(device));
                            deviceObject.put("type", device.get("type"));
                            deviceObject.put("latitude", device.get("latitude"));
                            deviceObject.put("longitude", device.get("longitude"));
                        } catch (JSONException ex) {
                            continue;
                        }
                    }
                    if (model.counterSelectedBluetoothDevicesCLC <= 0) {
                        try {
                            deviceObject.put("name", device.get("name"));
                            deviceObject.put("address", device.get("address"));
                            deviceObject.put("rssi", device.get("rssi"));
                            if (!getDeviceClass(device).isEmpty())
                                deviceObject.put("class", getDeviceClass(device));
                            deviceObject.put("type", device.get("type"));
                            deviceObject.put("latitude", device.get("latitude"));
                            deviceObject.put("longitude", device.get("longitude"));
                        } catch (JSONException ex) {
                            continue;
                        }
                    }
                    if (!deviceObject.isNull("name")) deviceArray.put(deviceObject);
                }
                try {
                    deviceList.put("devices", deviceArray);
                } catch (JSONException ex) {
                    deviceList = null;
                }
                break;
            case BluetoothDriver.BLUETOOTH_LE_MODE:
                for (HashMap<String, String> device : model.bluetoothDevicesLE) {
                    JSONObject deviceObject = new JSONObject();
                    if (model.counterSelectedBluetoothDevicesLE > 0 && Objects.equals(device.get("isSelected"), "true")) {
                        try {
                            deviceObject.put("name", device.get("name"));
                            deviceObject.put("address", device.get("address"));
                            deviceObject.put("rssi", device.get("rssi"));
                            if (!getDeviceClass(device).isEmpty())
                                deviceObject.put("class", getDeviceClass(device));
                            deviceObject.put("type", device.get("type"));
                            deviceObject.put("latitude", device.get("latitude"));
                            deviceObject.put("longitude", device.get("longitude"));
                            for (int i = 0; i < BluetoothScanRecord.dataTypeValueName.size(); i++) {
                                String dataType = BluetoothScanRecord.dataTypeValueName.valueAt(i);
                                if (device.containsKey(dataType)) {
                                    deviceObject.put(dataType, Objects.requireNonNull(device.get(dataType)).replaceFirst("\n", "").replaceAll("\n", ", "));
                                }
                            }
                            deviceObject.put("scanRecord", Objects.requireNonNull(device.get("scanRecord")).replaceAll("\n", ""));
                        } catch (JSONException ex) {
                            continue;
                        }
                    }
                    if (model.counterSelectedBluetoothDevicesLE <= 0) {
                        try {
                            deviceObject.put("name", device.get("name"));
                            deviceObject.put("address", device.get("address"));
                            deviceObject.put("rssi", device.get("rssi"));
                            if (!getDeviceClass(device).isEmpty())
                                deviceObject.put("class", getDeviceClass(device));
                            deviceObject.put("type", device.get("type"));
                            deviceObject.put("latitude", device.get("latitude"));
                            deviceObject.put("longitude", device.get("longitude"));
                            for (int i = 0; i < BluetoothScanRecord.dataTypeValueName.size(); i++) {
                                String dataType = BluetoothScanRecord.dataTypeValueName.valueAt(i);
                                if (device.containsKey(dataType)) {
                                    deviceObject.put(dataType, Objects.requireNonNull(device.get(dataType)).replaceFirst("\n", "").replaceAll("\n", ", "));
                                }
                            }
                            deviceObject.put("scanRecord", Objects.requireNonNull(device.get("scanRecord")).replaceAll("\n", ""));
                        } catch (JSONException ex) {
                            continue;
                        }
                    }
                    if (!deviceObject.isNull("name")) deviceArray.put(deviceObject);
                }
                try {
                    deviceList.put("devices", deviceArray);
                } catch (JSONException ex) {
                    deviceList = null;
                }
                break;
        }
        if (deviceList != null) return deviceList.toString();
        return "";
    }

    private final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!model.isDiscoveryRunning) {
                DeviceDetailsModel.getInstance().initDetails();
                switch (model.bluetoothModeSelected) {
                    case BluetoothDriver.BLUETOOTH_CLC_MODE:
                        DeviceDetailsModel.getInstance().getDevice(model.bluetoothDevicesCLC.get(position));
                        model.currentPositionCLC = position;
                        if (runFragment != null)
                            runFragment.Child(getString(R.string.action_search), getString(R.string.action_device_details));
                        break;
                    case BluetoothDriver.BLUETOOTH_LE_MODE:
                        DeviceDetailsModel.getInstance().getDevice(model.bluetoothDevicesLE.get(position));
                        model.currentPositionLE = position;
                        if (runFragment != null)
                            runFragment.Child(getString(R.string.action_search), getString(R.string.action_device_details));
                        break;
                }
            }
        }
    };

    private final AdapterView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {

        HashMap<String, String> device;

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (!model.isDiscoveryRunning) {
                switch (model.bluetoothModeSelected) {
                    case BluetoothDriver.BLUETOOTH_CLC_MODE:
                        device = model.bluetoothDevicesCLC.get(position);
                        if (Objects.equals(device.get("isSelected"), "false")) {
                            device.put("isSelected", "true");
                            model.counterSelectedBluetoothDevicesCLC++;
                        } else {
                            device.put("isSelected", "false");
                            model.counterSelectedBluetoothDevicesCLC--;
                        }
                        bluetoothDevicesAdapterCLC.notifyDataSetChanged();
                        break;
                    case BluetoothDriver.BLUETOOTH_LE_MODE:
                        device = model.bluetoothDevicesLE.get(position);
                        if (Objects.equals(device.get("isSelected"), "false")) {
                            device.put("isSelected", "true");
                            model.counterSelectedBluetoothDevicesLE++;
                        } else {
                            device.put("isSelected", "false");
                            model.counterSelectedBluetoothDevicesLE--;
                        }
                        bluetoothDevicesAdapterLE.notifyDataSetChanged();
                        break;
                }
            }
            requireActivity().invalidateOptionsMenu();
            return true;
        }
    };

    private void cancelCheckDevices() {
        if (!model.isDiscoveryRunning) {
            switch (model.bluetoothModeSelected) {
                case BluetoothDriver.BLUETOOTH_CLC_MODE:
                    for (Map<String, String> device : model.bluetoothDevicesCLC) {
                        device.put("isSelected", "false");
                    }
                    bluetoothDevicesAdapterCLC.notifyDataSetChanged();
                    model.counterSelectedBluetoothDevicesCLC = 0;
                    requireActivity().invalidateOptionsMenu();
                    break;
                case BluetoothDriver.BLUETOOTH_LE_MODE:
                    for (Map<String, String> device : model.bluetoothDevicesLE) {
                        device.put("isSelected", "false");
                    }
                    bluetoothDevicesAdapterLE.notifyDataSetChanged();
                    model.counterSelectedBluetoothDevicesLE = 0;
                    requireActivity().invalidateOptionsMenu();
                    break;
            }
        }
    }

    final View.OnClickListener onClickListenerSetSearchFilterFAB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            View view = View.inflate(getActivity(), R.layout.search_filter, null);
            final EditText filterText = view.findViewById(R.id.search_filter_string);
            final TextView rssiLevel = view.findViewById(R.id.search_filter_rssi_level);
            final SeekBar seekBar = view.findViewById(R.id.search_filter_seek_bar);
            if (btDriver.isSetRangeRssi()) {
                rssiLevel.setText(String.valueOf(btDriver.getMinRssi()));
                seekBar.setProgress(btDriver.getMinRssi() + 127);
            } else {
                rssiLevel.setText(String.valueOf(-127));
                seekBar.setProgress(0);
            }
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    rssiLevel.setText(String.valueOf(progress - 127));
                    btDriver.setMinRssi((progress - 127));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            filterText.setText(btDriver.getSearchStringFilter());
            setFilterRadioGroup(view);
            builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
                btDriver.setSearchStringFilter(filterText.getText().toString().trim());
                if (btDriver.isSetSearchFilter()) {
                    fabSearchFilter.setImageResource(R.drawable.ic_search_filter_on);
                } else {
                    fabSearchFilter.setImageResource(R.drawable.ic_search_filter_off);
                }
            });

            builder.setNegativeButton(R.string.dashboard_search_reset_button, (dialog, which) -> {
                btDriver.unsetSearchStringFilter();
                btDriver.unsetSearchCondition();
                btDriver.unsetRangeRssi();
                if (btDriver.isSetSearchFilter()) {
                    fabSearchFilter.setImageResource(R.drawable.ic_search_filter_on);
                } else {
                    fabSearchFilter.setImageResource(R.drawable.ic_search_filter_off);
                }
            });
            builder.setView(view);
            builder.setTitle(R.string.search_filter_dialog_title);
            builder.create();
            builder.show();
        }
    };

    @SuppressLint("NonConstantResourceId")
    void setFilterRadioGroup(View view) {
        RadioGroup filterRadioGroup = view.findViewById(R.id.search_filter_radio_group);
        if (btDriver.getSearchCondition().equals(getString(R.string.radio_button_none))) {
            ((RadioButton) view.findViewById(R.id.search_filter_radio_button_none)).setChecked(true);
        }
        if (btDriver.getSearchCondition().equals(getString(R.string.radio_button_only))) {
            ((RadioButton) view.findViewById(R.id.search_filter_radio_button_only)).setChecked(true);
        }
        if (btDriver.getSearchCondition().equals(getString(R.string.radio_button_or))) {
            ((RadioButton) view.findViewById(R.id.search_filter_radio_button_or)).setChecked(true);
        }
        if (btDriver.getSearchCondition().equals(getString(R.string.radio_button_and))) {
            ((RadioButton) view.findViewById(R.id.search_filter_radio_button_and)).setChecked(true);
        }
        filterRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.search_filter_radio_button_none:
                    btDriver.setSearchCondition(getString(R.string.radio_button_none));
                    break;
                case R.id.search_filter_radio_button_only:
                    btDriver.setSearchCondition(getString(R.string.radio_button_only));
                    break;
                case R.id.search_filter_radio_button_or:
                    btDriver.setSearchCondition(getString(R.string.radio_button_or));
                    break;
                case R.id.search_filter_radio_button_and:
                    btDriver.setSearchCondition(getString(R.string.radio_button_and));
                    break;
                default:
                case -1:
                    break;
            }
        });
    }

    private class FragmentMenuProvider implements MenuProvider {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setTitle(getResources().getString(R.string.app_name_short) + " - " + getResources().getString(R.string.action_search));
            }
            menuInflater.inflate(R.menu.menu_search, menu);
            if (model.isDiscoveryRunning) {
                menu.findItem(R.id.search_start_stop).setActionView(R.layout.actionbar_indeterminate_progress).setTitle(R.string.button_search_stop).setVisible(true);
                menu.findItem(R.id.search_start_stop).getActionView().setOnClickListener((v) -> {
                    btDriver.stopDiscovery(model.bluetoothModeSelected);
                    model.isDiscoveryRunning = false;
                    updateControlDiscovery();
                });
            } else {
                menu.findItem(R.id.search_start_stop).setActionView(null).setTitle(R.string.button_search_start).setIcon(R.drawable.ic_action_search).setVisible(true);
            }
            menu.findItem(R.id.search_send_report).setVisible(isVisibleSendReportMenuItem());
            menu.findItem(R.id.search_cancel_check_devices).setVisible(isVisibleCancelCheckDevicesMenuItem());
        }

        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            String selectedAction = (String) item.getTitle();
            if (selectedAction != null) {
                if (selectedAction.equals(getString(R.string.search_send_report))) {
                    sendReportServer(getActivity());
                    return true;
                }
                if (selectedAction.equals(getString(R.string.search_cancel_selection))) {
                    cancelCheckDevices();
                    return true;
                }
                if (selectedAction.equals(getString(R.string.button_search_start))) {
                    new Worker(getActivity(), btDriver, () -> {
                        switch (model.bluetoothModeSelected) {
                            case BluetoothDriver.BLUETOOTH_CLC_MODE:
                                model.bluetoothDevicesCLC.clear();
                                bluetoothDevicesAdapterCLC.notifyDataSetChanged();
                                model.counterSelectedBluetoothDevicesCLC = 0;
                                model.currentPositionCLC = 0;
                                model.lastDateSearchCLC = String.format(Locale.getDefault(), "%1$tH:%1$tM:%1$tS %1$td.%1$tm.%1$tY", Calendar.getInstance());
                                btDriver.startDiscovery(model.bluetoothModeSelected);
                                model.isDiscoveryRunning = true;
                                updateControlDiscovery();
                                break;
                            case BluetoothDriver.BLUETOOTH_LE_MODE:
                                model.bluetoothDevicesLE.clear();
                                bluetoothDevicesAdapterLE.notifyDataSetChanged();
                                model.counterSelectedBluetoothDevicesLE = 0;
                                model.currentPositionLE = 0;
                                model.lastDateSearchLE = String.format(Locale.getDefault(), "%1$tH:%1$tM:%1$tS  %1$td.%1$tm.%1$tY", Calendar.getInstance());
                                btDriver.startDiscovery(model.bluetoothModeSelected);
                                model.isDiscoveryRunning = true;
                                updateControlDiscovery();
                                break;
                        }
                    }).start();
                }
            }
            return true;
        }
    }

}


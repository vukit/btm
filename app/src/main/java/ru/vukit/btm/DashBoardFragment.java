package ru.vukit.btm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import ru.vukit.btm.bluetooth.BluetoothDriver;
import ru.vukit.btm.database.DatabaseDriver;

@Keep
public class DashBoardFragment extends ListFragment implements OnMapReadyCallback {

    final MainActivityModel mainActivityModel = MainActivityModel.getInstance();
    final DashBoardModel model = DashBoardModel.getInstance();
    final DatabaseDriver dbDriver = DatabaseDriver.getInstance();
    final BluetoothDriver btDriver = BluetoothDriver.getInstance();

    SharedPreferences sharedPreferences;
    BluetoothDevicesAdapter bluetoothDevicesAdapter;
    RunFragment runFragment;
    ArrayList<String> pairedDevices;
    FloatingActionButton fabSearch;
    MapView mapViewSearch = null;
    GoogleMap mapSearch = null;
    Polyline searchArea = null;
    RadioGroup searchRadioGroup;
    Double searchAreaSize;
    AlertDialog alertDialog = null;

    public DashBoardFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new FragmentMenuProvider(), getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        fabSearch = view.findViewById(R.id.dashboard_fab_search);
        fabSearch.setOnClickListener(onClickListenerSearchFAB);
        return view;
    }

    private class BluetoothDevicesAdapter extends BaseAdapter {

        private final Context context;
        private final int resource;
        private final DatabaseDriver dbDatabaseDriver;
        private int count;
        private Cursor rowIds;

        BluetoothDevicesAdapter(Context context, int resource, DatabaseDriver dbDatabaseDriver) {
            this.context = context;
            this.resource = resource;
            this.dbDatabaseDriver = dbDatabaseDriver;
            closeCursor();
            this.rowIds = getRowIds(model.query);
            this.count = this.rowIds.getCount();
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = convertView;
            if (inflater != null) {
                if (row == null) row = inflater.inflate(resource, parent, false);
                TextView deviceItem = row.findViewById(R.id.dashboard_device_list_item);
                ImageView selectDevice = row.findViewById(R.id.dashboard_select_device);
                Cursor cursor = getBluetoothDevice(position);
                CharSequence device_info;
                String device_name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.NAME));
                SpannableString device_parameter = new SpannableString("\n" + getString(R.string.device_name) + " " + device_name + "\n");
                device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 1, getString(R.string.device_name).length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_parameter.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.color_device_name))), getString(R.string.device_name).length() + 2, device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_name).length() + 1, device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_info = TextUtils.concat(device_parameter);
                String device_address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.ADDRESS));
                device_parameter = new SpannableString(getString(R.string.device_mac_address) + " " + device_address + "\n");
                device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_mac_address).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_mac_address).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_info = TextUtils.concat(device_info, device_parameter);
                String device_paired = pairedDevices.contains(device_address) ? getString(R.string.yes) : getString(R.string.no);
                device_parameter = new SpannableString(getString(R.string.device_paired) + " " + device_paired + "\n");
                device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_paired).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_paired).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_info = TextUtils.concat(device_info, device_parameter);
                String device_class = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.CLASS));
                if (!device_class.equals("UNKNOWN")) {
                    device_parameter = new SpannableString(getString(R.string.device_class) + " " + device_class + "\n");
                    device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_class).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_class).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    device_info = TextUtils.concat(device_info, device_parameter);
                }
                String device_type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.TYPE));
                if (!device_type.equals("UNKNOWN")) {
                    device_parameter = new SpannableString(getString(R.string.device_type) + " " + device_type + "\n");
                    device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_type).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_type).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    device_info = TextUtils.concat(device_info, device_parameter);
                }
                deviceItem.setText(device_info);
                String device_latitude_string;
                Double device_latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.LATITUDE));
                if (device_latitude == MathModule.notLatitude)
                    device_latitude_string = getString(R.string.undefined);
                else device_latitude_string = String.valueOf(device_latitude);
                device_parameter = new SpannableString(getString(R.string.device_latitude) + " " + device_latitude_string + "\n");
                device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_latitude).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_latitude).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_info = TextUtils.concat(device_info, device_parameter);
                String device_longitude_string;
                Double device_longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.LONGITUDE));
                if (device_longitude == MathModule.notLongitude)
                    device_longitude_string = getString(R.string.undefined);
                else device_longitude_string = String.valueOf(device_longitude);
                device_parameter = new SpannableString(getString(R.string.device_longitude) + " " + device_longitude_string + "\n");
                device_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.device_longitude).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.device_longitude).length(), device_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                device_info = TextUtils.concat(device_info, device_parameter);
                deviceItem.setText(device_info);
                if (cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.SELECTED)) == 1)
                    selectDevice.setVisibility(View.VISIBLE);
                else selectDevice.setVisibility(View.INVISIBLE);
                cursor.close();
            }
            return row;
        }

        @Override
        public Object getItem(int position) {
            if (rowIds.moveToPosition(position)) {
                long rowId = rowIds.getLong(0);
                return getRowById(rowId);
            } else return null;
        }

        @Override
        public long getItemId(int position) {
            if (rowIds.moveToPosition(position)) return rowIds.getLong(0);
            else return 0;
        }

        Cursor getRowIds(String query) {
            return dbDatabaseDriver.rawQuery(query);
        }

        Cursor getRowById(long rowId) {
            return dbDatabaseDriver.rawQuery("SELECT * FROM " + DatabaseDriver.DatabaseContract.Devices.TABLE_NAME + " WHERE _ID = " + rowId + ";");
        }

        Cursor getBluetoothDevice(int position) {
            rowIds.moveToPosition(position);
            long rowId = rowIds.getLong(0);
            Cursor cursor = getRowById(rowId);
            cursor.moveToFirst();
            return cursor;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            closeCursor();
            this.rowIds = getRowIds(model.query);
            this.count = this.rowIds.getCount();
        }

        private void closeCursor() {
            if (this.rowIds != null && !this.rowIds.isClosed()) this.rowIds.close();
        }

        @Override
        protected void finalize() throws Throwable {
            closeCursor();
            super.finalize();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(StartApplication.getInstance());
        model.connectController(this);
        bluetoothDevicesAdapter = new BluetoothDevicesAdapter(getActivity(), R.layout.fragment_dashboard_device_list_item, dbDriver);
        setListAdapter(bluetoothDevicesAdapter);
        getListView().setOnItemClickListener(onItemClickListener);
        getListView().setOnItemLongClickListener(onItemLongClickListener);
        setSelection(model.currentPosition);
        try {
            runFragment = (RunFragment) getActivity();
        } catch (ClassCastException e) {
            runFragment = null;
        }
        updateView();
        if (model.isSetSearchFilter()) {
            fabSearch.setImageResource(R.drawable.ic_search_filter_on);
        } else {
            fabSearch.setImageResource(R.drawable.ic_search_filter_off);
        }
    }

    @Override
    public void onPause() {
        if (alertDialog != null) {
            alertDialog.cancel();
        }
        model.disconnectController();
        bluetoothDevicesAdapter = null;
        super.onPause();
    }

    private final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DeviceDetailsModel.getInstance().initDetails();
            Cursor cursor = bluetoothDevicesAdapter.getBluetoothDevice(position);
            DeviceDetailsModel.getInstance().getDevice(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices._ID)));
            cursor.close();
            model.currentPosition = position;
            if (runFragment != null) {
                runFragment.Child(getString(R.string.action_dashboard), getString(R.string.action_device_details));
            }
        }
    };

    private final AdapterView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = bluetoothDevicesAdapter.getBluetoothDevice(position);
            long deviceId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices._ID));
            int isSelected = (cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.SELECTED)) == 0) ? 1 : 0;
            cursor.close();
            dbDriver.selectDevice(deviceId, isSelected);
            updateView();
            return true;
        }
    };

    void updateView() {
        if (mainActivityModel.permissionBluetooth) {
            pairedDevices = btDriver.getBondedDevices();
        }
        bluetoothDevicesAdapter.notifyDataSetChanged();
        requireActivity().invalidateOptionsMenu();
    }

    final View.OnClickListener onClickListenerSearchFAB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            searchAreaSize = Double.valueOf(sharedPreferences.getString(SettingsFragment.KEY_DASHBOARD_SEARCH_AREA_SIZE, StartApplication.getInstance().getString(R.string.meter100)).split(" ")[0]);
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            View view = View.inflate(getActivity(), R.layout.dashboard_search, null);
            final EditText searchString = view.findViewById(R.id.dashboard_search_string);
            searchString.setText(model.search_string);
            if (model.search_latitude == MathModule.notLatitude && model.search_longitude == MathModule.notLongitude) {
                if (MainActivityModel.getInstance().lastLocation != null) {
                    model.search_latitude = MainActivityModel.getInstance().lastLocation.getLatitude();
                    model.search_longitude = MainActivityModel.getInstance().lastLocation.getLongitude();
                } else {
                    model.search_latitude = 0.0000001;
                    model.search_longitude = 0.0000001;
                }
            }

            builder.setView(view);
            setSearchRadioGroup(view);
            setSearchMap(view);
            builder.setPositiveButton(android.R.string.search_go, (dialog, id) -> {
                unsetSearchMap();
                String whereString = "";
                model.search_string = searchString.getText().toString().trim();
                if (model.search_string.isEmpty()) {
                    if (model.search_radio_button.equals(getString(R.string.radio_button_none))) {
                        whereString = "";
                    }
                    if (model.search_radio_button.equals(getString(R.string.radio_button_only))) {
                        whereString = " WHERE "
                                + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " >= " + model.search_longitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " <= " + model.search_longitude_max + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " >= " + model.search_latitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " <= " + model.search_latitude_max;
                    }
                    if (model.search_radio_button.equals(getString(R.string.radio_button_or))) {
                        whereString = " WHERE "
                                + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " >= " + model.search_longitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " <= " + model.search_longitude_max + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " >= " + model.search_latitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " <= " + model.search_latitude_max;
                    }
                    if (model.search_radio_button.equals(getString(R.string.radio_button_and))) {
                        whereString = " WHERE "
                                + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " >= " + model.search_longitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " <= " + model.search_longitude_max + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " >= " + model.search_latitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " <= " + model.search_latitude_max;
                    }
                } else {
                    if (model.search_radio_button.equals(getString(R.string.radio_button_none))) {
                        whereString = " WHERE "
                                + DatabaseDriver.DatabaseContract.Devices.NAME + " LIKE '%" + model.search_string + "%'" + " OR "
                                + DatabaseDriver.DatabaseContract.Devices.ADDRESS + " LIKE '%" + model.search_string + "%'" + " OR "
                                + DatabaseDriver.DatabaseContract.Devices.CLASS + " LIKE '%" + model.search_string + "%'" + " OR "
                                + DatabaseDriver.DatabaseContract.Devices.TYPE + " LIKE '%" + model.search_string + "%'";
                    }
                    if (model.search_radio_button.equals(getString(R.string.radio_button_only))) {
                        whereString = " WHERE "
                                + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " >= " + model.search_longitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " <= " + model.search_longitude_max + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " >= " + model.search_latitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " <= " + model.search_latitude_max;
                    }
                    if (model.search_radio_button.equals(getString(R.string.radio_button_or))) {
                        whereString = " WHERE "
                                + "(" + DatabaseDriver.DatabaseContract.Devices.NAME + " LIKE '%" + model.search_string + "%'" + " OR "
                                + DatabaseDriver.DatabaseContract.Devices.ADDRESS + " LIKE '%" + model.search_string + "%'" + " OR "
                                + DatabaseDriver.DatabaseContract.Devices.CLASS + " LIKE '%" + model.search_string + "%'" + " OR "
                                + DatabaseDriver.DatabaseContract.Devices.TYPE + " LIKE '%" + model.search_string + "%') OR"
                                + "(" + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " >= " + model.search_longitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " <= " + model.search_longitude_max + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " >= " + model.search_latitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " <= " + model.search_latitude_max + ")";
                    }
                    if (model.search_radio_button.equals(getString(R.string.radio_button_and))) {
                        whereString = " WHERE "
                                + "(" + DatabaseDriver.DatabaseContract.Devices.NAME + " LIKE '%" + model.search_string + "%'" + " OR "
                                + DatabaseDriver.DatabaseContract.Devices.ADDRESS + " LIKE '%" + model.search_string + "%'" + " OR "
                                + DatabaseDriver.DatabaseContract.Devices.CLASS + " LIKE '%" + model.search_string + "%'" + " OR "
                                + DatabaseDriver.DatabaseContract.Devices.TYPE + " LIKE '%" + model.search_string + "%') AND"
                                + "(" + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " >= " + model.search_longitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LONGITUDE + " <= " + model.search_longitude_max + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " >= " + model.search_latitude_min + " AND "
                                + DatabaseDriver.DatabaseContract.Devices.LATITUDE + " <= " + model.search_latitude_max + ")";
                    }
                }
                model.query = "SELECT " + DatabaseDriver.DatabaseContract.Devices._ID + " FROM " + DatabaseDriver.DatabaseContract.Devices.TABLE_NAME + whereString + " ORDER BY " + DatabaseDriver.DatabaseContract.Devices._ID + " DESC;";
                dbDriver.cancelSelectionDevices();
                updateView();
                if (model.isSetSearchFilter()) {
                    fabSearch.setImageResource(R.drawable.ic_search_filter_on);
                } else {
                    fabSearch.setImageResource(R.drawable.ic_search_filter_off);
                }
            });
            builder.setNegativeButton(R.string.dashboard_search_reset_button, (dialog, which) -> {
                unsetSearchMap();
                model.search_string = "";
                model.search_latitude = MathModule.notLatitude;
                model.search_longitude = MathModule.notLongitude;
                model.search_radio_button = getString(R.string.radio_button_none);
                model.search_latitude_min = 0.0;
                model.search_longitude_min = 0.0;
                model.search_latitude_max = 0.0;
                model.search_longitude_max = 0.0;
                model.query = "SELECT " + DatabaseDriver.DatabaseContract.Devices._ID + " FROM " + DatabaseDriver.DatabaseContract.Devices.TABLE_NAME + " ORDER BY " + DatabaseDriver.DatabaseContract.Devices._ID + " DESC;";
                dbDriver.cancelSelectionDevices();
                updateView();
                if (model.isSetSearchFilter()) {
                    fabSearch.setImageResource(R.drawable.ic_search_filter_on);
                } else {
                    fabSearch.setImageResource(R.drawable.ic_search_filter_off);
                }
            });
            builder.setTitle(R.string.dashboard_search_dialog_title);
            builder.create();
            alertDialog = builder.show();
        }
    };

    @SuppressLint("NonConstantResourceId")
    void setSearchRadioGroup(View view) {
        searchRadioGroup = view.findViewById(R.id.dashboard_radio_group);
        if (model.search_radio_button.equals(getString(R.string.radio_button_none))) {
            ((RadioButton) view.findViewById(R.id.dashboard_radio_button_none)).setChecked(true);
        }
        if (model.search_radio_button.equals(getString(R.string.radio_button_only))) {
            ((RadioButton) view.findViewById(R.id.dashboard_radio_button_only)).setChecked(true);
        }
        if (model.search_radio_button.equals(getString(R.string.radio_button_or))) {
            ((RadioButton) view.findViewById(R.id.dashboard_radio_button_or)).setChecked(true);
        }
        if (model.search_radio_button.equals(getString(R.string.radio_button_and))) {
            ((RadioButton) view.findViewById(R.id.dashboard_radio_button_and)).setChecked(true);
        }
        searchRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.dashboard_radio_button_none:
                    model.search_radio_button = getString(R.string.radio_button_none);
                    break;
                case R.id.dashboard_radio_button_only:
                    model.search_radio_button = getString(R.string.radio_button_only);
                    break;
                case R.id.dashboard_radio_button_or:
                    model.search_radio_button = getString(R.string.radio_button_or);
                    break;
                case R.id.dashboard_radio_button_and:
                    model.search_radio_button = getString(R.string.radio_button_and);
                    break;
                default:
                case -1:
                    break;
            }
        });
    }

    void setSearchMap(View view) {
        mapViewSearch = view.findViewById(R.id.dashboard_search_mapView);
        mapViewSearch.onCreate(null);
        mapViewSearch.onResume();
        mapViewSearch.getMapAsync(this);
    }

    void unsetSearchMap() {
        if (mapViewSearch != null) {
            mapViewSearch.setVisibility(View.INVISIBLE);
            mapViewSearch.onPause();
            mapViewSearch.onDestroy();
        }
        searchArea = null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapSearch = googleMap;
        mapSearch.getUiSettings().setMapToolbarEnabled(false);
        mapSearch.getUiSettings().setMyLocationButtonEnabled(false);
        mapSearch.getUiSettings().setZoomControlsEnabled(false);
        LatLng center_point = new LatLng(model.search_latitude, model.search_longitude);
        mapSearch.moveCamera(CameraUpdateFactory.newLatLng(center_point));
        createSearchAreaCoordinates();
        PolylineOptions searchAreaOptions = new PolylineOptions()
                .add(new LatLng(model.search_latitude_min, model.search_longitude_min))
                .add(new LatLng(model.search_latitude_min, model.search_longitude_max))
                .add(new LatLng(model.search_latitude_max, model.search_longitude_max))
                .add(new LatLng(model.search_latitude_max, model.search_longitude_min))
                .add(new LatLng(model.search_latitude_min, model.search_longitude_min))
                .width(2)
                .color(Color.RED)
                .clickable(false);
        searchArea = mapSearch.addPolyline(searchAreaOptions);
        mapSearch.setOnMapClickListener((latLng) -> {
            model.search_latitude = latLng.latitude;
            model.search_longitude = latLng.longitude;
            mapSearch.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            createSearchAreaCoordinates();
            PolylineOptions newSearchAreaOptions = new PolylineOptions()
                    .add(new LatLng(model.search_latitude_min, model.search_longitude_min))
                    .add(new LatLng(model.search_latitude_min, model.search_longitude_max))
                    .add(new LatLng(model.search_latitude_max, model.search_longitude_max))
                    .add(new LatLng(model.search_latitude_max, model.search_longitude_min))
                    .add(new LatLng(model.search_latitude_min, model.search_longitude_min))
                    .width(2)
                    .color(Color.RED)
                    .clickable(false);
            if (searchArea != null) searchArea.remove();
            searchArea = mapSearch.addPolyline(newSearchAreaOptions);
        });
    }

    void createSearchAreaCoordinates() {
        Location location;
        location = MathModule.createLocation(model.search_latitude, model.search_longitude, Math.PI, searchAreaSize / 2.0);
        model.search_latitude_min = location.getLatitude();
        location = MathModule.createLocation(model.search_latitude, model.search_longitude, 0, searchAreaSize / 2.0);
        model.search_latitude_max = location.getLatitude();
        location = MathModule.createLocation(model.search_latitude, model.search_longitude, Math.PI / 2.0, searchAreaSize / 2.0);
        model.search_longitude_max = location.getLongitude();
        location = MathModule.createLocation(model.search_latitude, model.search_longitude, 3.0 * Math.PI / 2.0, searchAreaSize / 2.0);
        model.search_longitude_min = location.getLongitude();
    }

    private class FragmentMenuProvider implements MenuProvider {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setTitle(getResources().getString(R.string.app_name_short) + " - " + getResources().getString(R.string.action_dashboard));
            }
            menuInflater.inflate(R.menu.menu_dashboard, menu);
            boolean isSelectedDevices = dbDriver.areSelectedDevices();
            menu.findItem(R.id.dashboard_delete_selected_devices).setVisible(isSelectedDevices);
            menu.findItem(R.id.dashboard_cancel_check_devices).setVisible(isSelectedDevices);
        }

        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            String selectedAction = (String) item.getTitle();
            if (selectedAction != null) {
                if (selectedAction.equals(getString(R.string.dashboard_cancel_selection))) {
                    dbDriver.cancelSelectionDevices();
                    updateView();
                    return true;
                }
                if (selectedAction.equals(getString(R.string.dashboard_delete_selected_devices))) {
                    dbDriver.deleteSelectionDevices();
                    return true;
                }
            }
            return true;
        }
    }

}

package ru.vukit.btm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.MapView;

import static ru.vukit.btm.SettingsFragment.KEY_LOCATION_PROVIDER;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import ru.vukit.btm.bluetooth.BluetoothDriver;

@Keep
public class MainActivity extends AppCompatActivity implements LocationListener, RunFragment, SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences sharedPreferences;
    final MainActivityModel model = MainActivityModel.getInstance();
    final BluetoothDriver btDriver = BluetoothDriver.getInstance();
    LocationManager mLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setSupportActionBar(findViewById(R.id.toolbar));
        Thread mapViewInit = new Thread(() -> {
            try {
                MapView mv = new MapView(getApplicationContext());
                mv.onCreate(null);
                mv.onPause();
                mv.onDestroy();
            } catch (Exception ignored) {
            }
        });
        mapViewInit.setContextClassLoader(getClass().getClassLoader());
        mapViewInit.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        model.connectController(this);
        if (model.selectedAction == null) {
            model.selectedAction = sharedPreferences.getString(SettingsFragment.KEY_STARTUP_SCREEN, getString(R.string.action_dashboard));
        }
        selectAction(model.selectedAction);
        if (!model.isCheckedPermission) {
            List<String> allPermissionsRequests = new ArrayList<>();
            List<String> permissionsRequests;
            model.permissionInternet = (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED);
            if (!model.permissionInternet) {
                allPermissionsRequests.add(Manifest.permission.INTERNET);
            }
            permissionsRequests = model.getLocationPermissionsRequests();
            if (permissionsRequests.isEmpty()) {
                model.permissionLocation = true;
            } else {
                allPermissionsRequests.addAll(permissionsRequests);
            }
            permissionsRequests = model.getBluetoothPermissionsRequests();
            if (permissionsRequests.isEmpty()) {
                model.permissionBluetooth = true;
            } else {
                allPermissionsRequests.addAll(permissionsRequests);
            }
            if (!allPermissionsRequests.isEmpty()) {
                ActivityCompat.requestPermissions(this, allPermissionsRequests.toArray(new String[0]), MainActivityModel.PERMISSIONS_REQUEST_ALL);
            }
            model.isCheckedPermission = true;
        }
        btDriver.registerBroadcastReceivers();
        setupLocationManager(sharedPreferences.getString(KEY_LOCATION_PROVIDER, LocationManager.PASSIVE_PROVIDER));
    }

    @Override
    protected void onStop() {
        if (!isChangingConfigurations() && sharedPreferences.getBoolean(SettingsFragment.KEY_BLUETOOTH_TURN_OFF, false)) {
            btDriver.Disable();
        }
        btDriver.unregisterBroadcastReceivers();
        model.disconnectController();
        if (mLocationManager != null) mLocationManager.removeUpdates(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MainActivityModel.PERMISSIONS_REQUEST_ALL) {
            model.permissionBluetooth = true;
            model.permissionLocation = false;
            for (int i = 0; i < permissions.length; i++) {
                switch (permissions[i]) {
                    case Manifest.permission.INTERNET:
                        model.permissionInternet = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                        break;
                    case Manifest.permission.BLUETOOTH:
                    case Manifest.permission.BLUETOOTH_ADMIN:
                    case Manifest.permission.BLUETOOTH_CONNECT:
                    case Manifest.permission.BLUETOOTH_SCAN:
                        if (model.permissionBluetooth && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            model.permissionBluetooth = false;
                        }
                        break;
                    case Manifest.permission.ACCESS_FINE_LOCATION:
                    case Manifest.permission.ACCESS_COARSE_LOCATION:
                        if (!model.permissionLocation && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            model.permissionLocation = true;
                        }
                        break;
                }
            }
        }
        setupLocationManager(sharedPreferences.getString(KEY_LOCATION_PROVIDER, LocationManager.PASSIVE_PROVIDER));
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        model.selectedAction = (String) item.getTitle();
        return selectAction(model.selectedAction) || super.onOptionsItemSelected(item);
    }

    private boolean selectAction(String selectedAction) {
        if (selectedAction != null) {
            if (selectedAction.equals(getString(R.string.action_search))) {
                model.childFragment = "";
                model.parentFragment = "";
                addFragment(new SearchFragment());
                return true;
            }
            if (selectedAction.equals(getString(R.string.action_settings))) {
                model.childFragment = "";
                model.parentFragment = "";
                addFragment(new SettingsFragment());
                return true;
            }
            if (selectedAction.equals(getString(R.string.action_dashboard))) {
                model.childFragment = "";
                model.parentFragment = "";
                addFragment(new DashBoardFragment());
                return true;
            }
            if (selectedAction.equals(getString(R.string.action_about))) {
                model.childFragment = "";
                model.parentFragment = "";
                addFragment(new AboutFragment());
                return true;
            }
            if (selectedAction.equals(getString(R.string.action_device_details))) {
                addFragment(new DeviceDetailsFragment());
                return true;
            }
        }
        return false;
    }

    private void addFragment(Fragment new_fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_container, new_fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void Child(String parent, String child) {
        model.childFragment = child;
        model.parentFragment = parent;
        model.selectedAction = child;
        selectAction(model.selectedAction);
    }

    @Override
    public void Parent(String parent, String child) {
        model.childFragment = "";
        model.parentFragment = "";
        model.selectedAction = parent;
        selectAction(model.selectedAction);
    }

    @Override
    public void onBackPressed() {
        if (!model.parentFragment.isEmpty() && !model.childFragment.isEmpty()) {
            if (model.childFragment.equals(getString(R.string.action_device_details))) {
                DeviceDetailsModel.getInstance().stopGetGattServices(false);
                btDriver.rfCommDisconnect(false);
                Parent(model.parentFragment, model.childFragment);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        model.lastLocation = location;
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    @SuppressLint("MissingPermission")
    private void setupLocationManager(String locationProvider) {
        if (mLocationManager != null) mLocationManager.removeUpdates(this);
        if (model.permissionLocation) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (mLocationManager != null) {
                try {
                    mLocationManager.requestLocationUpdates(locationProvider, 1000, 1, this);
                    model.lastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } catch (SecurityException ex) {
                    model.lastLocation = null;
                }
            }
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_LOCATION_PROVIDER)) {
            setupLocationManager(sharedPreferences.getString(KEY_LOCATION_PROVIDER, LocationManager.PASSIVE_PROVIDER));
        }
    }
}

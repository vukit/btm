package ru.vukit.btm.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.provider.BaseColumns;

import androidx.annotation.Keep;

import java.util.HashMap;
import java.util.Objects;
import java.util.Observable;

import ru.vukit.btm.R;
import ru.vukit.btm.StartApplication;

@Keep
@SuppressWarnings({"deprecation"})
public
class DatabaseDriver extends Observable {

    private static DatabaseDriver INSTANCE = null;

    public static synchronized DatabaseDriver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseDriver();
        }
        return (INSTANCE);
    }

    private final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(StartApplication.getInstance().getApplicationContext());
    private final SQLiteDatabase db = databaseHelper.getWritableDatabase();

    public Cursor rawQuery(String query) {
        return db.rawQuery(query, null);
    }

    public boolean isDeviceExist(String mac_address) {
        boolean result = false;
        Cursor cursor = db.query(
                DatabaseContract.Devices.TABLE_NAME,
                null,
                DatabaseContract.Devices.ADDRESS + " = ?",
                new String[]{mac_address},
                null,
                null,
                null
        );
        if (cursor.getCount() != 0) result = true;
        cursor.close();
        return result;
    }

    public void selectDevice(long deviceId, int isSelected) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Devices.SELECTED, isSelected);
        db.update(DatabaseContract.Devices.TABLE_NAME, values, DatabaseContract.Devices._ID + " = ?", new String[]{String.valueOf(deviceId)});
    }

    public boolean areSelectedDevices() {
        boolean result = false;
        Cursor cursor = db.query(
                DatabaseContract.Devices.TABLE_NAME,
                null,
                DatabaseContract.Devices.SELECTED + " = ?",
                new String[]{"1"},
                null,
                null,
                null
        );
        if (cursor.getCount() != 0) result = true;
        cursor.close();
        return result;
    }

    public void cancelSelectionDevices() {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Devices.SELECTED, 0);
        db.update(DatabaseContract.Devices.TABLE_NAME, values, DatabaseContract.Devices.SELECTED + " = ?", new String[]{String.valueOf(1)});
    }

    public void updateDeviceName(long deviceId, String device_name) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Devices.NAME, device_name);
        db.update(DatabaseContract.Devices.TABLE_NAME, values, DatabaseContract.Devices._ID + " = ?", new String[]{String.valueOf(deviceId)});
    }

    public void updateDeviceSDPServices(String address, String serviceUUIDs) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Devices.SERVICE_UUIDs, serviceUUIDs);
        db.update(DatabaseContract.Devices.TABLE_NAME, values, DatabaseContract.Devices.ADDRESS + " = ?", new String[]{address});
    }

    public void updateDeviceManufacturer(long deviceId, String manufacturer) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Devices.MANUFACTURER, manufacturer);
        db.update(DatabaseContract.Devices.TABLE_NAME, values, DatabaseContract.Devices._ID + " = ?", new String[]{String.valueOf(deviceId)});
    }

    public void updateDeviceLocation(long deviceId, Double longitude, Double latitude) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Devices.LONGITUDE, longitude);
        values.put(DatabaseContract.Devices.LATITUDE, latitude);
        db.update(DatabaseContract.Devices.TABLE_NAME, values, DatabaseContract.Devices._ID + " = ?", new String[]{String.valueOf(deviceId)});
    }

    public void insertDevice(HashMap<String, String> device) {
        String scan_record = "";
        if (device.containsKey("scanRecord"))
            scan_record = Objects.requireNonNull(device.get("scanRecord")).replaceAll(" ", "").replaceAll("\n", "");
        String serviceUUIDs = "";
        if (device.containsKey("serviceUUIDs")) serviceUUIDs = device.get("serviceUUIDs");
        new InsertDevice().execute(device.get("name"), device.get("address"), device.get("class"), device.get("type"), device.get("latitude"), device.get("longitude"), scan_record, serviceUUIDs, device.get("manufacturer"));
    }

    private static class InsertDevice extends AsyncTask<String, Void, Integer> {

        protected void onPreExecute() {
            super.onPreExecute();
            DatabaseDriver.getInstance().setChanged();
            DatabaseDriver.getInstance().notifyObservers(new DatabaseMessage(DatabaseMessage.TYPE_BEGIN_TRANSACTION, StartApplication.getInstance().getString(R.string.action_adding_device_to_dashboard)));
        }

        @Override
        protected Integer doInBackground(String... params) {
            int result;
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.Devices.NAME, params[0]);
            values.put(DatabaseContract.Devices.ADDRESS, params[1]);
            values.put(DatabaseContract.Devices.CLASS, params[2]);
            values.put(DatabaseContract.Devices.TYPE, params[3]);
            values.put(DatabaseContract.Devices.LATITUDE, params[4]);
            values.put(DatabaseContract.Devices.LONGITUDE, params[5]);
            values.put(DatabaseContract.Devices.SCAN_RECORD, params[6]);
            values.put(DatabaseContract.Devices.SERVICE_UUIDs, params[7]);
            values.put(DatabaseContract.Devices.MANUFACTURER, params[8]);
            Cursor cursor = DatabaseDriver.getInstance().db.query(
                    DatabaseContract.Devices.TABLE_NAME,
                    null,
                    DatabaseContract.Devices.ADDRESS + " = ?",
                    new String[]{params[1]},
                    null,
                    null,
                    null
            );

            if (cursor.getCount() == 0) {
                DatabaseDriver.getInstance().db.insert(DatabaseContract.Devices.TABLE_NAME, null, values);
                result = 0;
            } else {
                DatabaseDriver.getInstance().db.update(DatabaseContract.Devices.TABLE_NAME, values, DatabaseContract.Devices.ADDRESS + " = ?", new String[]{params[1]});
                result = 1;
            }
            cursor.close();
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            switch (result) {
                case 0:
                    DatabaseDriver.getInstance().setChanged();
                    DatabaseDriver.getInstance().notifyObservers(new DatabaseMessage(DatabaseMessage.TYPE_NEW_DATA, StartApplication.getInstance().getString(R.string.action_added_device_to_dashboard)));
                    break;
                case 1:
                    DatabaseDriver.getInstance().setChanged();
                    DatabaseDriver.getInstance().notifyObservers(new DatabaseMessage(DatabaseMessage.TYPE_NEW_DATA, StartApplication.getInstance().getString(R.string.action_updated_device_to_dashboard)));
                    break;
            }
        }
    }

    public void deleteSelectionDevices() {
        new DeleteSelectionDevices().execute();
    }

    private static class DeleteSelectionDevices extends AsyncTask<String, Void, Integer> {

        protected void onPreExecute() {
            super.onPreExecute();
            DatabaseDriver.getInstance().setChanged();
            DatabaseDriver.getInstance().notifyObservers(new DatabaseMessage(DatabaseMessage.TYPE_BEGIN_TRANSACTION, StartApplication.getInstance().getString(R.string.action_delete_device_on_dashboard)));
        }

        @Override
        protected Integer doInBackground(String... strings) {
            return DatabaseDriver.getInstance().db.delete(DatabaseContract.Devices.TABLE_NAME, DatabaseContract.Devices.SELECTED + " = ?", new String[]{String.valueOf(1)});
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result > 1) {
                DatabaseDriver.getInstance().setChanged();
                DatabaseDriver.getInstance().notifyObservers(new DatabaseMessage(DatabaseMessage.TYPE_NEW_DATA, StartApplication.getInstance().getString(R.string.action_removed_device_records_to_dashboard)));
            } else {
                DatabaseDriver.getInstance().setChanged();
                DatabaseDriver.getInstance().notifyObservers(new DatabaseMessage(DatabaseMessage.TYPE_NEW_DATA, StartApplication.getInstance().getString(R.string.action_removed_device_record_to_dashboard)));
            }
        }
    }

    public static final class DatabaseContract {

        private DatabaseContract() {
        }

        static final int DATABASE_VERSION = 1;
        static final String DATABASE_NAME = "BTM";
        static final String[] SQL_CREATE_TABLE_ARRAY = {Devices.CREATE_TABLE};
        static final String[] SQL_UPGRADE_TABLE_ARRAY = {Devices.UPGRADE_TABLE};

        public static final class Devices implements BaseColumns {
            private Devices() {
            }

            public final static String TABLE_NAME = "devices";
            public final static String NAME = "name";
            public final static String ADDRESS = "address";
            public final static String CLASS = "class";
            public final static String TYPE = "type";
            public final static String LATITUDE = "latitude";
            public final static String LONGITUDE = "longitude";
            public final static String SCAN_RECORD = "scan_record";
            public final static String SERVICE_UUIDs = "service_uuids";
            public final static String MANUFACTURER = "manufacturer";
            public final static String SELECTED = "selected";
            public final static String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                    + _ID + " INTEGER PRIMARY KEY,"
                    + NAME + " TEXT,"
                    + ADDRESS + " TEXT,"
                    + CLASS + " TEXT,"
                    + TYPE + " TEXT,"
                    + LATITUDE + " REAL,"
                    + LONGITUDE + " REAL,"
                    + SCAN_RECORD + " TEXT, "
                    + SERVICE_UUIDs + " TEXT, "
                    + MANUFACTURER + " TEXT, "
                    + SELECTED + " INT DEFAULT 0"
                    + ");";
            public final static String UPGRADE_TABLE = "";
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static DatabaseHelper DatabaseHelperHolder = null;

        public static DatabaseHelper getInstance(Context context) {
            if (DatabaseHelperHolder == null) DatabaseHelperHolder = new DatabaseHelper(context);
            return DatabaseHelperHolder;
        }

        private DatabaseHelper(Context context) {
            super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            for (String table : DatabaseContract.SQL_CREATE_TABLE_ARRAY) db.execSQL(table);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            for (String table : DatabaseContract.SQL_UPGRADE_TABLE_ARRAY) db.execSQL(table);
        }
    }
}

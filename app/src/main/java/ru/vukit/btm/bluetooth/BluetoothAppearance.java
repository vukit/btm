package ru.vukit.btm.bluetooth;

import android.content.res.Resources;
import android.util.SparseArray;

import ru.vukit.btm.R;
import ru.vukit.btm.StartApplication;

public class BluetoothAppearance {

    static private final Resources resources = StartApplication.getInstance().getResources();

    public static String getAppearance(int appearanceID) {
        if (appearance.indexOfKey(appearanceID) < 0) return resources.getString(R.string.unknown);
        else return appearance.get(appearanceID);
    }

    private static final SparseArray<String> appearance = new SparseArray<>();

    static {
        appearance.put(64, "Generic Phone");
        appearance.put(128, "Generic Computer");
        appearance.put(192, "Generic Watch");
        appearance.put(193, "Sports Watch");
        appearance.put(256, "Generic Clock");
        appearance.put(320, "Generic Display");
        appearance.put(384, "Generic Remote Control");
        appearance.put(448, "Generic Eye-glasses");
        appearance.put(512, "Generic Tag");
        appearance.put(576, "Generic Keyring");
        appearance.put(640, "Generic Media Player");
        appearance.put(704, "Generic Barcode Scanner");
        appearance.put(768, "Generic Thermometer");
        appearance.put(769, "Thermometer (Ear)");
        appearance.put(832, "Generic Heart rate Sensor");
        appearance.put(833, "Heart Rate Belt");
        appearance.put(896, "Generic Blood Pressure");
        appearance.put(897, "Blood Pressure (Arm)");
        appearance.put(898, "Blood Pressure (Wrist)");
        appearance.put(960, "Human Interface Device");
        appearance.put(961, "Keyboard");
        appearance.put(962, "Mouse");
        appearance.put(963, "Joystick");
        appearance.put(964, "Game pad");
        appearance.put(965, "Digitizer Tablet");
        appearance.put(966, "Card Reader");
        appearance.put(967, "Digital Pen");
        appearance.put(968, "Barcode Scanner");
        appearance.put(1024, "Generic Glucose Meter");
        appearance.put(1088, "Running Walking Sensor");
        appearance.put(1089, "In-Shoe Running Walking Sensor");
        appearance.put(1090, "On-Shoe Running Walking Sensor");
        appearance.put(1091, "On-Hip Running Walking Sensor");
        appearance.put(1152, "Cycling");
        appearance.put(1153, "Cycling (Cycling Computer)");
        appearance.put(1154, "Cycling (Speed Sensor)");
        appearance.put(1155, "Cycling (Cadence Sensor)");
        appearance.put(1156, "Cycling (Power Sensor)");
        appearance.put(1157, "Cycling (Speed and Cadence Sensor)");
        appearance.put(3136, "Pulse Oximeter");
        appearance.put(3137, "Fingertip");
        appearance.put(3138, "Wrist Worn");
        appearance.put(3200, "Weight Scale");
        appearance.put(5184, "Outdoor Sports Activity");
        appearance.put(5185, "Location Display Device");
        appearance.put(5186, "Location and Navigation Display Device");
        appearance.put(5187, "Location Pod");
        appearance.put(5188, "Location and Navigation Pod");
    }
}

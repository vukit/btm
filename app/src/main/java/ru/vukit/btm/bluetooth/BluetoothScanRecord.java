package ru.vukit.btm.bluetooth;

import android.content.res.Resources;
import android.util.SparseArray;

import java.util.Arrays;
import java.util.HashMap;

import ru.vukit.btm.R;
import ru.vukit.btm.StartApplication;

public class BluetoothScanRecord {

    public static final SparseArray<String> dataTypeValueName = new SparseArray<>();

    static {
        dataTypeValueName.put(0x01, "Flags");
        dataTypeValueName.put(0x02, "Incomplete List of 16-bit Service Class UUIDs");
        dataTypeValueName.put(0x03, "Complete List of 16-bit Service Class UUIDs");
        dataTypeValueName.put(0x04, "Incomplete List of 32-bit Service Class UUIDs");
        dataTypeValueName.put(0x05, "Complete List of 32-bit Service Class UUIDs");
        dataTypeValueName.put(0x06, "Incomplete List of 128-bit Service Class UUIDs");
        dataTypeValueName.put(0x07, "Complete List of 128-bit Service Class UUIDs");
        dataTypeValueName.put(0x08, "Shortened Local Name");
        dataTypeValueName.put(0x09, "Complete Local Name");
        dataTypeValueName.put(0x0A, "Tx Power Level");
        dataTypeValueName.put(0x0D, "Class of Device");
        dataTypeValueName.put(0x0E, "Simple Pairing Hash C-192");
        dataTypeValueName.put(0x0F, "Simple Pairing Randomizer R");
        dataTypeValueName.put(0x10, "Security Manager TK Value");
        dataTypeValueName.put(0x11, "Security Manager Out of Band Flags");
        dataTypeValueName.put(0x12, "Slave Connection Interval Range");
        dataTypeValueName.put(0x14, "List of 16-bit Service Solicitation UUIDs");
        dataTypeValueName.put(0x1F, "List of 32-bit Service Solicitation UUIDs");
        dataTypeValueName.put(0x15, "List of 128-bit Service Solicitation UUIDs");
        dataTypeValueName.put(0x16, "Service Data - 16-bit UUID");
        dataTypeValueName.put(0x20, "Service Data - 32-bit UUID");
        dataTypeValueName.put(0x21, "Service Data - 128-bit UUID");
        dataTypeValueName.put(0x22, "LE Secure Connections Confirmation Value");
        dataTypeValueName.put(0x23, "LE Secure Connections Random Value");
        dataTypeValueName.put(0x24, "URI");
        dataTypeValueName.put(0x25, "Indoor Positioning");
        dataTypeValueName.put(0x26, "Transport Discovery Data");
        dataTypeValueName.put(0x17, "Public Target Address");
        dataTypeValueName.put(0x18, "Random Target Address");
        dataTypeValueName.put(0x19, "Appearance");
        dataTypeValueName.put(0x1A, "Advertising Interval");
        dataTypeValueName.put(0x1B, "LE Bluetooth Device Address");
        dataTypeValueName.put(0x1C, "LE Role");
        dataTypeValueName.put(0x1D, "Simple Pairing Hash C-256");
        dataTypeValueName.put(0x1E, "Simple Pairing Randomizer R-256");
        dataTypeValueName.put(0x3D, "3D Information Data");
        dataTypeValueName.put(0xFF, "Manufacturer");
    }

    private static final Resources resources = StartApplication.getInstance().getResources();

    private static final String postfixBluetoothBaseUUID = "-0000-1000-8000-00805F9B34FB";

    public static HashMap<String, String> decode(String stringScanRecord) {
        stringScanRecord = stringScanRecord.replaceAll(" ", "").replaceAll("\n", "");
        int length = stringScanRecord.length();
        byte[] scanRecord = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            scanRecord[i / 2] = (byte) ((Character.digit(stringScanRecord.charAt(i), 16) << 4) + Character.digit(stringScanRecord.charAt(i + 1), 16));
        }
        return decode(scanRecord);
    }

    public static HashMap<String, String> decode(byte[] scanRecord) {
        HashMap<String, String> scanRecordMap = new HashMap<>();
        if (scanRecord.length == 0) return scanRecordMap;
        int state = 1, i = 0;
        int dataLength = 0;
        int dataType = 0;
        String dataValue;
        while (state != 0) { // Используем автоматный подход для разбора scanRecord
            try {
                switch (state) {
                    case 1: // Определяем длину данных
                        dataLength = scanRecord[i++] - 1;
                        if (dataLength == 0 || i == scanRecord.length) state = 0;
                        else state = 2;
                        break;
                    case 2: // Определяем тип данных
                        dataType = scanRecord[i++] & 0xFF;
                        state = 3;
                        break;
                    case 3: // Декодируем данные
                        switch (dataType) {
                            case 0x01:
                                if (dataLength == 1) {
                                    String descriptionFlags = "";
                                    if ((scanRecord[i] & 0x01) != 0)
                                        descriptionFlags += "\nLE Limited Discoverable Mode";
                                    if ((scanRecord[i] & 0x02) != 0)
                                        descriptionFlags += "\nLE General Discoverable Mode";
                                    if ((scanRecord[i] & 0x04) != 0)
                                        descriptionFlags += "\nBR/EDR Not Supported";
                                    if ((scanRecord[i] & 0x08) != 0)
                                        descriptionFlags += "\nSimultaneous LE and BR/EDR (Controller)";
                                    if ((scanRecord[i] & 0x10) != 0)
                                        descriptionFlags += "\nSimultaneous LE and BR/EDR (Host)";
                                    if (!descriptionFlags.isEmpty())
                                        scanRecordMap.put(dataTypeValueName.get(dataType), descriptionFlags);
                                } else {
                                    dataValue = bytesHexString(scanRecord, i, dataLength);
                                    scanRecordMap.put(dataTypeValueName.get(dataType), dataValue);
                                }
                                state = 1;
                                break;
                            case 0x02:
                            case 0x03:
                            case 0x14:
                                scanRecordMap.put(dataTypeValueName.get(dataType), getService16to128bitUUIDs(scanRecord, i, dataLength));
                                state = 1;
                                break;
                            case 0x04:
                            case 0x05:
                            case 0x1F:
                                scanRecordMap.put(dataTypeValueName.get(dataType), getService32to128bitUUIDs(scanRecord, i, dataLength));
                                state = 1;
                                break;
                            case 0x06:
                            case 0x07:
                            case 0x15:
                                scanRecordMap.put(dataTypeValueName.get(dataType), getService128bitUUIDs(scanRecord, i, dataLength));
                                state = 1;
                                break;
                            case 0x08:
                            case 0x09:
                                scanRecordMap.put(dataTypeValueName.get(dataType), new String(Arrays.copyOfRange(scanRecord, i, i + dataLength)));
                                state = 1;
                                break;
                            case 0x0A:
                                scanRecordMap.put(dataTypeValueName.get(dataType), scanRecord[i] + " " + resources.getString(R.string.dBm));
                                state = 1;
                                break;
                            case 0x0D:
                            case 0x0E:
                            case 0x0F:
                            case 0x10:
                            case 0x11:
                            case 0x12:
                            case 0x22:
                            case 0x23:
                            case 0x24:
                            case 0x25:
                            case 0x26:
                            case 0x17:
                            case 0x18:
                            case 0x1A:
                            case 0x1B:
                            case 0x1C:
                            case 0x1D:
                            case 0x1E:
                            case 0x3D:
                                dataValue = bytesHexString(scanRecord, i, dataLength);
                                scanRecordMap.put(dataTypeValueName.get(dataType), dataValue);
                                state = 1;
                                break;
                            case 0x16:
                                scanRecordMap.put(dataTypeValueName.get(dataType), getServiceData16bitUUID(scanRecord, i, dataLength));
                                state = 1;
                                break;
                            case 0x20:
                                scanRecordMap.put(dataTypeValueName.get(dataType), getServiceData32bitUUID(scanRecord, i, dataLength));
                                state = 1;
                                break;
                            case 0x21:
                                scanRecordMap.put(dataTypeValueName.get(dataType), getServiceData128bitUUID(scanRecord, i, dataLength));
                                state = 1;
                                break;
                            case 0x19:
                                dataValue = "\n" + BluetoothAppearance.getAppearance(((scanRecord[i + 1] << 8) + scanRecord[i]));
                                scanRecordMap.put(dataTypeValueName.get(dataType), dataValue);
                                state = 1;
                                break;
                            case 0xFF:
                                dataValue = "\n" + BluetoothManufacturers.getName(((scanRecord[i + 1] << 8) + scanRecord[i]));
                                scanRecordMap.put(dataTypeValueName.get(dataType), dataValue);
                                state = 1;
                                break;
                            default:
                                state = 1;
                                break;
                        }
                        i += dataLength;
                        break;
                }
            } catch (IndexOutOfBoundsException ignored) {
                return scanRecordMap;
            }
        }
        return scanRecordMap;
    }

    private static String bytesHexString(byte[] byteArray, int from, int length) {
        StringBuilder result = new StringBuilder("\n");
        int byteCounter = 0;
        for (byte b : Arrays.copyOfRange(byteArray, from, from + length)) {
            result.append(String.format("%02x ", b));
            if (++byteCounter == 10) {
                byteCounter = 0;
                result.append("\n");
            }
        }
        return result.toString();
    }

    private static String getService128bitUUIDs(byte[] byteArray, int from, int length) {
        StringBuilder result = new StringBuilder("\n");
        int byteCounter = 0;
        for (int i = from + length; i >= from; i--) {
            result.append(String.format("%02x", byteArray[i]));
            if (++byteCounter == 16) {
                byteCounter = 0;
                if (i != from + 1) {
                    result.append("\n");
                }
                continue;
            }
            if (byteCounter == 4 || byteCounter == 6 || byteCounter == 8 || byteCounter == 10) {
                result.append("-");
            }
        }
        return result.toString();
    }

    private static String getService32to128bitUUIDs(byte[] byteArray, int from, int length) {
        StringBuilder result = new StringBuilder("\n");
        StringBuilder uuid32 = new StringBuilder();
        int byteCounter = 0;
        for (int i = from + length; i >= from; i--) {
            uuid32.append(String.format("%02x", byteArray[i]));
            if (++byteCounter == 4) {
                byteCounter = 0;
                result.append(uuid32);
                result.append(postfixBluetoothBaseUUID);
                uuid32 = new StringBuilder();
                if (i != from + 1) result.append("\n");
            }
        }
        return result.toString();
    }

    private static String getService16to128bitUUIDs(byte[] byteArray, int from, int length) {
        StringBuilder result = new StringBuilder("\n");
        StringBuilder uuid16 = new StringBuilder();
        int byteCounter = 0;
        for (int i = from + length; i >= from; i--) {
            uuid16.append(String.format("%02x", byteArray[i]));
            if (++byteCounter == 2) {
                byteCounter = 0;
                result.append("0000");
                result.append(uuid16);
                result.append(postfixBluetoothBaseUUID);
                uuid16 = new StringBuilder();
                if (i != from + 1) result.append("\n");
            }
        }
        return result.toString();
    }

    private static String getServiceData16bitUUID(byte[] byteArray, int from, int length) {
        StringBuilder result = new StringBuilder("\n");
        for (int i = from + 1; i >= from; i--) result.append(String.format("%02x", byteArray[i]));
        if (length > 2) {
            result.append(" (");
            for (byte b : Arrays.copyOfRange(byteArray, from + 2, from + length))
                result.append(String.format("%02x ", b));
            result.append(")");
        }
        return result.toString();
    }

    private static String getServiceData32bitUUID(byte[] byteArray, int from, int length) {
        StringBuilder result = new StringBuilder("\n");
        for (int i = from + 3; i >= from; i--) result.append(String.format("%02x", byteArray[i]));
        if (length > 4) {
            result.append(" (");
            for (byte b : Arrays.copyOfRange(byteArray, from + 4, from + length))
                result.append(String.format("%02x ", b));
            result.append(")");
        }
        return result.toString();
    }

    private static String getServiceData128bitUUID(byte[] byteArray, int from, int length) {
        StringBuilder result = new StringBuilder("\n");
        for (int i = from + 15; i >= from; i--) result.append(String.format("%02x", byteArray[i]));
        if (length > 16) {
            result.append(" (");
            for (byte b : Arrays.copyOfRange(byteArray, from + 16, from + length))
                result.append(String.format("%02x ", b));
            result.append(")");
        }
        return result.toString();
    }

}

package org.appspot.apprtc.profiler;

import static java.lang.Boolean.TRUE;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HardwarePropertiesManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthNr;
import android.telephony.TelephonyManager;
import android.util.Log;


import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.appspot.apprtc.HudFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.appspot.apprtc.R;

public class ProfilingService extends Service {

    int value = 0;
    private Timer timer;

    Process p = null;
    String foreground_pid = null;
    String[] process_info;
    String[][] foreground_thread_info;
    boolean csv_save = TRUE;
    File thread_csv_file = null;
    String battery_current_avg = null;
    String battery_voltage_now = null;
    String battery_power_avg = null;
    Profiler profiler = new Profiler();

    private Handler handler = new Handler();
    private BatteryManager batteryManager;

    private PowerManager powerManager;
    private HardwarePropertiesManager hardwarePropertiesManager;
    private Runnable runnableCode;
    private BroadcastReceiver batteryInfoReceiver;

    // Variables to be observed
    Intent intent;

    int catCounter = 0;
    int cpuLittle;
    int cpuBig1;
    int cpuBig2;

    float skinTemp;
    float cpuTemp;
    float skinState;

    float batteryPct = 0;
    float batteryVoltage = 0;
    float batteryCurrent = 0;
    int batteryPower = 0;

    // LTE, NR info
    int lte_cellId = 0;
    int lte_bw = 0;
    int lte_rsrp = 0;
    long nr_cellId = 0;
    int nr_rsrp = 0;
    String nr_band;

    long referenceTime = 0;

    File myFile;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "ENDUREChannel";

    String header = "Time, cpu_little, cpu_big1, cpu_big2, battery_cap, battery_cur, battery_power, temp_modem, temp_cpu, thermal_throttle," +
            "lte_id, lte_bw, lte_rsrp, nr_id, nr_rsrp, nr_bands\n";

    public ProfilingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("tftf", "Start  profiling service");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My Service Channel";
            String description = "Channel for My Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service Running")
                .setContentText("Doing something in the background...")
                .build();

        startForeground(NOTIFICATION_ID, notification);

        intent = new Intent("org.appspot.apprtc.UPDATE_HUD_FRAGMENT");
        batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        hardwarePropertiesManager = (HardwarePropertiesManager) getSystemService(Context.HARDWARE_PROPERTIES_SERVICE);
        myFile = createCsvFile();
        referenceTime = System.currentTimeMillis();
        runnableCode = new Runnable() {
            @Override
            public void run() {
                GetBatteryInfo();
                GetCpuInfo();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    readLteInfo();
                    readNrInfo();
                }

                if (catCounter % 25 == 0) {
                    GetTempInfo();

                    String notificationContent = "Power: " + batteryPower + "mW, CPU temp: " + cpuTemp +" C, Throttle: " + skinState;
                    updateNotification(notificationContent);

                    catCounter = 1;
                } else {
                    catCounter += 1;
                }
                //Logging(myFile);
                // Schedule this runnable again after 1 second
                handler.postDelayed(this, 200);

            }
        };

        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);
    }

    public void GetBatteryInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        batteryVoltage = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) : -1;
        batteryPct = level * 100 / (float) scale;

        // Get battery current (in microamperes)
        batteryCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000;

        batteryPower = (int) (batteryCurrent * batteryVoltage / 1000);

        // Broadcast battery information
        intent.putExtra("BatteryCapacity", batteryPct);
        intent.putExtra("BatteryCurrent", batteryCurrent);
        intent.putExtra("BatteryPower", batteryPower);
        LocalBroadcastManager.getInstance(ProfilingService.this).sendBroadcast(intent);

        // Log the battery status
        Log.i("BatteryMonitorService", "Battery Level: " + batteryPct + "%, Voltage: " + batteryVoltage + "mV, Current: " + batteryCurrent + "mA, Power: " + batteryPower + "mW");
    }

    public void GetCpuInfo() {
        cpuLittle = (int) profiler.getCPUClk_little();
        cpuBig1 = (int) profiler.getCPUClk_big1();
        cpuBig2 = (int) profiler.getCPUClk_big2();

        // Broadcast battery information
        intent.putExtra("CpuLittle", cpuLittle);
        intent.putExtra("CpuBig1", cpuBig1);
        intent.putExtra("CpuBig2", cpuBig2);
        LocalBroadcastManager.getInstance(ProfilingService.this).sendBroadcast(intent);

        Log.i("CpuMonitorService", "Little: " + cpuLittle + " Big1: " + cpuBig1 + " Big2: " + cpuBig2);

    }

    public void GetTempInfo() {
        try {
            float[] cpuTemps = hardwarePropertiesManager.getDeviceTemperatures(
                    HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
                    HardwarePropertiesManager.TEMPERATURE_CURRENT);

            float[] skinTemps = hardwarePropertiesManager.getDeviceTemperatures(
                    HardwarePropertiesManager.DEVICE_TEMPERATURE_SKIN,
                    HardwarePropertiesManager.TEMPERATURE_CURRENT);

            skinTemp = printTemperatures("skin", skinTemps);
            cpuTemp = printTemperatures("cpu", cpuTemps);
            // skinState = profiler.getSkinState();

            intent.putExtra("SkinTemp", skinTemp);
            intent.putExtra("CpuTemp", cpuTemp);
            intent.putExtra("SkinState", skinState);
            LocalBroadcastManager.getInstance(ProfilingService.this).sendBroadcast(intent);

            Log.i("TempMonitorService", "Skin: " + skinTemp + " Cpu: " + cpuTemp + " SkinState: " + skinState);
        }
        catch (Exception e) {
            Log.i("TempMonitorService", "No permission");
        }
    }

    // Function to print temperature arrays
    private float printTemperatures(String label, float[] temps) {
        if (temps != null && temps.length > 0) {
            for (float temp : temps) {
                Log.i("TemperatureInfo", label + " " + temp);
                return temp;
            }
        } else {
            Log.i("TemperatureInfo", label + ": No data");
        }
        return 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void readLteInfo() {
        TelephonyManager tm = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        List<CellInfo> cellInfoList = tm.getAllCellInfo();
        for (android.telephony.CellInfo cellInfo : cellInfoList) {
            if (cellInfo instanceof CellInfoLte) {
                // cast to CellInfoLte and call all the CellInfoLte methods you need
                lte_cellId = ((CellInfoLte) cellInfo).getCellIdentity().getPci();
                lte_bw = ((CellInfoLte) cellInfo).getCellIdentity().getBandwidth()/1000;
                lte_rsrp = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();

                int lte_info =  lte_rsrp;

                intent.putExtra("LteInfo", lte_info);
                return;
            }
        }
        String lte_info = "NO LTE SIGNAL !";
        intent.putExtra("LteInfo", lte_info);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void readNrInfo() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        List<CellInfo> cellInfoList = tm.getAllCellInfo();
        for (CellInfo cellInfo : cellInfoList) {
            if (cellInfo instanceof CellInfoNr) {
                CellInfoNr cellInfoNr = (CellInfoNr) cellInfo;
                CellIdentityNr cellIdentity = (CellIdentityNr) cellInfoNr.getCellIdentity();
                CellSignalStrengthNr cellSignalStrength = (CellSignalStrengthNr) cellInfoNr.getCellSignalStrength();

                nr_cellId = cellIdentity.getNci();
                int [] bands = cellIdentity.getBands();
                nr_rsrp = cellSignalStrength.getSsRsrp();

                nr_band = Arrays.toString(bands); // Convert bands array to string

                int nr_info = nr_rsrp;
                intent.putExtra("NrInfo", nr_info);
            }
        }
        String nr_info = "No NR signal!";
        intent.putExtra("NrInfo", nr_info);
    }

    public File createCsvFile() {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date());
        String fileName = "energy-" + timeStamp + ".csv";

        // Get the directory for the app's private files
        File storageDir = getApplicationContext().getFilesDir();
        File csvFile = new File(storageDir, fileName);

        Log.i("csv logging service", "Storage: " + storageDir + " File name: " + fileName);

        try {
            FileWriter writer = new FileWriter(csvFile);
            writer.append(header);
            writer.append("\n"); // New line for the next record

            writer.flush();
            writer.close();
        }
        catch (Exception e) {

        }

        return csvFile;
    }

    public void Logging (File csvFile) {
        try {
            FileWriter writer = new FileWriter(csvFile, true);
            long currentTime = System.currentTimeMillis() - referenceTime;
            String dataRow = currentTime + ", " +
                    cpuLittle + ", " + cpuBig1 + ", " + cpuBig2 + ", " +
                    batteryPct + ", " + batteryCurrent + ", " +
                    batteryPower + ", " + skinTemp + ", " + cpuTemp + ", " +
                    skinState + ", "+
                    lte_cellId + ", "+ lte_bw + ", "+ lte_rsrp + ", "+
                    nr_cellId + ", "+ nr_rsrp + ", "+ nr_band;
            writer.append(dataRow);
            writer.append("\n"); // New line for the next record

            writer.flush();
            writer.close();
        }
        catch (Exception e) {

        }
    }

    @Override
    public void onDestroy() {
        File csvFile = myFile;
        Map<String, Double> averages = calculateAverages(csvFile);

        // Log or use the averages as needed
        String stats = "Avg Battery Power: " + averages.get("averageBatteryPower") +
                ", Avg CPU Temp: " + averages.get("averageTempCpu") +
                ", Avg Thermal Throttle: " + averages.get("averageThermalThrottle");

        intent.putExtra("Stats", stats);
        LocalBroadcastManager.getInstance(ProfilingService.this).sendBroadcast(intent);

        super.onDestroy();
        stopForeground(true);
        handler.removeCallbacks(runnableCode);

        Log.i("tftf", "stop profiling service " + stats);

    }

    private void updateNotification(String contentText) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, HudFragment.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Measurement in progress...")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public Map<String, Double> calculateAverages(File csvFile) {
        BufferedReader reader = null;
        int count = 0;
        double totalBatteryPower = 0;
        double totalTempCpu = 0;
        double totalThermalThrottle = 0;

        try {
            reader = new BufferedReader(new FileReader(csvFile));
            String line;

            // Skip header line
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 10) { // Ensure there are enough values in the line
                    totalBatteryPower += Double.parseDouble(values[6].trim()); // battery_power
                    totalTempCpu += Double.parseDouble(values[8].trim()); // temp_cpu
                    totalThermalThrottle += Double.parseDouble(values[9].trim()); // thermal_throttle
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Map<String, Double> averages = new HashMap<>();
        if (count > 0) {
            averages.put("averageBatteryPower", (double) (totalBatteryPower / count));
            averages.put("averageTempCpu", (double)(totalTempCpu / count));
            averages.put("averageThermalThrottle", (double)(totalThermalThrottle / count));
        }
        return averages;
    }

}
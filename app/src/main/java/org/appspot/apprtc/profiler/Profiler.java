package org.appspot.apprtc.profiler;

import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Profiler {
    private Timer timer= new Timer();
    public Profiler() {
        getConfiguration ();

        // To read with negligible overheads
        changePermissions(TZONE_MODEM_SKIN);
        changePermissions(TZONE_CPU);
        changePermissions(CLOCK_LITTLE);
        changePermissions(CLOCK_BIG1);
        changePermissions(CLOCK_BIG2);
        changePermissions(TSTATE_MODEM_SKIN);
        changePermissions(CAPACITY_BATTERY);
        changePermissions(CURRENT_BATTERY);
        changePermissions(VOLTAGE_BATTERY);
    };

    public static String model_name = "";
    public static String NET_INTERFACE = "";
    private static String TZONE_MODEM_SKIN = "hi";
    private static String TZONE_CPU;
    public static String CLOCK_LITTLE;
    public static String CLOCK_BIG1;
    public static String CLOCK_BIG2;
    private static String TSTATE_MODEM_SKIN;

    private static String CAPACITY_BATTERY;
    private static String CURRENT_BATTERY;
    private static String VOLTAGE_BATTERY;

    public static List<Integer> AVAIL_LITTLE_FREQ;
    public static List<Integer> AVAIL_BIG1_FREQ;
    public static List<Integer> AVAIL_BIG2_FREQ;

    public static void getConfiguration() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            model_name = capitalize(model);
        } else {
            model_name = capitalize(manufacturer) + " " + model;
        }
        Log.i("modelname", model_name);
        configureTargets ();
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public static void configureTargets() {
        // Galaxy S20U
        if (model_name.contains("SM-G988N")) {
            TZONE_MODEM_SKIN = "/sys/class/thermal/thermal_zone47/temp";
            TZONE_CPU = "/sys/class/thermal/thermal_zone14/temp";
            CLOCK_LITTLE = "/sys/devices/system/cpu/cpufreq/policy0/cpuinfo_cur_freq";
            CLOCK_BIG1 = "/sys/devices/system/cpu/cpufreq/policy4/cpuinfo_cur_freq";
            CLOCK_BIG2 = "/sys/devices/system/cpu/cpufreq/policy7/cpuinfo_cur_freq";
            TSTATE_MODEM_SKIN = "/sys/class/thermal/cooling_device22/cur_state";
        }

        // Galaxy S22 path
        if (model_name.contains("SM-S901N")) {
            // rmnet_data1: skt, rmnet_data2: kt
            NET_INTERFACE = "rmnet_data2";
            TZONE_MODEM_SKIN = "/sys/class/thermal/thermal_zone16/temp";
            TZONE_CPU = "/sys/class/thermal/thermal_zone33/temp";
            CLOCK_LITTLE = "/sys/devices/system/cpu/cpufreq/policy0/cpuinfo_cur_freq";
            CLOCK_BIG1 = "/sys/devices/system/cpu/cpufreq/policy4/cpuinfo_cur_freq";
            CLOCK_BIG2 = "/sys/devices/system/cpu/cpufreq/policy7/cpuinfo_cur_freq";
            TSTATE_MODEM_SKIN = "/sys/class/thermal/cooling_device45/cur_state";
            CURRENT_BATTERY = "/sys/class/power_supply/battery/current_avg";
            VOLTAGE_BATTERY = "/sys/class/power_supply/battery/voltage_now";
        }


        // Pixel 5
        if (model_name.contains("GD1YQ")) {
            TZONE_MODEM_SKIN = "/sys/class/thermal/thermal_zone6/temp";
            TZONE_CPU = "/sys/class/thermal/thermal_zone17/temp";
            CLOCK_LITTLE = "/sys/devices/system/cpu/cpufreq/policy0/cpuinfo_cur_freq";
            CLOCK_BIG1 = "/sys/devices/system/cpu/cpufreq/policy6/cpuinfo_cur_freq";
            CLOCK_BIG2 = "/sys/devices/system/cpu/cpufreq/policy7/cpuinfo_cur_freq";
            TSTATE_MODEM_SKIN = "/sys/class/thermal/cooling_device31/cur_state";
        }

        // Pixel 6a
        if (model_name.contains("GX7AS") || model_name.contains("GB62Z") || model_name.contains("G1AZG")||model_name.contains("Pixel 6a")) {
//            NET_INTERFACE = "rmnet1";
            NET_INTERFACE = "wlan0"; // this is a temporary setting for test
            TZONE_MODEM_SKIN = "/sys/class/thermal/thermal_zone12/temp"; // cellular emergency is triggered by neutral_therm (thermal_zone6)
            TZONE_CPU = "/sys/class/thermal/thermal_zone0/temp";
            CLOCK_LITTLE = "/sys/devices/system/cpu/cpufreq/policy0/cpuinfo_cur_freq";
            CLOCK_BIG1 = "/sys/devices/system/cpu/cpufreq/policy4/cpuinfo_cur_freq";
            CLOCK_BIG2 = "/sys/devices/system/cpu/cpufreq/policy6/cpuinfo_cur_freq";
            TSTATE_MODEM_SKIN = "/sys/class/thermal/cooling_device8/cur_state";
            CURRENT_BATTERY = "/sys/class/power_supply/battery/current_avg";
            VOLTAGE_BATTERY = "/sys/class/power_supply/battery/voltage_now";
        }

        // Pixel 7 Pro
        if (model_name.contains("Pixel 7 Pro")) {
            NET_INTERFACE = "rmnet1";
            TZONE_MODEM_SKIN = "/sys/class/thermal/thermal_zone12/temp"; // cellular emergency is triggered by neutral_therm (thermal_zone6)
            TZONE_CPU = "/sys/class/thermal/thermal_zone3/temp";
            CLOCK_LITTLE = "/sys/devices/system/cpu/cpufreq/policy0/cpuinfo_cur_freq";
            CLOCK_BIG1 = "/sys/devices/system/cpu/cpufreq/policy4/cpuinfo_cur_freq";
            CLOCK_BIG2 = "/sys/devices/system/cpu/cpufreq/policy6/cpuinfo_cur_freq";
            TSTATE_MODEM_SKIN = "/sys/class/thermal/cooling_device11/cur_state";
            CAPACITY_BATTERY = "/sys/class/power_supply/battery/capacity";
            CURRENT_BATTERY = "/sys/class/power_supply/battery/current_avg";
            VOLTAGE_BATTERY = "/sys/class/power_supply/battery/voltage_now";
        }
        AVAIL_LITTLE_FREQ = getAvailableCpuFrequencies(CLOCK_LITTLE.replace("cpuinfo_cur_freq", ""));
        AVAIL_BIG1_FREQ = getAvailableCpuFrequencies(CLOCK_BIG1.replace("cpuinfo_cur_freq", ""));
        AVAIL_BIG2_FREQ = getAvailableCpuFrequencies(CLOCK_BIG2.replace("cpuinfo_cur_freq", ""));
    }

    public void changePermissions(String filepath) {
        Process su;
        try {
            Log.i("tftf", "[changePermissions] " + filepath);
            su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            // "chmod 777" sets the file permissions to be world-readable/writable/executable.
            // Replace with the permissions you want.
            outputStream.writeBytes("chmod 777 " + filepath + "\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public float getSkinTemp () {
        return getSysfs(TZONE_MODEM_SKIN);
    }
    public float getCPUTemp () {
        return getSysfsCat(TZONE_CPU);
    }
    public float getSkinState () {
        return getSysfsCat(TSTATE_MODEM_SKIN) * 1000.0f;
    }
    public float getCPUClk_big1 () {
        return getSysfs(CLOCK_BIG1);
    }
    public float getCPUClk_big2 () {
        return getSysfs(CLOCK_BIG2);
    }
    public float getCPUClk_little () {
        return getSysfs(CLOCK_LITTLE);
    }

    public float getBatteryCapacity() {return getSysfsCat(CAPACITY_BATTERY); }
    public float getBatteryCurrent() {return getSysfsCat(CURRENT_BATTERY); }
    public float getBatteryVoltage() {return getSysfsCat(VOLTAGE_BATTERY); }


    public float getSysfs(String dir) {
        Log.i("tftf", "[getSysfs] " + dir);
        try {
            RandomAccessFile file = new RandomAccessFile(dir, "r");
            String line = file.readLine();
            Log.i("tftf", "[getSysfs] " + line);
            float temp = Float.parseFloat(line.trim()) / 1000.0f;
            file.close();
            return temp;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("tftf", String.valueOf(e));
            return 0.0f;
        }
    }

    public float getSysfsCat(String dir) {
        Process p;
        try {
            p = Runtime.getRuntime().exec("su -c cat " + dir);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            float temp = Float.parseFloat(line.trim()) / 1000.0f;
            reader.close();
            // It's important to consume and close the error stream
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (errorReader.readLine() != null) {}
            errorReader.close();
            return temp;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }

    public static List<Integer> getAvailableCpuFrequencies(String file_path) {
        String filePath = file_path + "/scaling_available_frequencies";
        BufferedReader reader = null;
        List<Integer> frequencies = new ArrayList<>();

        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            if (line != null) {
                String[] freqs = line.split(" ");
                for (String freq : freqs) {
                    try {
                        frequencies.add(Integer.parseInt(freq.trim()));
                    } catch (NumberFormatException e) {
                        // Skip this value if it can't be parsed
                    }
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

        return frequencies;
    }


    public void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try{
                    String line2 = String.valueOf(getCPUClk_little());
                    String line3 = String.valueOf(getCPUClk_big1());
                    String line4 = String.valueOf(getCPUClk_big2());
                    Log.i("tftf", "[clock_little] " + line2 + " MHz");
                    Log.i("tftf", "[clock_big1] " + line3 + " MHz");
                    Log.i("tftf", "[clock_big2] " + line4 + " MHz");

                }catch (NullPointerException e) {
                    e.printStackTrace();
                }

            }
        }, 0, 1000); // 1초마다 실행
    }

    public void stop(){
        if (timer != null) {
            timer.cancel();
        }
    }
}

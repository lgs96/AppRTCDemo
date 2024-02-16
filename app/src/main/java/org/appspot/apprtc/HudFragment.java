/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.appspot.apprtc;

import android.app.Fragment;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.webrtc.StatsReport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.opencsv.CSVWriter;


/**
 * Fragment for HUD statistics display.
 */
public class HudFragment extends Fragment {

  // Step 1: Create a list of keys to extract from the map
  List<String> computeToExtractSent = new ArrayList<String>() {{
    add("googAvgEncodeMs");
    add("googFrameHeightInput");
    add("googFrameWidthInput");
    add("googFrameHeightSent");
    add("googFrameWidthSent");
    add("googRtt");
  }};

  List<String> computeToExtractRecv = new ArrayList<String>() {{
    add("googRenderDelayMs");
    add("googDecodeMs");
    add("googFrameHeightReceived");
    add("googFrameWidthReceived");
  }};

  List<String> networkToExtractSent = new ArrayList<String>() {{
    add("googFrameRateSent");
    add("googTargetEncBitrate");
    add("googActualEncBitrate");
    add("googTransmitBitrate");
    add("googAvailableSendBandwidth");
    add("googRetransmitBitrate");
    add("packetsLost");
  }};

  List<String> networkToExtractRecv = new ArrayList<String>() {{
    add("googFrameRateReceived");
    add("bytesReceived");
  }};

  List<String> combinedSentList;
  List<String> combinedRecvList;


  String[] valuesSent;
  String[] valuesRecv;

  File csvfileSent = null;
  File csvfileRecv = null;
  CSVWriter writerSent = null;
  CSVWriter writerRecv = null;
  String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
  String fileNameSent = "webRtc_Sent_" + timeStamp + ".csv";
  String fileNameRecv = "webRtc_Recv_" + timeStamp + ".csv";

  long startTime = System.nanoTime();

  String[] csv_dataSent;
  String[] csv_dataRecv;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          // Handling Sent
          csvfileSent = new File(getActivity().getFilesDir(), fileNameSent);
          writerSent = new CSVWriter(new FileWriter(csvfileSent, true));
          combinedSentList = new ArrayList<String>();
          combinedSentList.add("Time");
          combinedSentList.addAll(computeToExtractSent);
          combinedSentList.addAll(networkToExtractSent);
          csv_dataSent = combinedSentList.toArray(new String[0]);
          writerSent.writeNext(csv_dataSent);
          writerSent.close(); // close file

          // Handling Recv
          csvfileRecv = new File(getActivity().getFilesDir(), fileNameRecv);
          writerRecv = new CSVWriter(new FileWriter(csvfileRecv, true));
          combinedRecvList = new ArrayList<String>();
          combinedRecvList.add("Time");
          combinedRecvList.addAll(computeToExtractRecv);
          combinedRecvList.addAll(networkToExtractRecv);
          csv_dataRecv = combinedRecvList.toArray(new String[0]);
          writerRecv.writeNext(csv_dataRecv);
          writerRecv.close(); // close file


          valuesSent = new String[combinedSentList.size()];
          Arrays.fill(valuesSent, "N/A");

          valuesRecv = new String[combinedRecvList.size()];
          Arrays.fill(valuesRecv, "N/A");

          String filePathSent = csvfileSent.getAbsolutePath();
          Log.i("CSV File Path", filePathSent);

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }

  private TextView encoderStatView;
  private TextView hudViewBwe;
  private TextView hudViewConnection;
  private TextView hudViewVideoSend;
  private TextView hudViewVideoRecv;
  private ImageButton toggleDebugButton;
  private boolean videoCallEnabled;
  private boolean displayHud;
  private volatile boolean isRunning;
  private CpuMonitor cpuMonitor;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View controlView = inflater.inflate(R.layout.fragment_hud, container, false);

    // Create UI controls.
    encoderStatView = controlView.findViewById(R.id.encoder_stat_call);
    hudViewBwe = controlView.findViewById(R.id.hud_stat_bwe);
    hudViewConnection = controlView.findViewById(R.id.hud_stat_connection);
    hudViewVideoSend = controlView.findViewById(R.id.hud_stat_video_send);
    hudViewVideoRecv = controlView.findViewById(R.id.hud_stat_video_recv);
    toggleDebugButton = controlView.findViewById(R.id.button_toggle_debug);

    toggleDebugButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (displayHud) {
          int visibility =
              (hudViewBwe.getVisibility() == View.VISIBLE) ? View.INVISIBLE : View.VISIBLE;
          hudViewsSetProperties(visibility);
        }
      }
    });

    return controlView;
  }

  @Override
  public void onStart() {
    super.onStart();

    Bundle args = getArguments();
    if (args != null) {
      videoCallEnabled = args.getBoolean(CallActivity.EXTRA_VIDEO_CALL, true);
      displayHud = args.getBoolean(CallActivity.EXTRA_DISPLAY_HUD, false);
    }
    int visibility = displayHud ? View.VISIBLE : View.INVISIBLE;
    encoderStatView.setVisibility(visibility);
    toggleDebugButton.setVisibility(visibility);
    hudViewsSetProperties(View.INVISIBLE);
    isRunning = true;
  }

  @Override
  public void onStop() {
    isRunning = false;
    super.onStop();
  }

  public void setCpuMonitor(CpuMonitor cpuMonitor) {
    this.cpuMonitor = cpuMonitor;
  }

  private void hudViewsSetProperties(int visibility) {
    hudViewBwe.setVisibility(visibility);
    hudViewConnection.setVisibility(visibility);
    hudViewVideoSend.setVisibility(visibility);
    hudViewVideoRecv.setVisibility(visibility);
    hudViewBwe.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5);
    hudViewConnection.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5);
    hudViewVideoSend.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5);
    hudViewVideoRecv.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5);
  }

  private Map<String, String> getReportMap(StatsReport report) {
    Map<String, String> reportMap = new HashMap<>();
    for (StatsReport.Value value : report.values) {
      reportMap.put(value.name, value.value);
    }
    return reportMap;
  }

  public void updateEncoderStatistics(final StatsReport[] reports) {
    if (!isRunning || !displayHud) {
      return;
    }
    StringBuilder encoderStat = new StringBuilder(128);
    StringBuilder bweStat = new StringBuilder();
    StringBuilder connectionStat = new StringBuilder();
    StringBuilder videoSendStat = new StringBuilder();
    StringBuilder videoRecvStat = new StringBuilder();
    String fps = null;
    String targetBitrate = null;
    String actualBitrate = null;


    new Thread(new Runnable() {
      public void run() {

        for (StatsReport report : reports) {
          Map<String, String> reportMap = getReportMap(report);
          double elapsedSeconds = (System.nanoTime() - startTime) / 1e9;

          if ((report.type.equals("ssrc") && report.id.contains("ssrc"))) {
            if (report.id.contains("send")) {
              String trackId = reportMap.get("googTrackId");
              if (trackId != null && trackId.contains(PeerConnectionClient.VIDEO_TRACK_ID)) {
                valuesSent[0] = String.format("%.3f", elapsedSeconds);
                for (int i = 1; i < combinedSentList.size(); i++) {
                  if (reportMap.containsKey(combinedSentList.get(i))) {
                    Log.i("Goodsol CSV", report.id + " " + combinedSentList.get(i));
                    valuesSent[i] = reportMap.get(combinedSentList.get(i));
                  }
                }
                try {
                  writerSent = new CSVWriter(new FileWriter(csvfileSent, true)); // open file
                  writerSent.writeNext(valuesSent);
                  writerSent.flush();
                  writerSent.close(); // close file

                  Log.i("CSV values Sent", Arrays.toString(valuesSent));
                } catch (IOException e) {
                  e.printStackTrace();
                  Log.e("CSV_WRITE_ERROR", "Error writing to CSV file", e);
                }
              }
            } else if (report.id.contains("recv")) {
              String frameWidth = reportMap.get("googFrameWidthReceived");
              if (frameWidth != null) {
                valuesRecv[0] = String.format("%.3f", elapsedSeconds);
                for (int i = 1; i < combinedRecvList.size(); i++) {
                  if (reportMap.containsKey(combinedRecvList.get(i))) {
                    valuesRecv[i] = reportMap.get(combinedRecvList.get(i));
                  }
                  //Log.i("CSV File Recv", valuesRecv[i]);
                }
                try {
                  writerRecv = new CSVWriter(new FileWriter(csvfileRecv, true)); // open file
                  writerRecv.writeNext(valuesRecv);
                  writerRecv.flush();
                  writerRecv.close(); // close file

                  Log.i("CSV values Recv", Arrays.toString(valuesRecv));
                } catch (IOException e) {
                  e.printStackTrace();
                  Log.e("CSV_WRITE_ERROR", "Error writing to CSV file", e);
                }
              }
            }
          }
          else if (report.id.equals("bweforvideo")) {
            valuesSent[0] = String.format("%.3f", elapsedSeconds);
            for (int i = 1; i < combinedSentList.size(); i++) {
              if (reportMap.containsKey(combinedSentList.get(i))) {
                valuesSent[i] = reportMap.get(combinedSentList.get(i));
              }
            }
            try {
              writerSent = new CSVWriter(new FileWriter(csvfileSent, true)); // open file
              writerSent.writeNext(valuesSent);
              writerSent.flush();
              writerSent.close(); // close file

              Log.i("CSV values Sent", Arrays.toString(valuesSent));
            } catch (IOException e) {
              e.printStackTrace();
              Log.e("CSV_WRITE_ERROR", "Error writing to CSV file", e);
            }
          }
        }
      }
    }).start();

    for (StatsReport report : reports) {
      // Print report to figure out the available performance logs
      Map<String, String> myMap = getReportMap(report);
      Log.i("Goodsol", "Update map stats");
      for (Map.Entry<String, String> entry : myMap.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        Log.i("Goodsol", report.id + " " + key +" " + value);
      }

      if (report.type.equals("ssrc") && report.id.contains("ssrc") && report.id.contains("send")) {
        // Send video statistics.
        Map<String, String> reportMap = getReportMap(report);
        String trackId = reportMap.get("googTrackId");
        if (trackId != null && trackId.contains(PeerConnectionClient.VIDEO_TRACK_ID)) {
          fps = reportMap.get("googFrameRateSent");
          videoSendStat.append(report.id).append("\n");
          for (StatsReport.Value value : report.values) {
            String name = value.name.replace("goog", "");
            videoSendStat.append(name).append("=").append(value.value).append("\n");
          }
        }

      } else if (report.type.equals("ssrc") && report.id.contains("ssrc") && report.id.contains("recv")) {
        // Receive video statistics.
        Map<String, String> reportMap = getReportMap(report);
        // Check if this stat is for video track.
        String frameWidth = reportMap.get("googFrameWidthReceived");
        if (frameWidth != null) {
          videoRecvStat.append(report.id).append("\n");
          for (StatsReport.Value value : report.values) {
            String name = value.name.replace("goog", "");
            videoRecvStat.append(name).append("=").append(value.value).append("\n");
          }
        }


      } else if (report.id.equals("bweforvideo")) {
        // BWE statistics.
        Map<String, String> reportMap = getReportMap(report);
        targetBitrate = reportMap.get("googTargetEncBitrate");
        actualBitrate = reportMap.get("googActualEncBitrate");

        bweStat.append(report.id).append("\n");
        for (StatsReport.Value value : report.values) {
          String name = value.name.replace("goog", "").replace("Available", "");
          bweStat.append(name).append("=").append(value.value).append("\n");
        }
      } else if (report.type.equals("googCandidatePair")) {
        // Connection statistics.
        Map<String, String> reportMap = getReportMap(report);
        String activeConnection = reportMap.get("googActiveConnection");
        if (activeConnection != null && activeConnection.equals("true")) {
          connectionStat.append(report.id).append("\n");
          for (StatsReport.Value value : report.values) {
            String name = value.name.replace("goog", "");
            connectionStat.append(name).append("=").append(value.value).append("\n");
          }
        }
      }
    }
    hudViewBwe.setText(bweStat.toString());
    hudViewConnection.setText(connectionStat.toString());
    hudViewVideoSend.setText(videoSendStat.toString());
    hudViewVideoRecv.setText(videoRecvStat.toString());

    if (videoCallEnabled) {
      if (fps != null) {
        encoderStat.append("Fps:  ").append(fps).append("\n");
      }
      if (targetBitrate != null) {
        encoderStat.append("Target BR: ").append(targetBitrate).append("\n");
      }
      if (actualBitrate != null) {
        encoderStat.append("Actual BR: ").append(actualBitrate).append("\n");
      }
    }

    if (cpuMonitor != null) {
      encoderStat.append("CPU%: ")
          .append(cpuMonitor.getCpuUsageCurrent())
          .append("/")
          .append(cpuMonitor.getCpuUsageAverage())
          .append(". Freq: ")
          .append(cpuMonitor.getFrequencyScaleAverage());
    }
    encoderStatView.setText(encoderStat.toString());
  }
}

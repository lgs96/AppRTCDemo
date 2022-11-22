package org.appspot.apprtc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import org.jetbrains.annotations.Nullable;

public class MediaProjectionAccessService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent intent1 = new Intent(this, PeerConnectionClient.class);

        PendingIntent pendingIntent1 = PendingIntent.getActivity(this, 0, intent1, 0);

        Notification notification1 = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification1 = new Notification.Builder(this, "ScreenRecorder")
                    .setContentTitle("yNote studios")
                    .setContentText("Filming...")
                    .setContentIntent(pendingIntent1).build();
        }

        startForeground(1, notification1);

        return super.onStartCommand(intent, flags, startId);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("ScreenRecorder", "Foreground notification",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();

        super.onDestroy();
    }
}
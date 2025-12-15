package com.base.baterynotification;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class BatteryApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }
    // Tạo kênh thông báo cho service phía dưới chứ không phải gửi thông báo(vì mỗi thông báo yêu cầu phải có 1 kênh.
    // có thể tạo luôn trong service nhưng không nên)
    // bắt buộc tạo TRƯỚC khi tạo thông báo
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "BatteryMonitorServiceChannel",
                    "Theo dõi Pin (Service)",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationChannel alertChannel = new NotificationChannel(
                    "BatteryAlertChannel",
                    "Cảnh báo Pin Đầy",
                    NotificationManager.IMPORTANCE_HIGH
            );

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            alertChannel.setSound(alarmSound, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                manager.createNotificationChannel(alertChannel);
            }
        }
    }
}

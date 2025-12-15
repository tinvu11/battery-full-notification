package com.base.baterynotification.utils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.base.baterynotification.R;
import com.base.baterynotification.data.repository.BatteryRepository;
import com.base.baterynotification.ui.main.MainActivity;


// Chạy nền không bị kill bởi hệ thống
public class BatteryMonitorService extends Service {
    private static final String TAG = "BatteryMonitorService";

    // ID cho Notification
    public static final String CHANNEL_ID_FOREGROUND = "BatteryMonitorServiceChannel";
    public static final String CHANNEL_ID_ALERT = "BatteryAlertChannel";
    private static final int FOREGROUND_SERVICE_ID = 1;
    private static final int ALERT_NOTIFICATION_ID = 2;
    private int TARGET_BATTERY_LEVEL = 90;
    private MediaPlayer mediaPlayer; // Biến quản lý âm thanh

    // 1. Định nghĩa tên hành động (Key) để Activity nhận diện
    public static final String ACTION_SERVICE_STOPPED = "ACTION_SERVICE_STOPPED";
    private static final String ACTION_STOP_ALARM = "STOP_ALARM";
    private boolean isAlertSent = false; // Cờ để đảm bảo chỉ thông báo 1 lần
    private BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // 1. Lấy Nhiệt độ (Temperature)
            int temperatureInt = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            float temperature = temperatureInt / 10.0f;

            // 2. Lấy Điện áp (Voltage)
            int voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            float voltage = voltageMv / 1000.0f;

            // 3. Lấy Tuổi thọ/Sức khỏe (Health)
            int healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
            String healthString = getHealthString(healthInt);

            // 4. Lấy Công nghệ pin (Li-ion, Li-poly...)
            String technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);

            // Lấy thời gian còn lại để sạc đầy
            long timeRemaining = -1;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) { // Android 9+
                BatteryManager mBatteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                if (mBatteryManager != null) {
                    timeRemaining = mBatteryManager.computeChargeTimeRemaining();
                }
            }
            if (timeRemaining > 0) {
                long minutes = (timeRemaining / 1000) / 60;
            }

            long chargeCounter = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                BatteryManager mBatteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                if (mBatteryManager != null) {
                    // Trả về µAh -> Chia 1000 để ra mAh
                    chargeCounter = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000;
                }
            }

            //6. Lấy trạng thái sạc
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            boolean isFull = status == BatteryManager.BATTERY_STATUS_FULL;

            //7. Lấy mức pin hiện tại
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPercent = (int) ((level / (float) scale) * 100);

            BatteryRepository.getInstance().updateBatteryInfo(
                    temperature,
                    voltage,
                    healthString,
                     isCharging,
                    batteryPercent,
                    technology,
                     timeRemaining,
                     chargeCounter
            );

           if ((batteryPercent >= TARGET_BATTERY_LEVEL || isFull) && isCharging && !isAlertSent) {
                isAlertSent = true;
                sendBatteryAlertNotification();
            } else if (!isCharging && !isFull) {
               // trường hợp mà khi người dùng chưa cắm sạc thì tắt luôn
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.hasExtra("TARGET_LEVEL")) {
            TARGET_BATTERY_LEVEL = intent.getIntExtra("TARGET_LEVEL", 100);
        }
        if (intent != null && ACTION_STOP_ALARM.equals(intent.getAction())) {
            stopAlarm();
            stopForeground(true);
            Intent stopSignal = new Intent(ACTION_SERVICE_STOPPED);
            stopSignal.setPackage(getPackageName());
            sendBroadcast(stopSignal);
            stopSelf();
            return START_NOT_STICKY;
        }


        Notification notification = createForegroundNotification();
        startForeground(FOREGROUND_SERVICE_ID, notification);
        // startForeground: hàm giữ cho app không bị kill nhưng nó cần 1 thông báo để cho người dùng biết là app đang chạy
        // vì thế cần FOREGROUND_SERVICE_ID, notification để hiển thị thông báo nhưng thông báo này không cần phải đổ chuông hay pop up
        // mà chỉ cần hiển thị lên trên thanh status bar của điện thoại do đó cần 1 kênh được đăng ký là IMPORTANT_LOW

        // Đăng ký lắng nghe sự kiện pin
        // ACTION_BATTERY_CHANGED broadcast dùng để phát hiện ra bất cứ thay đổi nào của pin như tăng giảm, nhiệt độ, nguồn điện
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //khi pin thay đổi hệ thống sẽ gửi broadcast cho batteryLevelReceiver nó sẽ nhận và kiểm tra và gửi thông báo
        registerReceiver(batteryLevelReceiver, filter);

        // Đặt lại cờ mỗi khi service được khởi động lại
        isAlertSent = false;
        // Nếu service bị kill, tự khởi động lại
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        stopAlarm();
        try {
            unregisterReceiver(batteryLevelReceiver);
            Log.d(TAG, "Service đã dừng.");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Không dùng bound service
    }


    // Tạo thông báo cho việc chạy ngầm(hiển thị trong thanh status bar của điện thoại nhờ kênh thông báo important_low. Nếu không có nó thì không gọi được hàm startForeground)
    private Notification createForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
                .setContentTitle("Đang theo dõi pin")
                .setContentText("Sẽ thông báo khi đạt " + TARGET_BATTERY_LEVEL + "%")
                .setContentIntent(pendingIntent)
                .build();
    }

    // Gửi thông báo pop up cảnh báo khi pin đạt mức( nổ chuông)

//    private void sendBatteryAlertNotification() {
//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
//
//        // Lấy âm thanh báo thức
//        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
//        if (alarmSound == null) {
//            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        }
//
//        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
//                .setContentTitle("Pin đã đạt " + TARGET_BATTERY_LEVEL + "%")
//                .setContentText("Vui lòng rút sạc để bảo vệ pin!")
//                .setSmallIcon(R.drawable.ic_battery_full) // Thêm icon này vào drawable
//                .setSound(alarmSound) // Đặt âm thanh báo thức
//                .setPriority(NotificationCompat.PRIORITY_HIGH) // Ưu tiên cao
//                .setCategory(NotificationCompat.CATEGORY_ALARM) // Phân loại là báo thức
//                .setContentIntent(pendingIntent)
//                .setAutoCancel(true) // Tự xóa khi nhấn vào
//                .build();
//
//        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        if (manager != null) {
//            // Hiển thị thông báo
//            manager.notify(ALERT_NOTIFICATION_ID, notification);
//        }
//    }
//
//    private void playAlarmSound() {
//        // Nếu đang kêu rồi thì thôi không bật lại
//        if (mediaPlayer != null && mediaPlayer.isPlaying()) return;
//
//        try {
//            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
//            if (alarmUri == null) {
//                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            }
//
//            mediaPlayer = new MediaPlayer();
//            mediaPlayer.setDataSource(this, alarmUri);
//
//            // Cấu hình âm thanh chuẩn Báo thức (Quan trọng để kêu to)
//            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
//                    .setUsage(AudioAttributes.USAGE_ALARM)
//                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                    .build());
//
//            mediaPlayer.setLooping(true); // LẶP LẠI LIÊN TỤC
//            mediaPlayer.prepare();
//            mediaPlayer.start();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    // hàm thông báo bằng chuông báo thức
    private void sendBatteryAlertNotification() {
        // 1. Intent mở App khi bấm vào thông báo
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingOpenApp = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE);

        // 2. Intent để TẮT CHUÔNG (Gửi lại vào chính Service này)
        Intent stopIntent = new Intent(this, BatteryMonitorService.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        PendingIntent pendingStop = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE);
        playAlarmSound();

        // 4. Tạo thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
                .setContentTitle("PIN ĐẦY " + TARGET_BATTERY_LEVEL + "%")
                .setContentText("Hãy rút sạc ngay!")
                .setSmallIcon(R.drawable.ic_battery_full)

                // Hiện đè lên màn hình khóa giống báo thức
                .setFullScreenIntent(pendingOpenApp, true)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Ưu tiên cao nhất
                .setCategory(NotificationCompat.CATEGORY_ALARM)

                .setOngoing(true) // Không cho vuốt xóa (phải bấm nút tắt)
                .setAutoCancel(false) // Bấm vào không tự mất

                // Thêm nút bấm hành động
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "TẮT CHUÔNG", pendingStop)
                .setContentIntent(pendingOpenApp);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(ALERT_NOTIFICATION_ID, builder.build());
        } else {
            Log.e(TAG, "NotificationManager is null, cannot show alert notification");
        }
    }
    private void playAlarmSound() {
        // Nếu đang kêu rồi thì thôi không bật lại
        if (mediaPlayer != null && mediaPlayer.isPlaying()) return;

        try {
            SharedPreferences prefs = getSharedPreferences("BatteryAppPrefs", MODE_PRIVATE);
            String customUriStr = prefs.getString("ALARM_URI", "");

            Uri alarmUri;

            if (!customUriStr.isEmpty()) {
                // Nếu người dùng đã chọn nhạc riêng -> Dùng nó
                alarmUri = Uri.parse(customUriStr);
            } else {
                // Nếu chưa chọn -> Dùng mặc định hệ thống
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);

            // Cấu hình âm thanh chuẩn Báo thức (Quan trọng để kêu to)
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());

            mediaPlayer.setLooping(true); // LẶP LẠI LIÊN TỤC
            mediaPlayer.prepare();
            mediaPlayer.start();

        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm sound", e);
            // Giải phóng MediaPlayer nếu có lỗi để tránh memory leak
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.release();
                } catch (Exception releaseError) {
                    Log.e(TAG, "Error releasing MediaPlayer", releaseError);
                }
                mediaPlayer = null;
            }
        }
    }
    private void stopAlarm() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
        }

        // Tắt luôn cái thông báo cảnh báo đi cho đỡ vướng mắt
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(ALERT_NOTIFICATION_ID); // ALERT_NOTIFICATION_ID là ID bạn tự đặt (ví dụ 2)
        } else {
            Log.e(TAG, "NotificationManager is null, cannot cancel notification");
        }
    }
    private static String getHealthString(int healthInt) {
        switch (healthInt) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Tốt";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Quá nóng";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Đã hỏng";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Quá áp";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Quá lạnh";
            default: return "Không rõ";
        }
    }
}
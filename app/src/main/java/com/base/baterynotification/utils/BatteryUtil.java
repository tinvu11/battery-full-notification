package com.base.baterynotification.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.base.baterynotification.data.model.BatteryInfo;

public class BatteryUtil {

    public static BatteryInfo getBatterySnapshot(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return null;

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


        return new BatteryInfo(temperature, voltage, healthString, isCharging, batteryPercent, technology, timeRemaining,  chargeCounter);
    }

    // Hàm phụ trợ chuyển đổi mã sức khỏe
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

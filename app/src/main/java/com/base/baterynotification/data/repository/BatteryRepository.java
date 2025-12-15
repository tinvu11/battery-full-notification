package com.base.baterynotification.data.repository;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.base.baterynotification.data.model.BatteryInfo;

public class BatteryRepository {
    private static BatteryRepository instance;
    private final MutableLiveData<BatteryInfo> batteryInfoLive = new MutableLiveData<>();

    // Singleton Pattern (Chỉ tạo 1 instance duy nhất)
    public static synchronized BatteryRepository getInstance() {
        if (instance == null) {
            instance = new BatteryRepository();
        }
        return instance;
    }
    public void updateBatteryInfo(float temp, float volt, String health, boolean isCharging, int batteryPercent, String technology,  long timeRemaining, long chargeCounter) {
        BatteryInfo info = new BatteryInfo(temp, volt, health, isCharging, batteryPercent, technology, timeRemaining, chargeCounter);
        batteryInfoLive.postValue(info);
    }
    public LiveData<BatteryInfo> getBatteryInfo() {
        return batteryInfoLive;
    }
}

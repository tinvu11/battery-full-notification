package com.base.baterynotification.ui.main;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.base.baterynotification.data.model.BatteryInfo;
import com.base.baterynotification.data.repository.BatteryRepository;
import com.base.baterynotification.utils.BatteryMonitorService;
import com.base.baterynotification.utils.BatteryUtil;
import com.base.baterynotification.utils.Event;

public class MainViewModel extends AndroidViewModel {

    private static final int DEFAULT_TARGET_LEVEL = 90;
    private static final String EXTRA_TARGET_LEVEL = "TARGET_LEVEL";

    // State
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> targetBatteryLevel = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isServiceRunning = new MutableLiveData<>(false);

    // Events
    private final MutableLiveData<Event<Boolean>> requestPermissionEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> toastMessageEvent = new MutableLiveData<>();

    // BroadcastReceiver cập nhật UI khi có thay đổi pin
    private final BroadcastReceiver uiBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshBatteryData();
        }
    };

    public MainViewModel(@NonNull Application application) {
        super(application);
        // Khởi tạo dữ liệu ban đầu
        refreshBatteryData();
        // Đăng ký receiver nhận ACTION_BATTERY_CHANGED
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        application.registerReceiver(uiBatteryReceiver, filter);
    }

    // --- Expose LiveData ---
    public LiveData<BatteryInfo> getRealtimeBatteryInfo() {
        return BatteryRepository.getInstance().getBatteryInfo();
    }

    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Integer> getTargetBatteryLevel() { return targetBatteryLevel; }
    public LiveData<Boolean> getIsServiceRunning() { return isServiceRunning; }
    public LiveData<Event<Boolean>> getRequestPermissionEvent() { return requestPermissionEvent; }
    public LiveData<Event<String>> getToastMessageEvent() { return toastMessageEvent; }

    // --- Actions ---
    public void setTargetBatteryLevel(int level) {
        if (level > 0 && level <= 100) targetBatteryLevel.setValue(level);
    }

    public void onPermissionResult(boolean isGranted) {
        if (isGranted) {
            startMonitoring();
        } else {
            statusMessage.setValue("Bạn cần cấp quyền để ứng dụng chạy.");
            toastMessageEvent.setValue(new Event<>("Vui lòng cấp quyền!"));
        }
    }

    public void checkPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean granted = ContextCompat.checkSelfPermission(getApplication(),
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            if (granted) startMonitoring();
            else requestPermissionEvent.setValue(new Event<>(true));
        } else {
            startMonitoring();
        }
    }

    public void stopMonitoring() {
        getApplication().stopService(createServiceIntent(getApplication(), DEFAULT_TARGET_LEVEL)); // stop by intent
        String msg = "Đã tắt theo dõi pin";
        statusMessage.setValue(msg);
        isServiceRunning.setValue(false);
        toastMessageEvent.setValue(new Event<>(msg));
    }

    public void checkServiceStatus() {
        boolean running = isServiceRunningInternal(BatteryMonitorService.class);
        isServiceRunning.postValue(running);
    }

    public void setIsServiceRunning(boolean isRunning) {
        isServiceRunning.setValue(isRunning);
    }

    // --- Internal helpers ---
    private void startMonitoring() {
        Integer current = targetBatteryLevel.getValue();
        int target = (current != null) ? current : DEFAULT_TARGET_LEVEL;

        Intent intent = createServiceIntent(getApplication(), target);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication().startForegroundService(intent);
        } else {
            getApplication().startService(intent);
        }

        String msg = "Đã bật theo dõi pin mức " + target + "%";
        statusMessage.setValue(msg);
        isServiceRunning.setValue(true);
        toastMessageEvent.setValue(new Event<>(msg));
    }

    private static Intent createServiceIntent(Context ctx, int targetLevel) {
        Intent i = new Intent(ctx, BatteryMonitorService.class);
        i.putExtra(EXTRA_TARGET_LEVEL, targetLevel);
        return i;
    }

    private boolean isServiceRunningInternal(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getApplication().getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }

    public void refreshBatteryData() {
        BatteryInfo info = BatteryUtil.getBatterySnapshot(getApplication());
        if (info != null) {
            BatteryRepository.getInstance().updateBatteryInfo(
                    info.temperature,
                    info.voltage,
                    info.health,
                    info.isCharging,
                    info.batteryPercent,
                    info.technology,
                    info.timeRemaining,
                    info.chargeCounter
            );
        }
    }

    @Override
    protected void onCleared() {
        // Hủy đăng ký receiver khi ViewModel bị hủy
        try {
            getApplication().unregisterReceiver(uiBatteryReceiver);
        } catch (IllegalArgumentException ignored) {
            // nếu chưa đăng ký hoặc đã unregister trước đó
        }
        super.onCleared();
    }
}

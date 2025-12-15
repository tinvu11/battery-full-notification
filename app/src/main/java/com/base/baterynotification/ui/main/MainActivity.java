package com.base.baterynotification.ui.main;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.base.baterynotification.R;
import com.base.baterynotification.databinding.ActivityMainBinding;
import com.base.baterynotification.utils.BatteryMonitorService;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private static final int REQUEST_CODE_POST_NOTIFICATION = 101;
    private MainViewModel viewModel;
    private final BroadcastReceiver serviceStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Khi nhận được tin nhắn "Service đã tắt"
            if (BatteryMonitorService.ACTION_SERVICE_STOPPED.equals(intent.getAction())) {
                // Ép trạng thái về FALSE ngay lập tức -> Switch sẽ tự tắt
                if (viewModel != null) {
                    viewModel.setIsServiceRunning(false);
                }
            }
        }
    };
    NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setContentView(binding.getRoot());
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        setupStatusBar();
    }
    private void setupStatusBar() {
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isLightMode = nightMode == Configuration.UI_MODE_NIGHT_NO;
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
    }
    // 2. Đăng ký lắng nghe khi màn hình hiện lên
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BatteryMonitorService.ACTION_SERVICE_STOPPED);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(serviceStatusReceiver);
        } catch (IllegalArgumentException e) {
            // Bỏ qua nếu chưa đăng ký
        }
    }
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//
//        // Nếu màn hình vừa được active lại (ví dụ: vừa đóng thanh thông báo, hoặc vừa mở app lên)
//        if (hasFocus) {
//            // Gọi hàm kiểm tra service trong ViewModel
//            // Hàm này sẽ tự update LiveData -> Tự update cái Switch của bạn
//            if (viewModel != null) {
//                viewModel.checkServiceStatus();
//            }
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.onPermissionResult(true);
            } else {
                viewModel.onPermissionResult(false);
            }
        }
    }

}
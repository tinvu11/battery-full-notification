package com.base.baterynotification.ui.main;
import static android.view.View.GONE;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.base.baterynotification.data.model.BatteryInfo;
import com.base.baterynotification.databinding.ItemInfoCardBinding;
import com.base.baterynotification.utils.BatteryCircleView;
import com.base.baterynotification.R;
import com.base.baterynotification.databinding.FragmentHomeBinding;
import com.base.baterynotification.utils.BatteryMonitorService;
import com.base.baterynotification.utils.PremiumManager;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HomeFragment extends Fragment  {
    private FragmentHomeBinding binding;
    private MainViewModel viewModel;
    private SharedPreferences sharedPreferences;
    private String timeRemind ="";
    private InterstitialAd mInterstitialAd;



    // Sử dụng ActivityResultLauncher để yêu cầu quyền
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    viewModel.onPermissionResult(true);
                } else {
                    viewModel.onPermissionResult(false);
                }
            });


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPreferences = requireContext().getSharedPreferences("BatteryAppPrefs", Context.MODE_PRIVATE);
        PremiumManager.getInstance(requireContext()).getPremiumStatus().observe(getViewLifecycleOwner(), isPremium -> {
            if (isPremium) {
                binding.imgPremium.setVisibility(GONE);
            }
        });
        setupBatteryCircleView();
        setupSwitchSeek();
        setupViewModelObservers();
        setupClickListeners();
        loadInterstitialAd();
        setupAds();

    }

    // Hiển thị khi kill app rồi vào lại
    public void setupSwitchSeek() {
        int targetLevel = sharedPreferences.getInt("target_level_alert", 80);
        binding.sliderPercent.setProgress(targetLevel);
        binding.tvPercent.setText(targetLevel + "%");
        binding.batteryCircle.setSatelliteProgress(targetLevel);
        viewModel.setTargetBatteryLevel(targetLevel);
    }

    private void setupBatteryCircleView() {
        BatteryCircleView batteryCircle = binding.getRoot().findViewById(R.id.batteryCircle);

        if (batteryCircle != null) {
            batteryCircle.setBatteryProgress(48);
            batteryCircle.setChargingState(true, "~ 78 phút nữa đầy");
            batteryCircle.setShowSatelliteDot(true);


            // Lấy progress từ slider nếu có
            if (binding.sliderPercent != null) {
                int progress = binding.sliderPercent.getProgress();
                batteryCircle.setSatelliteProgress(progress);
            }
        }
    }

    private void setupViewModelObservers() {
        // Observe status message
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                // Update UI với status message
                // Có thể hiển thị trong TextView hoặc Toast
            }
        });

        // Observe target battery level
        viewModel.getTargetBatteryLevel().observe(getViewLifecycleOwner(), level -> {
            if (level != null && binding.sliderPercent != null) {
                binding.sliderPercent.setProgress(level);
                binding.tvPercent.setText(level + "%");
                sharedPreferences.edit().putInt("target_level_alert", level).apply();
                BatteryCircleView batteryCircle = binding.getRoot().findViewById(R.id.batteryCircle);
                if (batteryCircle != null) {
                    batteryCircle.setSatelliteProgress(level);
                }
            }
        });

        // Observe service running state
        viewModel.getIsServiceRunning().observe(getViewLifecycleOwner(), isRunning -> {
            if (binding.switchAlert != null) {
                binding.switchAlert.setChecked(isRunning);
            }
            else {
                binding.switchAlert.setChecked(false);
            }
        });

        viewModel.getRequestPermissionEvent().observe(getViewLifecycleOwner(), event -> {
            Boolean shouldRequest = event.getContentIfNotHandled();
            if (shouldRequest != null && shouldRequest) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        });
        viewModel.getToastMessageEvent().observe(getViewLifecycleOwner(), event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        BatteryCircleView batteryCircle = binding.getRoot().findViewById(R.id.batteryCircle);
        ItemInfoCardBinding tempBinding = ItemInfoCardBinding.bind(binding.getRoot().findViewById(R.id.cvTemp));
        ItemInfoCardBinding lifeBinding = ItemInfoCardBinding.bind(binding.getRoot().findViewById(R.id.cvLife));
        ItemInfoCardBinding voltBinding = ItemInfoCardBinding.bind(binding.getRoot().findViewById(R.id.cvVolt));
        ItemInfoCardBinding techBinding = ItemInfoCardBinding.bind(binding.getRoot().findViewById(R.id.cvTech));

        tempBinding.iconInfo.setImageResource(R.drawable.ic_temp);
        lifeBinding.iconInfo.setImageResource(R.drawable.ic_heart_rate);
        voltBinding.iconInfo.setImageResource(R.drawable.ic_lightning);
        techBinding.iconInfo.setImageResource(R.drawable.ic_battery_low);

        tempBinding.iconInfo.setColorFilter(ContextCompat.getColor(requireContext(), R.color.orange));
        lifeBinding.iconInfo.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue));
        voltBinding.iconInfo.setColorFilter(ContextCompat.getColor(requireContext(), R.color.purple));
        techBinding.iconInfo.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green));

        tempBinding.cardImgInfo.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.lightOrange));
        lifeBinding.cardImgInfo.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.lightBlue));
        voltBinding.cardImgInfo.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.lightPurple));
        techBinding.cardImgInfo.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.lightGreen));

        viewModel.getRealtimeBatteryInfo().observe(getViewLifecycleOwner(), info -> {
            if(info == null) return;
            tempBinding.tvLabel.setText("Nhiệt độ");
            tempBinding.tvValue.setText(info.temperature + "°C");
            lifeBinding.tvLabel.setText("Sức khoẻ");
            lifeBinding.tvValue.setText(info.health);
            voltBinding.tvLabel.setText("Điện áp");
            voltBinding.tvValue.setText(Math.round(info.voltage * 10) / 10f + "V");
            techBinding.tvLabel.setText("Công nghệ");
            techBinding.tvValue.setText(info.technology);

            if (batteryCircle != null) {
                batteryCircle.setBatteryProgress(info.batteryPercent);
                if(info.timeRemaining > 0){
                    timeRemind = "";
                }else{
                    timeRemind = "";
                }
                batteryCircle.setChargingState(info.isCharging, timeRemind);
                batteryCircle.setShowSatelliteDot(true);
            }
        });
    }
    public  String formatTimeFromMillis(long millis) {
        long minutes = millis / 1000 / 60;

        if (minutes < 60) {
            return minutes + " phút";
        } else {
            long hours = minutes / 60;
            long remainMinutes = minutes % 60;
            return hours + " giờ " + remainMinutes + " phút";
        }
    }

    private void setupClickListeners() {
        binding.sliderPercent.setOnSeekBarChangeListener(
                new android.widget.SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(android.widget.SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        if (fromUser) {
                            viewModel.setTargetBatteryLevel(progress);
                        }
                    }
                    @Override
                    public void onStartTrackingTouch(android.widget.SeekBar seekBar) {

                    }
                    @Override
                    public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
                }
            );
        binding.switchAlert.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return true;
            }
        });
        binding.imgPremium.setOnClickListener(v -> {
            showPremiumDialog();
        });


        binding.switchAlert.setOnClickListener(view -> {
            boolean isChecked = binding.switchAlert.isChecked();
            if (isChecked) {
                BatteryInfo info = viewModel.getRealtimeBatteryInfo().getValue();
                int targetLevel = viewModel.getTargetBatteryLevel().getValue() != null
                        ? viewModel.getTargetBatteryLevel().getValue() : 100;
                 if (info == null || info.batteryPercent < targetLevel) {
                    viewModel.checkPermissionAndStartService();
                } else {
                    binding.switchAlert.setChecked(false);
                    Toast.makeText(requireContext(),
                            "Pin hiện tại (" + info.batteryPercent + "%) đã cao hơn mức cài đặt!",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                viewModel.stopMonitoring();
            }
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(requireActivity());
                    loadInterstitialAd();
                }
        });


        binding.icSetting.setOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(HomeFragment.this)
                        .navigate(R.id.action_homeFragment_to_settingFragment);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setupAds() {
        PremiumManager.getInstance(requireContext())
                .getPremiumStatus()
                .observe(getViewLifecycleOwner(), isPremium -> {
                    if (Boolean.TRUE.equals(isPremium)) {
                        binding.adView.setVisibility(GONE);
                        binding.adView.destroy();
                    } else {
                        binding.adView.setVisibility(View.VISIBLE);
                        AdRequest adRequest = new AdRequest.Builder().build();
                        binding.adView.loadAd(adRequest);
                    }
                });
    }
    private void loadInterstitialAd() {

        PremiumManager.getInstance(requireContext()).getPremiumStatus().observe(getViewLifecycleOwner(), isPremium -> {
            if (isPremium) {
                mInterstitialAd = null; // Đảm bảo biến này rỗng
                return;
            }
            AdRequest adRequest = new AdRequest.Builder().build();

            // ID TEST cho Interstitial: ca-app-pub-3940256099942544/1033173712
            InterstitialAd.load(requireContext(), "ca-app-pub-3940256099942544/1033173712", adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            mInterstitialAd = interstitialAd;
                            // Cài đặt sự kiện khi quảng cáo được tắt
                            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    // load thêm quảng cáo khác khi tắt hoặc mở lại vì không có thì no sẽ chỉ hiển thị 1 quảng cáo 1 lần
                                    loadInterstitialAd();
                                }
                            });
                        }
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            mInterstitialAd = null;
                        }
                    });
        });
    }
    private void showPremiumDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_premium, null);
        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();
        setupPremiumDialogClickListeners(dialogView, dialog);
        dialog.show();
    }
    private void setupPremiumDialogClickListeners(View dialogView, Dialog dialog) {
        MaterialCardView cvMonth = dialogView.findViewById(R.id.cvMonthNoAds);
        MaterialCardView cvLifetime = dialogView.findViewById(R.id.cvLifetimeNoAds);

        cvMonth.setSelected(true);
        cvLifetime.setSelected(false);

        cvMonth.setOnClickListener(v -> {
            if (!cvMonth.isSelected()) {
                cvMonth.setSelected(true);
                cvLifetime.setSelected(false);
        }
        });

        cvLifetime.setOnClickListener(v -> {
            if (!cvLifetime.isSelected()) {
                cvLifetime.setSelected(true);
                cvMonth.setSelected(false);
            }
        });

        dialogView.findViewById(R.id.tvRestore).setOnClickListener(v -> {
            dialog.dismiss();
        });

        // 6. (Nên có) Xử lý nút "Tiếp tục" / "Thanh toán"
        // Đây mới là lúc thực sự xử lý mua hàng và tắt dialog
    /*
    btnContinue.setOnClickListener(v -> {
        if (cvMonth.isSelected()) {
             // Mua gói tháng...
        } else {
             // Mua gói trọn đời...
        }
        dialog.dismiss();
    });
    */
    }


    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshBatteryData();
        viewModel.checkServiceStatus();
    }

}

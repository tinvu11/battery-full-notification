package com.base.baterynotification.ui.setting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.base.baterynotification.R;
import com.base.baterynotification.databinding.FragmentSettingBinding;

public class SettingFragment extends Fragment {
    private SharedPreferences sharedPreferences;
    private FragmentSettingBinding binding;
    private NavController navController;
    private final ActivityResultLauncher<Intent> pickRingtoneLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // Lấy URI của bài nhạc được chọn
                    Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

                    if (uri != null) {
                        // Lưu đường dẫn (String) vào SharedPreferences
                        sharedPreferences.edit().putString("ALARM_URI", uri.toString()).apply();
                        binding.tvAlert.setText(getRingtoneTitle(uri));
                        // (Tuỳ chọn) Cập nhật tên bài hát lên màn hình
                        Toast.makeText(requireContext(), "Đã chọn nhạc chuông mới", Toast.LENGTH_SHORT).show();
                    } else {
                        // Trường hợp chọn "Silent" (Im lặng) -> Xóa chuông cũ hoặc xử lý tùy ý
                        sharedPreferences.edit().putString("ALARM_URI", "").apply();
                    }
                }
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingBinding.inflate(inflater, container, false);
        sharedPreferences = requireContext().getSharedPreferences("BatteryAppPrefs", Context.MODE_PRIVATE);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        binding.llSoundAlert.setOnClickListener(v -> {
            openRingtonePicker();
        });
        binding.toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        String a = sharedPreferences.getString("ALARM_URI", null);
        if (a == null) {
            binding.tvAlert.setText("Mặc định");
            return;
        }
       binding.tvAlert.setText(getRingtoneTitle(Uri.parse(a)));
    }

    private String getRingtoneTitle(Uri uri) {
        Ringtone ringtone = RingtoneManager.getRingtone(requireContext(), uri);
        return ringtone.getTitle(requireContext());
    }

    private void openRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);

        // Chỉ hiện nhạc chuông báo thức
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        // Hiện tiêu đề
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chọn âm báo pin đầy");

        // Cho phép chọn "Silent" (Im lặng) hay không? (true/false)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);

        // Hiển thị bài nhạc đang được chọn hiện tại (nếu có)
        String currentUriStr = sharedPreferences.getString("ALARM_URI", null);
        if (currentUriStr != null && !currentUriStr.isEmpty()) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentUriStr));
        } else {
            // Mặc định chọn bài đầu tiên nếu chưa lưu gì
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
        }
        pickRingtoneLauncher.launch(intent);
    }
}
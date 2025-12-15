package com.base.baterynotification.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class PremiumManager {

    private static final String PREFS_NAME = "premium_prefs";
    private static final String KEY_IS_PREMIUM = "is_premium_user";

    private static PremiumManager instance;
    private final SharedPreferences prefs;

    // LiveData để toàn bộ app tự động cập nhật giao diện khi mua thành công
    private final MutableLiveData<Boolean> isPremiumLiveData = new MutableLiveData<>();

    private PremiumManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Khởi tạo giá trị ban đầu
        isPremiumLiveData.setValue(prefs.getBoolean(KEY_IS_PREMIUM, true));
    }

    public static synchronized PremiumManager getInstance(Context context) {
        if (instance == null) {
            instance = new PremiumManager(context);
        }
        return instance;
    }

    // 1. Kiểm tra trạng thái (Dùng để ẩn hiện tính năng)
    public boolean isPremium() {
        return prefs.getBoolean(KEY_IS_PREMIUM, false);
    }

    // 2. Lắng nghe trạng thái (Dùng để cập nhật UI realtime)
    public LiveData<Boolean> getPremiumStatus() {
        return isPremiumLiveData;
    }

    // 3. Mua gói (Mock)
    public void setPremium(boolean isPremium) {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply();
        isPremiumLiveData.postValue(isPremium); // Báo cho toàn bộ app biết
    }
}

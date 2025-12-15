package com.base.baterynotification.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class BatteryCircleView extends View {

    // --- MÀU SẮC ---
    private final int COLOR_GREEN = Color.parseColor("#2ECC71");
    private final int COLOR_BG_RING = Color.parseColor("#ECF0F1");
    private final int COLOR_TEXT_PRIMARY = COLOR_GREEN;
    private final int COLOR_TEXT_SECONDARY = Color.parseColor("#95A5A6");
    private final int COLOR_DOT = Color.parseColor("#4A90E2");

    // --- KÍCH THƯỚC (Sẽ được convert sang pixel) ---
    private float STROKE_WIDTH_DP = 15f;    // Độ dày vòng pin
    private float TEXT_SIZE_PERCENT_SP = 48f;
    private float TEXT_SIZE_SMALL_SP = 14f;
    private float DOT_RADIUS_DP = 6f;       // Bán kính chấm tròn
    private float GAP_DP = 16f;             // Khoảng cách yêu cầu 16dp

    // Biến lưu giá trị pixel sau khi convert
    private float strokeWidthPx;
    private float dotRadiusPx;
    private float gapPx;

    // --- PAINTS & OBJECTS ---
    private Paint bgArcPaint;
    private Paint progressArcPaint;
    private Paint percentTextPaint;
    private Paint statusTextPaint;
    private Paint timeTextPaint;
    private Paint dotPaint;

    private RectF arcRect = new RectF();
    private Drawable chargingIcon;
    private Rect textBounds = new Rect();

    // --- TRẠNG THÁI ---
    private int batteryProgress = 0;
    private int satelliteProgress = 0;
    private boolean isCharging = false;
    private String timeRemainingText = "";
    private boolean showSatelliteDot = false;

    public BatteryCircleView(Context context) {
        super(context);
        init(context);
    }

    public BatteryCircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // Convert kích thước từ DP/SP sang Pixel
        strokeWidthPx = dpToPx(STROKE_WIDTH_DP);
        dotRadiusPx = dpToPx(DOT_RADIUS_DP);
        gapPx = dpToPx(GAP_DP);
        float textSizePercent = spToPx(TEXT_SIZE_PERCENT_SP);
        float textSizeSmall = spToPx(TEXT_SIZE_SMALL_SP);

        // 1. Paint nền
        bgArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgArcPaint.setColor(COLOR_BG_RING);
        bgArcPaint.setStyle(Paint.Style.STROKE);
        bgArcPaint.setStrokeWidth(strokeWidthPx);
        bgArcPaint.setStrokeCap(Paint.Cap.ROUND);

        // 2. Paint pin
        progressArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressArcPaint.setColor(COLOR_GREEN);
        progressArcPaint.setStyle(Paint.Style.STROKE);
        progressArcPaint.setStrokeWidth(strokeWidthPx);
        progressArcPaint.setStrokeCap(Paint.Cap.ROUND);

        // 3. Paint chữ % (Canh giữa)
        percentTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        percentTextPaint.setColor(COLOR_TEXT_PRIMARY);
        percentTextPaint.setTextSize(textSizePercent);
        percentTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        percentTextPaint.setTextAlign(Paint.Align.CENTER);

        // 4. Paint chữ trạng thái (Sạc)
        statusTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        statusTextPaint.setColor(COLOR_GREEN);
        statusTextPaint.setTextSize(textSizeSmall);
        statusTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        statusTextPaint.setTextAlign(Paint.Align.LEFT); // Canh trái để vẽ cùng icon

        // 5. Paint chữ thời gian
        timeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timeTextPaint.setColor(COLOR_TEXT_SECONDARY);
        timeTextPaint.setTextSize(textSizeSmall);
        timeTextPaint.setTextAlign(Paint.Align.CENTER);

        // 6. Paint chấm tròn
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(COLOR_DOT);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setShadowLayer(8f, 0, 4f, Color.parseColor("#664A90E2"));
        setLayerType(LAYER_TYPE_SOFTWARE, dotPaint);

        // Icon tia sét
        chargingIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_camera); // Thay icon sạc của bạn vào đây
        if (chargingIcon != null) {
            chargingIcon.setTint(COLOR_GREEN);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Tính toán khoảng cách cần thiết để chấm tròn không bị cắt
        // Padding = Nửa nét vẽ + Khoảng cách 16dp + Đường kính chấm tròn (2 * radius)
        float paddingNeeded = (strokeWidthPx / 2f) + gapPx + (dotRadiusPx * 2) + 5f; // +5f dư ra một chút cho bóng đổ

        int size = Math.min(w, h);
        float radius = (size / 2f) - paddingNeeded;

        // Cập nhật hình chữ nhật chứa vòng pin
        arcRect.set(
                w / 2f - radius,
                h / 2f - radius,
                w / 2f + radius,
                h / 2f + radius
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // 1. Vẽ vòng nền
        canvas.drawArc(arcRect, 0, 360, false, bgArcPaint);

        // 2. Vẽ vòng pin
        float sweepAngle = (360f * batteryProgress / 100f);
        canvas.drawArc(arcRect, -90, sweepAngle, false, progressArcPaint);

        // 3. Vẽ chữ căn chỉnh
        drawCenteredContent(canvas, centerX, centerY);

        // 4. Vẽ chấm tròn vệ tinh (Nếu bật)
        if (showSatelliteDot) {
            drawSatelliteDot(canvas, centerX, centerY);
        }
    }

    private void drawCenteredContent(Canvas canvas, float cx, float cy) {
        // --- A. VẼ PHẦN TRĂM (CHÍNH GIỮA) ---
        String percentStr = batteryProgress + "%";

        // Lấy thông số font để căn giữa theo chiều dọc chuẩn xác
        Paint.FontMetrics percentMetrics = percentTextPaint.getFontMetrics();
        // Công thức căn giữa vertical: Y = CenterY - (Descent + Ascent) / 2
        float percentYOffset = (percentMetrics.descent + percentMetrics.ascent) / 2f;
        float percentBaseLineY = cy - percentYOffset;

        canvas.drawText(percentStr, cx, percentBaseLineY, percentTextPaint);

        // Nếu đang sạc thì vẽ thêm chữ trên và dưới
        if (isCharging) {
            // --- B. VẼ TRẠNG THÁI (BÊN TRÊN - CÁCH 16DP) ---
            // Tính toán vị trí Y: Đỉnh của chữ % - 16dp - Chiều cao chữ status (phần descent)
            // Top của chữ % = percentBaseLineY + percentMetrics.ascent (ascent là số âm)
            float percentTopY = percentBaseLineY + percentMetrics.ascent;

            String statusStr = "ĐANG SẠC";
            Paint.FontMetrics statusMetrics = statusTextPaint.getFontMetrics();

            // Y vẽ (baseline) của status = Top của % - 16dp - (phần dưới của chữ status)
            float statusBaseLineY = percentTopY - 8 - statusMetrics.descent;

            // Xử lý vẽ icon + text
            float iconSize = statusTextPaint.getTextSize(); // Icon to bằng chữ
            float textWidth = statusTextPaint.measureText(statusStr);
            float iconPadding = dpToPx(4); // Khoảng cách giữa icon và chữ
            float totalWidth = iconSize + iconPadding + textWidth;
            float startX = cx - (totalWidth / 2f);

            if (chargingIcon != null) {
                chargingIcon.setBounds(
                        (int) startX,
                        (int) (statusBaseLineY - iconSize),
                        (int) (startX + iconSize),
                        (int) statusBaseLineY
                );
                chargingIcon.draw(canvas);
            }
            canvas.drawText(statusStr, startX + iconSize + iconPadding, statusBaseLineY, statusTextPaint);


            // --- C. VẼ THỜI GIAN (BÊN DƯỚI - CÁCH 16DP) ---
            // Đáy của chữ % = percentBaseLineY + percentMetrics.descent
            float percentBottomY = percentBaseLineY + percentMetrics.descent;

            Paint.FontMetrics timeMetrics = timeTextPaint.getFontMetrics();
            // Y vẽ (baseline) của time = Đáy của % + 16dp + (khoảng cách từ đỉnh đến baseline của time - tức là -ascent)
            float timeBaseLineY = percentBottomY + 8 - timeMetrics.ascent;

            canvas.drawText(timeRemainingText, cx, timeBaseLineY, timeTextPaint);
        }
    }

    private void drawSatelliteDot(Canvas canvas, float cx, float cy) {
        float angleDegrees = -90f + (360f * satelliteProgress / 100f);
        double angleRad = Math.toRadians(angleDegrees);

        // Bán kính quỹ đạo = Bán kính vòng pin (tính đến mép ngoài) + 16dp + Bán kính chấm
        // arcRect.width()/2 là bán kính tính đến tâm nét vẽ
        float ringOuterRadius = (arcRect.width() / 2f) + (strokeWidthPx / 2f);

        // Khoảng cách từ mép ngoài vòng pin đến tâm chấm tròn
        float orbitRadius = ringOuterRadius + gapPx + dotRadiusPx;

        float dotX = cx + (float) (orbitRadius * Math.cos(angleRad));
        float dotY = cy + (float) (orbitRadius * Math.sin(angleRad));

        canvas.drawCircle(dotX, dotY, dotRadiusPx, dotPaint);
    }

    // --- UTILS ---
    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().getDisplayMetrics());
    }

    // --- SETTER ---
    public void setBatteryProgress(int progress) {
        this.batteryProgress = progress;
        invalidate();
    }

    public void setSatelliteProgress(int progress) {
        this.satelliteProgress = progress;
        invalidate();
    }

    public void setChargingState(boolean isCharging, String timeRemainingText) {
        this.isCharging = isCharging;
        this.timeRemainingText = timeRemainingText;
        invalidate();
    }

    public void setShowSatelliteDot(boolean show) {
        this.showSatelliteDot = show;
        invalidate();
    }
}
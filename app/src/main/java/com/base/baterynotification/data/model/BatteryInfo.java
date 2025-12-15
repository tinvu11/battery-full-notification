package com.base.baterynotification.data.model;


public class BatteryInfo {
    public float temperature; // Nhiệt độ
    public float voltage;     // Điện áp
    public String health;     // Sức khỏe

    public boolean isCharging;
    public int batteryPercent;
    public String technology;
    public long timeRemaining;
    public long chargeCounter;




    public BatteryInfo(float temperature, float voltage, String health,  boolean isCharging, int batteryPercent, String technology,  long timeRemaining, long chargeCounter) {
        this.temperature = temperature;
        this.voltage = voltage;
        this.health = health;
        this.isCharging = isCharging;
        this.batteryPercent = batteryPercent;
        this.technology = technology;
        this.timeRemaining = timeRemaining;
        this.chargeCounter = chargeCounter;
    }
}

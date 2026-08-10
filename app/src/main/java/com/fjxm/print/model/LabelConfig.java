package com.fjxm.print.model;

import android.content.Context;
import android.content.SharedPreferences;

public final class LabelConfig {
    private static final String PREFS = "label_config";

    public float widthMm = 50f;
    public float heightMm = 40f;
    public float gapMm = 2f;
    public float horizontalMarginMm = 1.5f;
    public int dpi = 203;
    public int density = 8;
    public int speed = 3;
    public String operator = "";

    public int widthDots() {
        int value = Math.round(widthMm * dpi / 25.4f);
        return Math.max(160, Math.min(800, value));
    }

    public int heightDots() {
        int value = Math.round(heightMm * dpi / 25.4f);
        return Math.max(120, Math.min(640, value));
    }

    public int horizontalMarginDots() {
        return Math.max(4, Math.round(horizontalMarginMm * dpi / 25.4f));
    }

    public static LabelConfig load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        LabelConfig config = new LabelConfig();
        config.widthMm = prefs.getFloat("width_mm", config.widthMm);
        config.heightMm = prefs.getFloat("height_mm", config.heightMm);
        config.gapMm = prefs.getFloat("gap_mm", config.gapMm);
        config.horizontalMarginMm = prefs.getFloat("margin_mm", config.horizontalMarginMm);
        config.operator = prefs.getString("operator", "");
        return config;
    }

    public void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putFloat("width_mm", widthMm)
                .putFloat("height_mm", heightMm)
                .putFloat("gap_mm", gapMm)
                .putFloat("margin_mm", horizontalMarginMm)
                .putString("operator", operator)
                .apply();
    }

    public static void reset(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}

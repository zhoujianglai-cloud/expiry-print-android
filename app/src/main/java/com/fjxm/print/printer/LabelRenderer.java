package com.fjxm.print.printer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.fjxm.print.data.MaterialEntity;
import com.fjxm.print.model.LabelConfig;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class LabelRenderer {
    private LabelRenderer() {}

    public static Bitmap render(MaterialEntity item, int storageType, int durationHours,
                                long startMillis, LabelConfig config) {
        int width = config.widthDots();
        int height = config.heightDots();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        float marginX = config.horizontalMarginDots();
        float marginY = Math.max(6f, height * 0.025f);
        float left = marginX;
        float top = marginY;
        float right = width - marginX;
        float bottom = height - marginY;
        float titleBottom = top + (bottom - top) * 0.22f;
        float storageBottom = top + (bottom - top) * 0.40f;

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(Color.BLACK);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Math.max(1.6f, width / 210f));
        float horizontalInset = Math.max(6f, width * 0.02f);
        canvas.drawLine(left + horizontalInset, titleBottom,
                right - horizontalInset, titleBottom, line);
        canvas.drawLine(left + horizontalInset, storageBottom,
                right - horizontalInset, storageBottom, line);

        String product = item == null ? "测试标签" : item.product;
        drawCenteredFit(canvas, product, (left + right) / 2f, (top + titleBottom) / 2f,
                right - left - 28f, Math.max(24f, width * 0.072f), true);

        float colWidth = (right - left) / 3f;
        String[] storageNames = {"冷藏", "冷冻", "常温"};
        for (int i = 0; i < 3; i++) {
            float cellLeft = left + colWidth * i;
            float centerX = cellLeft + colWidth / 2f;
            float radius = Math.min(10f, colWidth * 0.08f);
            float centerY = (titleBottom + storageBottom) / 2f;
            Paint storagePaint = textPaint(Math.max(19f, width * 0.057f), true);
            float textWidth = storagePaint.measureText(storageNames[i]);
            float gap = Math.max(7f, width * 0.018f);
            float groupWidth = radius * 2f + gap + textWidth;
            float circleX = centerX - groupWidth / 2f + radius;
            drawRadio(canvas, circleX, centerY, radius, storageType == i + 1, line);
            drawTextCenteredY(canvas, storageNames[i], circleX + radius + gap, centerY,
                    storagePaint.getTextSize(), true);
        }

        canvas.drawLine(left + colWidth, storageBottom, left + colWidth, bottom, line);
        canvas.drawLine(left + colWidth * 2f, storageBottom, left + colWidth * 2f, bottom, line);

        long endMillis = startMillis + Math.max(0, durationHours) * 60L * 60L * 1000L;
        drawDateCell(canvas, left, storageBottom, colWidth, bottom - storageBottom, startMillis);
        drawDateCell(canvas, left + colWidth, storageBottom,
                colWidth, bottom - storageBottom, endMillis);
        if (config.operator != null && !config.operator.trim().isEmpty()) {
            drawCenteredFit(canvas, config.operator.trim(), left + colWidth * 2.5f,
                    storageBottom + (bottom - storageBottom) * 0.63f,
                    colWidth - 18f, Math.max(20f, colWidth * 0.20f), true);
        }
        return bitmap;
    }

    private static void drawDateCell(Canvas canvas, float left, float top, float width, float height,
                                     long time) {
        float centerX = left + width / 2f;
        String[] lines = formatDateLines(time);
        float size = Math.max(20f, width * 0.20f);
        drawCenteredFit(canvas, lines[0], centerX, top + height * 0.45f,
                width - 16f, size, true);
        drawCenteredFit(canvas, lines[1], centerX, top + height * 0.63f,
                width - 16f, size, true);
        drawCenteredFit(canvas, lines[2], centerX, top + height * 0.81f,
                width - 16f, size, true);
    }

    static String[] formatDateLines(long time) {
        Date date = new Date(time);
        return new String[] {
                new SimpleDateFormat("yyyy年", Locale.CHINA).format(date),
                new SimpleDateFormat("MM月dd日", Locale.CHINA).format(date),
                new SimpleDateFormat("HH:mm", Locale.CHINA).format(date)
        };
    }

    private static void drawRadio(Canvas canvas, float centerX, float centerY, float radius,
                                  boolean selected, Paint paint) {
        Paint radio = new Paint(paint);
        radio.setStrokeWidth(Math.max(2f, radius * 0.22f));
        canvas.drawCircle(centerX, centerY, radius, radio);
        if (selected) {
            radio.setStyle(Paint.Style.FILL);
            canvas.drawCircle(centerX, centerY, radius * 0.46f, radio);
        }
    }

    private static void drawCenteredFit(Canvas canvas, String text, float centerX, float centerY,
                                        float maxWidth, float desiredSize, boolean bold) {
        Paint paint = textPaint(desiredSize, bold);
        while (paint.measureText(text) > maxWidth && paint.getTextSize() > 12f) {
            paint.setTextSize(paint.getTextSize() - 1f);
        }
        float baseline = centerY - (paint.ascent() + paint.descent()) / 2f;
        canvas.drawText(text, centerX - paint.measureText(text) / 2f, baseline, paint);
    }

    private static void drawTextCenteredY(Canvas canvas, String text, float x, float centerY,
                                          float textSize, boolean bold) {
        Paint paint = textPaint(textSize, bold);
        float baseline = centerY - (paint.ascent() + paint.descent()) / 2f;
        canvas.drawText(text, x, baseline, paint);
    }

    private static Paint textPaint(float size, boolean bold) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        paint.setTextSize(size);
        return paint;
    }
}

package com.fjxm.print.printer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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
        float titleBottom = top + (bottom - top) * 0.21f;
        float storageBottom = top + (bottom - top) * 0.43f;

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(Color.BLACK);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Math.max(2f, width / 180f));
        canvas.drawRect(new RectF(left, top, right, bottom), line);
        canvas.drawLine(left, titleBottom, right, titleBottom, line);
        canvas.drawLine(left, storageBottom, right, storageBottom, line);

        String product = item == null ? "测试标签" : item.product;
        drawCenteredFit(canvas, product, (left + right) / 2f, (top + titleBottom) / 2f,
                right - left - 20f, Math.max(22f, width * 0.075f), true);

        float colWidth = (right - left) / 3f;
        String[] storageNames = {"冷藏", "冷冻", "常温"};
        for (int i = 0; i < 3; i++) {
            float cellLeft = left + colWidth * i;
            float centerX = cellLeft + colWidth / 2f;
            float box = Math.min(24f, colWidth * 0.18f);
            float groupWidth = box + 6f + Math.min(colWidth * 0.52f, 48f);
            float boxLeft = centerX - groupWidth / 2f;
            float centerY = (titleBottom + storageBottom) / 2f;
            canvas.drawRect(boxLeft, centerY - box / 2f, boxLeft + box, centerY + box / 2f, line);
            if (storageType == i + 1) drawCheck(canvas, boxLeft, centerY - box / 2f, box, line);
            drawTextCenteredY(canvas, storageNames[i], boxLeft + box + 6f, centerY,
                    Math.max(17f, width * 0.052f), false);
        }

        canvas.drawLine(left + colWidth, storageBottom, left + colWidth, bottom, line);
        canvas.drawLine(left + colWidth * 2f, storageBottom, left + colWidth * 2f, bottom, line);

        long endMillis = startMillis + Math.max(0, durationHours) * 60L * 60L * 1000L;
        drawDateCell(canvas, left, storageBottom, colWidth, bottom - storageBottom,
                "制作时间", startMillis, null);
        drawDateCell(canvas, left + colWidth, storageBottom, colWidth, bottom - storageBottom,
                "有效时间", endMillis, null);
        drawDateCell(canvas, left + colWidth * 2f, storageBottom, colWidth, bottom - storageBottom,
                "操作人", 0L, config.operator);
        return bitmap;
    }

    private static void drawDateCell(Canvas canvas, float left, float top, float width, float height,
                                     String label, long time, String customText) {
        float centerX = left + width / 2f;
        drawCenteredFit(canvas, label, centerX, top + height * 0.18f, width - 10f,
                Math.max(14f, width * 0.14f), false);
        if (customText != null) {
            drawCenteredFit(canvas, customText.isEmpty() ? "—" : customText,
                    centerX, top + height * 0.58f, width - 12f,
                    Math.max(18f, width * 0.19f), false);
            return;
        }
        Date date = new Date(time);
        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(date);
        String clock = new SimpleDateFormat("HH:mm", Locale.CHINA).format(date);
        drawCenteredFit(canvas, day, centerX, top + height * 0.52f, width - 10f,
                Math.max(17f, width * 0.18f), false);
        drawCenteredFit(canvas, clock, centerX, top + height * 0.78f, width - 10f,
                Math.max(19f, width * 0.21f), false);
    }

    private static void drawCheck(Canvas canvas, float x, float y, float size, Paint paint) {
        Paint check = new Paint(paint);
        check.setStrokeWidth(Math.max(3f, size * 0.15f));
        check.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x + size * 0.18f, y + size * 0.52f,
                x + size * 0.42f, y + size * 0.76f, check);
        canvas.drawLine(x + size * 0.42f, y + size * 0.76f,
                x + size * 0.84f, y + size * 0.24f, check);
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

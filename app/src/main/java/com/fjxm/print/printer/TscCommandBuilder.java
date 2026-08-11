package com.fjxm.print.printer;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.fjxm.print.model.LabelConfig;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public final class TscCommandBuilder {
    private TscCommandBuilder() {}

    public static byte[] build(Bitmap bitmap, LabelConfig config) {
        int rowBytes = (bitmap.getWidth() + 7) / 8;
        byte[] pixels = toMonochrome(bitmap, rowBytes);
        String header = String.format(Locale.US,
                "SIZE %.1f mm,%.1f mm\r\nGAP %.1f mm,0 mm\r\nDIRECTION 1\r\nDENSITY %d\r\nSPEED %d\r\nCLS\r\nBITMAP 0,0,%d,%d,0,",
                config.widthMm, config.heightMm, config.gapMm,
                config.density, config.speed, rowBytes, bitmap.getHeight());
        ByteArrayOutputStream output = new ByteArrayOutputStream(header.length() + pixels.length + 32);
        byte[] headerBytes = header.getBytes(StandardCharsets.US_ASCII);
        byte[] footerBytes = "\r\nPRINT 1,1\r\n".getBytes(StandardCharsets.US_ASCII);
        output.write(headerBytes, 0, headerBytes.length);
        output.write(pixels, 0, pixels.length);
        output.write(footerBytes, 0, footerBytes.length);
        return output.toByteArray();
    }

    private static byte[] toMonochrome(Bitmap bitmap, int rowBytes) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] row = new int[width];
        byte[] result = new byte[rowBytes * height];
        // GP-M322 uses 0 for a heated (black) dot and 1 for an unprinted (white) dot.
        // Start with a fully white label, then clear only the bits that should print black.
        Arrays.fill(result, (byte) 0xFF);
        for (int y = 0; y < height; y++) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1);
            for (int x = 0; x < width; x++) {
                int color = row[x];
                int luminance = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000;
                if (luminance < 160) {
                    int index = y * rowBytes + x / 8;
                    result[index] = markBlack(result[index], x % 8);
                }
            }
        }
        return result;
    }

    static byte markBlack(byte current, int bitIndex) {
        return (byte) (current & ~(0x80 >> bitIndex));
    }
}

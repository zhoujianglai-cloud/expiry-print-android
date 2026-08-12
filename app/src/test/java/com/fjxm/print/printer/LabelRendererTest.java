package com.fjxm.print.printer;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import java.util.TimeZone;

public class LabelRendererTest {
    @Test
    public void dateUsesThreeLineChineseFormat() {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            assertArrayEquals(new String[] {"1970年", "01月01日", "08:00"},
                    LabelRenderer.formatDateLines(0L));
        } finally {
            TimeZone.setDefault(previous);
        }
    }
}

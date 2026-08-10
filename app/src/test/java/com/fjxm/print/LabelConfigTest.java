package com.fjxm.print;

import static org.junit.Assert.assertEquals;

import com.fjxm.print.model.LabelConfig;

import org.junit.Test;

public class LabelConfigTest {
    @Test
    public void gpM322DefaultsAre400By320Dots() {
        LabelConfig config = new LabelConfig();
        assertEquals(400, config.widthDots());
        assertEquals(320, config.heightDots());
    }
}

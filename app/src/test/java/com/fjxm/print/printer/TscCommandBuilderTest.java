package com.fjxm.print.printer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TscCommandBuilderTest {
    @Test
    public void gpM322UsesZeroBitsForBlackPrintedDots() {
        assertEquals((byte) 0x7F, TscCommandBuilder.markBlack((byte) 0xFF, 0));
        assertEquals((byte) 0xFE, TscCommandBuilder.markBlack((byte) 0xFF, 7));
    }
}

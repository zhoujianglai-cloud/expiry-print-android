package com.fjxm.print.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CategoryLabelFormatterTest {
    @Test public void removesKitchenPrefixAndPositionSuffixTogether() {
        assertEquals("总配", CategoryLabelFormatter.format("后厨总配岗位"));
        assertEquals("意面饭", CategoryLabelFormatter.format("后厨意面饭岗位"));
        assertEquals("披萨", CategoryLabelFormatter.format("后厨披萨岗位"));
        assertEquals("中餐", CategoryLabelFormatter.format("后厨中餐岗位"));
    }

    @Test public void leavesOtherCategoryNamesUnchanged() {
        assertEquals("水吧", CategoryLabelFormatter.format("水吧"));
        assertEquals("调饮", CategoryLabelFormatter.format("调饮"));
        assertEquals("后厨", CategoryLabelFormatter.format("后厨"));
        assertNull(CategoryLabelFormatter.format(null));
    }
}

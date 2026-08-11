package com.fjxm.print;

import static org.junit.Assert.assertEquals;

import com.fjxm.print.data.CategoryEntity;
import com.fjxm.print.data.MaterialEntity;

import org.junit.Test;

public class CategoryEntityTest {
    @Test
    public void categoryCanBeAppliedToNewIngredient() {
        CategoryEntity category = new CategoryEntity();
        category.type = 9;
        category.typeName = "后厨";
        category.cateId = 23;
        category.cateName = "酱料类";

        MaterialEntity material = new MaterialEntity();
        material.applyCategory(category);

        assertEquals("后厨 · 酱料类", category.displayName());
        assertEquals(9, material.type);
        assertEquals("后厨", material.typeName);
        assertEquals(23, material.cateId);
        assertEquals("酱料类", material.cateName);
    }
}

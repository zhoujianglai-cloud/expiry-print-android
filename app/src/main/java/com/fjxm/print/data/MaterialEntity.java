package com.fjxm.print.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "materials", indices = {@Index("product"), @Index("typeName"), @Index("cateName")})
public class MaterialEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public int type;
    @NonNull public String typeName = "";
    public int cateId;
    @NonNull public String cateName = "";
    public int productId;
    @NonNull public String product = "";
    public int storeType = 1;
    public int refrigerationHours;
    public int normalHours;
    public int freezingHours;
    @NonNull public String remarks = "";

    public int durationFor(int storageType) {
        if (storageType == 2) return freezingHours;
        if (storageType == 3) return normalHours;
        return refrigerationHours;
    }

    public void setDurationFor(int storageType, int hours) {
        storeType = storageType;
        if (storageType == 2) freezingHours = hours;
        else if (storageType == 3) normalHours = hours;
        else refrigerationHours = hours;
    }

    public void applyCategory(CategoryEntity category) {
        type = category.type;
        typeName = category.typeName;
        cateId = category.cateId;
        cateName = category.cateName;
    }
}

package com.fjxm.print.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories",
        indices = {@Index(value = {"typeName", "cateName"}, unique = true)})
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public int type;
    @NonNull public String typeName = "";
    public int cateId;
    @NonNull public String cateName = "";

    public String displayName() {
        return typeName + " · " + cateName;
    }
}

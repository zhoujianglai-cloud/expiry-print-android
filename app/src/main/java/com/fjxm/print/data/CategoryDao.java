package com.fjxm.print.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type, cateId, id")
    List<CategoryEntity> getAll();

    @Query("SELECT COUNT(*) FROM categories")
    int count();

    @Query("SELECT COALESCE(MIN(type), 0) FROM categories WHERE typeName = :typeName")
    int typeForName(String typeName);

    @Query("SELECT COALESCE(MAX(type), 0) + 1 FROM categories")
    int nextType();

    @Query("SELECT COALESCE(MAX(cateId), 0) + 1 FROM categories")
    int nextCateId();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CategoryEntity category);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<CategoryEntity> categories);
}

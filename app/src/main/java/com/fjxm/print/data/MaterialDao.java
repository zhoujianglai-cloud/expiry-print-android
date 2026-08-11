package com.fjxm.print.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MaterialDao {
    @Query("SELECT * FROM materials ORDER BY type, cateId, productId, id")
    List<MaterialEntity> getAll();

    @Query("SELECT * FROM materials WHERE id = :id LIMIT 1")
    MaterialEntity getById(long id);

    @Query("SELECT * FROM materials WHERE product LIKE '%' || :keyword || '%' OR cateName LIKE '%' || :keyword || '%' OR typeName LIKE '%' || :keyword || '%' ORDER BY type, cateId, productId")
    List<MaterialEntity> search(String keyword);

    @Query("SELECT COUNT(*) FROM materials")
    int count();

    @Query("SELECT COALESCE(MAX(productId), 0) + 1 FROM materials")
    int nextProductId();

    @Insert
    long insert(MaterialEntity item);

    @Insert
    void insertAll(List<MaterialEntity> items);

    @Update
    void update(MaterialEntity item);
}

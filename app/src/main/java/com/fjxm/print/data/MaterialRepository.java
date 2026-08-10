package com.fjxm.print.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class MaterialRepository {
    private static volatile MaterialRepository instance;

    private final Context context;
    private final MaterialDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private MaterialRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dao = AppDatabase.get(context).materialDao();
    }

    public static MaterialRepository get(Context context) {
        if (instance == null) {
            synchronized (MaterialRepository.class) {
                if (instance == null) instance = new MaterialRepository(context);
            }
        }
        return instance;
    }

    public void loadAll(Consumer<List<MaterialEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            List<MaterialEntity> result = dao.getAll();
            main.post(() -> callback.accept(result));
        });
    }

    public void search(String keyword, Consumer<List<MaterialEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            List<MaterialEntity> result = dao.search(keyword.trim());
            main.post(() -> callback.accept(result));
        });
    }

    public void getById(long id, Consumer<MaterialEntity> callback) {
        executor.execute(() -> {
            ensureSeeded();
            MaterialEntity result = dao.getById(id);
            main.post(() -> callback.accept(result));
        });
    }

    public void update(MaterialEntity item, Runnable callback) {
        executor.execute(() -> {
            dao.update(item);
            if (callback != null) main.post(callback);
        });
    }

    private void ensureSeeded() {
        if (dao.count() > 0) return;
        List<MaterialEntity> migrated = readLegacyDatabase();
        if (!migrated.isEmpty()) {
            dao.insertAll(migrated);
            return;
        }
        try (InputStream input = context.getAssets().open("materials.json")) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
            JSONArray array = new JSONArray(output.toString(StandardCharsets.UTF_8.name()));
            List<MaterialEntity> items = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject source = array.getJSONObject(i);
                MaterialEntity item = new MaterialEntity();
                item.type = source.optInt("type");
                item.typeName = source.optString("typeName");
                item.cateId = source.optInt("cateId");
                item.cateName = source.optString("cateName");
                item.productId = source.optInt("productId");
                item.product = source.optString("product");
                item.storeType = source.optInt("storeType", 1);
                item.refrigerationHours = source.optInt("refrigerationHours");
                item.normalHours = source.optInt("normalHours");
                item.freezingHours = source.optInt("freezingHours");
                item.remarks = source.optString("remarks");
                items.add(item);
            }
            dao.insertAll(items);
        } catch (Exception error) {
            throw new IllegalStateException("无法导入内置模板", error);
        }
    }

    /** Imports user-edited templates when this rebuild replaces the legacy APK. */
    private List<MaterialEntity> readLegacyDatabase() {
        List<MaterialEntity> items = new ArrayList<>();
        File legacyFile = context.getDatabasePath("print.db");
        if (!legacyFile.exists()) return items;
        try (SQLiteDatabase database = SQLiteDatabase.openDatabase(
                legacyFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
             Cursor cursor = database.query("material", null, null, null, null, null,
                     "type, cateId, productId, id")) {
            while (cursor.moveToNext()) {
                MaterialEntity item = new MaterialEntity();
                item.type = integer(cursor, "type");
                item.typeName = string(cursor, "typeName");
                item.cateId = integer(cursor, "cateId");
                item.cateName = string(cursor, "cateName");
                item.productId = integer(cursor, "productId");
                item.product = string(cursor, "product");
                item.storeType = integer(cursor, "storeType");
                item.refrigerationHours = integer(cursor, "refrigerationTime");
                item.normalHours = integer(cursor, "normalTemperatureTime");
                item.freezingHours = integer(cursor, "freezingTime");
                item.remarks = string(cursor, "remarsk");
                if (!item.product.isEmpty()) items.add(item);
            }
        } catch (Exception ignored) {
            items.clear();
        }
        return items;
    }

    private static int integer(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index < 0 || cursor.isNull(index) ? 0 : cursor.getInt(index);
    }

    private static String string(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index < 0 || cursor.isNull(index) ? "" : cursor.getString(index);
    }
}

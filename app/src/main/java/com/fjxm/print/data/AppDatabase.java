package com.fjxm.print.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {MaterialEntity.class, CategoryEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `categories` "
                    + "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`type` INTEGER NOT NULL, `typeName` TEXT NOT NULL, "
                    + "`cateId` INTEGER NOT NULL, `cateName` TEXT NOT NULL)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
                    + "`index_categories_typeName_cateName` ON `categories` (`typeName`, `cateName`)");
            database.execSQL("INSERT OR IGNORE INTO `categories` (`type`, `typeName`, `cateId`, `cateName`) "
                    + "SELECT MIN(`type`), `typeName`, MIN(`cateId`), `cateName` FROM `materials` "
                    + "WHERE `typeName` <> '' AND `cateName` <> '' GROUP BY `typeName`, `cateName`");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `materials` ADD COLUMN `userCreated` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `categories` ADD COLUMN `userCreated` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE `materials` SET `userCreated` = 1 WHERE `productId` > 160");
            database.execSQL("UPDATE `categories` SET `userCreated` = 1 WHERE `cateId` > 39");
        }
    };

    public abstract MaterialDao materialDao();
    public abstract CategoryDao categoryDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "expiry_print.db"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build();
                }
            }
        }
        return instance;
    }
}

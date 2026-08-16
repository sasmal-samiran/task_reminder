package com.myreminder.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TaskEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderDurationValue INTEGER NOT NULL DEFAULT 30")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderDurationUnit TEXT NOT NULL DEFAULT 'MINUTES'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderDate INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderTime TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderIntervalMinutes INTEGER NOT NULL DEFAULT 1440")
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderDurationValue INTEGER NOT NULL DEFAULT 30")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderDurationUnit TEXT NOT NULL DEFAULT 'MINUTES'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderDate INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderTime TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderIntervalMinutes INTEGER NOT NULL DEFAULT 1440")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myreminder.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
}

package com.artuok.appwork.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DbHelper extends SQLiteOpenHelper {
    private static final int DatabaseVersion = 8;
    private static final String DatabaseName = "Calendar";
    public static final String t_subjects = "subjects";
    public static final String t_task = "task";
    public static final String t_event = "event";

    public DbHelper(@Nullable Context context) {
        super(context, DatabaseName, null, DatabaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + t_subjects + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "color INTEGER NOT NULL)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + t_task + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date DATETIME NOT NULL," +
                "title TEXT NOT NULL," +
                "end_date DATETIME NOT NULL," +
                "subject INTEGER NOT NULL," +
                "description VARCHAR(500)," +
                "status INTEGER(1) NOT NULL)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + t_event + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "day_of_week INTEGER NOT NULL," +
                "time LONG NOT NULL," +
                "duration LONG NOT NULL," +
                "type INTEGER NOT NULL," +
                "subject INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE " + t_event);
        sqLiteDatabase.execSQL("DROP TABLE " + t_task);
        sqLiteDatabase.execSQL("DROP TABLE " + t_subjects);

        onCreate(sqLiteDatabase);
    }


}

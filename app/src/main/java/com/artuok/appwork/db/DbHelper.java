package com.artuok.appwork.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {

    public enum TasksEnum {
        id,
        date,
        title,
        deadline,
        subject,
        description,
        status
    }

    private static final int DatabaseVersion = 4;
    private static final String DatabaseName = "Calendar";
    public static final String t_subjects = "subjects";
    public static final String t_task = "task";
    public static final String t_event = "event";

    List<Integer> id = new ArrayList<>();
    List<String> date = new ArrayList<>();
    List<String> title = new ArrayList<>();
    List<String> deadline = new ArrayList<>();
    List<String> subject = new ArrayList<>();
    List<String> description = new ArrayList<>();
    List<Integer> status = new ArrayList<>();

    public DbHelper(@Nullable Context context) {
        super(context, DatabaseName, null, DatabaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + t_subjects + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + t_task + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date DATETIME NOT NULL," +
                "title TEXT NOT NULL," +
                "end_date DATETIME NOT NULL," +
                "subject INTEGER NOT NULL," +
                "description VARCHAR(500)," +
                "status INTEGER(1) NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + t_event + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "day_of_week INTEGER NOT NULL," +
                "time LONG NOT NULL," +
                "duration LONG NOT NULL," +
                "type INTEGER NOT NULL)");

        onCreate(sqLiteDatabase);
    }


}

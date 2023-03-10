package com.artuok.appwork.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;

public class DbHelper extends SQLiteOpenHelper {
    private static final int DatabaseVersion = 16;
    private static final String DatabaseName = "Calendar";
    public static final String t_subjects = "subjects";
    @Deprecated
    public static final String t_task = "task";
    public static final String T_TASK = "tasks";
    public static final String t_event = "event";
    public static final String t_alarm = "alarm";
    public static final String T_PHOTOS = "photos";

    private FirebaseAuth auth = FirebaseAuth.getInstance();

    public DbHelper(@Nullable Context context) {
        super(context, DatabaseName, null, DatabaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + t_subjects + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "color INTEGER NOT NULL)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_TASK + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date LONG NOT NULL," +
                "end_date LONG NOT NULL," +
                "subject INTEGER NOT NULL," +
                "description VARCHAR(500)," +
                "status INTEGER(1) NOT NULL,"+
                "user TEXT NOT NULL)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + t_event + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "day_of_week INTEGER NOT NULL," +
                "time LONG NOT NULL," +
                "duration LONG NOT NULL," +
                "type INTEGER NOT NULL," +
                "subject INTEGER NOT NULL)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + t_alarm + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "hour LONG NOT NULL," +
                "last_alarm INTEGER NOT NULL," +
                "alarm INTEGER NOT NULL)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_PHOTOS + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "awaiting INTEGER NOT NULL," +
                "name TEXT NOT NULL," +
                "path TEXT NOT NULL," +
                "timestamp LONG NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("ALTER TABLE  "+T_TASK+" ADD COLUMN user TEXT");
        if(auth.getCurrentUser() != null){
            ContentValues values = new ContentValues();
            values.put("user", auth.getCurrentUser().getUid());
            sqLiteDatabase.update(T_TASK, values, "", null);
        }
        onCreate(sqLiteDatabase);
    }


}

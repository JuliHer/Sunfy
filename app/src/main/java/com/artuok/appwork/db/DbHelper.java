package com.artuok.appwork.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;

public class DbHelper extends SQLiteOpenHelper {
    public static final int DatabaseVersion = 23;
    private static final String DatabaseName = "Calendar";

    public static final String T_TAG = "subject";
    @Deprecated
    public static final String t_subject = "subjects";
    public static final String T_TASK = "task";
    @Deprecated
    public static final String T_TASKS = "tasks";
    public static final String t_event = "event";
    public static final String t_alarm = "alarm";
    public static final String T_PHOTOS = "photos";
    public static final String T_PROJECTS = "projects";

    private FirebaseAuth auth = FirebaseAuth.getInstance();

    public DbHelper(@Nullable Context context) {
        super(context, DatabaseName, null, DatabaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        createTables(sqLiteDatabase);
    }

    public static void createTables(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_TAG + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "color INTEGER NOT NULL," +
                "proyect INTEGER NOT NULL DEFAULT 0)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS "+ T_TASK +"(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "description TEXT NOT NULL," +
                "date LONG NOT NULL," +
                "process_date LONG NOT NULL," +
                "complete_date LONG NOT NULL," +
                "deadline LONG NOT NULL," +
                "subject INTEGER NOT NULL," +
                "status INTEGER NOT NULL," +
                "user TEXT NOT NULL," +
                "favorite INTEGER NOT NULL)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS "+ T_PROJECTS +"(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "project_key TEXT)");
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
        onCreate(sqLiteDatabase);

        if(i < 21){
            String sqlQuery = "INSERT INTO "+T_TASK+" (id, description, date, process_date, complete_date, deadline, subject, status, user, favorite)" +
                    " SELECT id, description, date, date AS process_date, completed_date AS complete_date, end_date AS deadline, subject, status, user, favorite" +
                    " FROM "+T_TASKS+";";
            sqLiteDatabase.execSQL(sqlQuery);
        }

        if(i < 22){
            String sqlQuery = "INSERT INTO "+T_TAG+" (id, name, color, proyect)" +
            " SELECT id, name, color, '0' AS proyect" +
                    " FROM "+t_subject+";";
            sqLiteDatabase.execSQL(sqlQuery);
        }

        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " +T_TASKS);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " +t_subject);
    }


}

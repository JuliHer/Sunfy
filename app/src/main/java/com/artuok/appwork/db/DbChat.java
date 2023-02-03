package com.artuok.appwork.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DbChat extends SQLiteOpenHelper {
    private static final int DatabaseVersion = 12;
    private static final String DatabaseName = "Chat";
    public static final String T_CHATS_OPEN = "open";
    public static final String T_CHATS_LOGGED = "LOGGED";
    public static final String T_CHATS_MSG = "MSG";


    public DbChat(@Nullable Context context) {
        super(context, DatabaseName, null, DatabaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS_OPEN + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "phone TEXT NOT NULL," +
                "phoneIN TEXT NOT NULL," +
                "lastMsg TEXT NOT NULL," +
                "hasChat INTEGER NOT NULL," +
                "img TEXT NOT NULL)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS_LOGGED + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "phone TEXT NOT NULL," +
                "regionISO TEXT NOT NULL," +
                "chatId TEXT NOT NULL," +
                "isLog INTEGER NOT NULL," +
                "img TEXT NOT NULL," +
                "UNIQUE(phone))");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS_MSG + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "MSG TEXT NOT NULL," +
                "me int NOT NULL," +
                "timeSend TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "chat TEXT NOT NULL," +
                "number TEXT NOT NULL," +
                "mid TEXT NOT NULL," +
                "status INTEGER NOT NULL," +
                "UNIQUE(mid))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE " + T_CHATS_LOGGED);
        onCreate(sqLiteDatabase);
    }
}

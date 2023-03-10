package com.artuok.appwork.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DbChat extends SQLiteOpenHelper {
    private static final int DatabaseVersion = 17;
    private static final String DatabaseName = "Chat";
    public static final String T_CHATS_OPEN = "open";
    public static final String T_CHATS_LOGGED = "LOGGED";
    public static final String T_CHATS_MSG = "MSG";
    public static final String T_CHATS_EVENT = "EVENT";




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
                "img TEXT," +
                "publicKey TEXT NOT NULL," +
                "userId TEXT NOT NULL," +
                "UNIQUE(phone))");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS_MSG + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "MSG TEXT," +
                "me int NOT NULL," +
                "timeSend TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "chat TEXT NOT NULL," +
                "number TEXT NOT NULL," +
                "mid TEXT NOT NULL," +
                "status INTEGER NOT NULL," +
                "reply TEXT," +
                "publicKey TEXT NOT NULL," +
                "user TEXT," +
                "UNIQUE(mid))");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS_EVENT+ "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "chat TEXT NOT NULL," +
                "date LONG NOT NULL," +
                "end_date LONG NOT NULL," +
                "message LONG NOT NULL," +
                "description VARCHAR(500)," +
                "user TEXT NOT NULL,"+
                "added INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        onCreate(sqLiteDatabase);
    }
}

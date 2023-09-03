package com.artuok.appwork.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DbChat extends SQLiteOpenHelper {
    private static final int DatabaseVersion = 30;
    private static final String DatabaseName = "Chat";
    public static final String T_CHATS = "CHATS";
    public static final String T_CHATS_MSG = "MSG";
    public static final String T_CHATS_EVENT = "EVENT";

    public DbChat(@Nullable Context context) {
        super(context, DatabaseName, null, DatabaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "type INTEGER NOT NULL," +
                "chat TEXT NOT NULL," +
                "code TEXT NOT NULL," +
                "image TEXT NOT NULL," +
                "publicKey TEXT NOT NULL," +
                "updated LONG NOT NULL," +
                "UNIQUE(chat))");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS_EVENT + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "end_date LONG NOT NULL," +
                "message LONG NOT NULL," +
                "description VARCHAR(500)," +
                "user TEXT NOT NULL)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS_MSG + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "message TEXT," +
                "type INTEGER NOT NULL," +
                "timestamp LONG NOT NULL," +
                "mid TEXT NOT NULL," +
                "status INTEGER NOT NULL," +
                "reply TEXT," +
                "chat INTEGER NOT NULL," +
                "UNIQUE(mid))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        if(i < 30){
            sqLiteDatabase.execSQL("DROP TABLE " + T_CHATS_EVENT);
            sqLiteDatabase.execSQL("DROP TABLE " + T_CHATS);
            sqLiteDatabase.execSQL("DROP TABLE " + T_CHATS_MSG);
        }


        onCreate(sqLiteDatabase);
    }
}

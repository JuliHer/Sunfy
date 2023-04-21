package com.artuok.appwork.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DbChat extends SQLiteOpenHelper {
    private static final int DatabaseVersion = 26;
    private static final String DatabaseName = "Chat";
    public static final String T_CHATS_OPEN = "open";
    public static final String T_CHATS = "CHATS";
    public static final String T_CHATS_LOGGED = "LOGGED";
    public static final String T_CHATS_MSG = "MSG";
    public static final String T_CHATS_EVENT = "EVENT";
    public static final String T_CHATS_EVENTR = "-NqMK5uHVYaMR7S-2IVM";

    public DbChat(@Nullable Context context) {
        super(context, DatabaseName, null, DatabaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "type INTEGER NOT NULL," +
                "contact TEXT," +
                "chat TEXT NOT NULL," +
                "image TEXT NOT NULL," +
                "publicKey TEXT NOT NULL," +
                "updated LONG NOT NULL," +
                "UNIQUE(chat))");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS_LOGGED + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "number TEXT NOT NULL," +
                "ISO TEXT NOT NULL," +
                "log INTEGER NOT NULL," +
                "image TEXT," +
                "userId TEXT NOT NULL," +
                "publicKey TEXT NOT NULL," +
                "updated LONG NOT NULL," +
                "added INTEGER NOT NULL," +
                "UNIQUE(number))");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS_MSG + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "message TEXT," +
                "me int NOT NULL," +
                "timeSend TEXT NOT NULL," +
                "mid TEXT NOT NULL," +
                "status INTEGER NOT NULL," +
                "reply TEXT," +
                "number TEXT NOT NULL," +
                "chat INTEGER NOT NULL," +
                "UNIQUE(mid))");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + T_CHATS_EVENT + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "chat INTEGER NOT NULL," +
                "date LONG NOT NULL," +
                "end_date LONG NOT NULL," +
                "message LONG NOT NULL," +
                "description VARCHAR(500)," +
                "user TEXT NOT NULL," +
                "added INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        if (i < 21) {
            sqLiteDatabase.execSQL("DROP TABLE " + T_CHATS_OPEN);
            sqLiteDatabase.execSQL("DROP TABLE " + T_CHATS_MSG);
            sqLiteDatabase.execSQL("DROP TABLE " + T_CHATS);
            sqLiteDatabase.execSQL("DROP TABLE " + T_CHATS_LOGGED);
            sqLiteDatabase.execSQL("DROP TABLE " + T_CHATS_EVENT);
        }

        if (i < 25) {
            sqLiteDatabase.execSQL("DROP TABLE " + T_CHATS_LOGGED);
        }

        if (i < 26) {
            sqLiteDatabase.execSQL("ALTER TABLE " + T_CHATS + " ADD COLUMN updated DEFAULT (0) NOT NULL");
        }
        onCreate(sqLiteDatabase);
    }
}

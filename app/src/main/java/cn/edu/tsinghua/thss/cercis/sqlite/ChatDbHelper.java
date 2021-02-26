package cn.edu.tsinghua.thss.cercis.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

// TODO Replace with greenDAO
public class ChatDbHelper extends SQLiteOpenHelper {
    public ChatDbHelper(@Nullable Context context, @Nullable SQLiteDatabase.CursorFactory factory) {
        super(context, "chat_db", factory, 1, ChatDbHelper::initDb);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        initDb(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // do nothing
    }

    private static void initDb(SQLiteDatabase db) {
        db.execSQL("create table chats(id INTEGER primary key, type INTEGER, name TEXT)");
    }
}

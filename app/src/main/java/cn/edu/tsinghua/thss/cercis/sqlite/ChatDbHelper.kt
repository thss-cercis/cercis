package cn.edu.tsinghua.thss.cercis.sqlite

import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper

// TODO Replace with greenDAO
class ChatDbHelper(context: Context?, factory: CursorFactory?) : SQLiteOpenHelper(context, "chat_db", factory, 1, DatabaseErrorHandler { db: SQLiteDatabase -> initDb(db) }) {
    override fun onCreate(db: SQLiteDatabase) {
        initDb(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // do nothing
    }

    companion object {
        @JvmStatic
        private fun initDb(db: SQLiteDatabase) {
            db.execSQL("create table chats(id INTEGER primary key, type INTEGER, name TEXT)")
        }
    }
}
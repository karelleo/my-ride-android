package com.myride.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Receipt(
    val id: Long, val service: String, val date: String, val amount: Long,
    val origin: String, val destination: String, val driver: String,
    val plate: String, val note: String, val attachment: String
)

class ReceiptDb(context: Context) : SQLiteOpenHelper(context, "myride.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE receipts (id INTEGER PRIMARY KEY AUTOINCREMENT, service TEXT, date TEXT, amount INTEGER, origin TEXT, destination TEXT, driver TEXT, plate TEXT, note TEXT, attachment TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun add(service: String, date: String, amount: Long, origin: String, destination: String, driver: String, plate: String, note: String, attachment: String): Long {
        val v = ContentValues().apply {
            put("service", service); put("date", date); put("amount", amount); put("origin", origin)
            put("destination", destination); put("driver", driver); put("plate", plate); put("note", note); put("attachment", attachment)
        }
        return writableDatabase.insert("receipts", null, v)
    }
    fun all(): List<Receipt> {
        val out = mutableListOf<Receipt>()
        readableDatabase.rawQuery("SELECT id,service,date,amount,origin,destination,driver,plate,note,attachment FROM receipts ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out.add(Receipt(c.getLong(0), c.getString(1) ?: "GoCar", c.getString(2) ?: "", c.getLong(3), c.getString(4) ?: "", c.getString(5) ?: "", c.getString(6) ?: "", c.getString(7) ?: "", c.getString(8) ?: "", c.getString(9) ?: ""))
        }
        return out
    }
    fun find(id: Long): Receipt? = all().firstOrNull { it.id == id }
}

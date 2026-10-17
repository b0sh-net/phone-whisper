package net.b0sh.audiotext

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "audiotext_live.db"
private const val DB_VERSION = 1
private const val TABLE = "live_transcripts"

/** SQLite storage for live transcriptions (research R4). */
private class LiveTranscriptDbHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "created_at INTEGER NOT NULL, " +
                "duration_ms INTEGER NOT NULL, " +
                "text TEXT NOT NULL)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }
}

/**
 * Android [TranscriptRepository] backed by a local SQLite database.
 * Thin adapter: the ordered query mirrors [InMemoryTranscriptRepository].
 */
class SqliteTranscriptRepository(context: Context) : TranscriptRepository {

    private val helper = LiveTranscriptDbHelper(context.applicationContext)

    override fun add(createdAtMillis: Long, durationMillis: Long, text: String): Long {
        val values = ContentValues().apply {
            put("created_at", createdAtMillis)
            put("duration_ms", durationMillis)
            put("text", text)
        }
        return helper.writableDatabase.insert(TABLE, null, values)
    }

    override fun update(id: Long, durationMillis: Long, text: String) {
        val values = ContentValues().apply {
            put("duration_ms", durationMillis)
            put("text", text)
        }
        helper.writableDatabase.update(TABLE, values, "id = ?", arrayOf(id.toString()))
    }

    override fun list(): List<ArchivedTranscript> {
        val result = mutableListOf<ArchivedTranscript>()
        helper.readableDatabase.query(
            TABLE,
            arrayOf("id", "created_at", "duration_ms", "text"),
            null,
            null,
            null,
            null,
            "created_at DESC, id DESC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    ArchivedTranscript(
                        id = cursor.getLong(0),
                        createdAtMillis = cursor.getLong(1),
                        durationMillis = cursor.getLong(2),
                        text = cursor.getString(3),
                    ),
                )
            }
        }
        return result
    }

    override fun delete(id: Long) {
        helper.writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString()))
    }
}

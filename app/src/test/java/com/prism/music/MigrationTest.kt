package com.prism.music

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.prism.music.data.db.PrismDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Every older database version upgrades to the current one without losing the listener's history. Each version's
 * schema is exported by Room into app/schemas; this builds a database exactly as that version left it (with a song,
 * a like and plays in it), opens it with the app's migrations, and checks the rows survived. Room itself refuses to
 * open it if a migration's result doesn't match the current schema.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MigrationTest {
    private val schemaDir = listOf("schemas", "app/schemas").map { File(it, "com.prism.music.data.db.PrismDatabase") }.first { it.isDirectory }
    private val versions = schemaDir.listFiles()!!.mapNotNull { it.nameWithoutExtension.toIntOrNull() }.sorted()

    @Test
    fun everyVersionUpgradesKeepingHistory() {
        val latest = versions.last()
        for (from in versions.dropLast(1)) {
            val context = RuntimeEnvironment.getApplication()
            val name = "migrate-from-$from.db"
            context.deleteDatabase(name)
            createAt(context.getDatabasePath(name), from)
            val db = Room.databaseBuilder(context, PrismDatabase::class.java, name)
                .addMigrations(*PrismDatabase.MIGRATIONS)
                .allowMainThreadQueries()
                .build()
            runBlocking {
                val song = db.songs().get("song1")
                assertEquals("v$from: song kept", "Title", song?.title)
                assertTrue("v$from: like kept", song!!.liked)
                assertEquals("v$from: plays kept", 2, db.plays().count(0, Long.MAX_VALUE))
                assertEquals("v$from: play time kept", 200_000L, db.plays().totalMs(0, Long.MAX_VALUE))
                assertEquals("v$from: top song", "song1", db.plays().topSongs(0, Long.MAX_VALUE, 5).single().songId)
            }
            assertEquals(latest, db.openHelper.readableDatabase.version)
            db.close()
        }
    }

    @Test
    fun currentSchemaIsExported() {
        // A version bump without its exported schema would leave the next migration untestable.
        val fresh = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PrismDatabase::class.java).allowMainThreadQueries().build()
        val current = fresh.openHelper.writableDatabase.version
        fresh.close()
        assertEquals(current, versions.last())
        assertEquals("one migration per version step", current - 1, PrismDatabase.MIGRATIONS.size)
    }

    /** Builds the database as version [version] of Prism created it, from the exported schema. */
    private fun createAt(file: File, version: Int) {
        val schema = Json.parseToJsonElement(File(schemaDir, "$version.json").readText()).jsonObject["database"]!!.jsonObject
        file.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        for (entity in schema["entities"]!!.jsonArray) {
            val e = entity.jsonObject
            val table = e.str("tableName")
            db.execSQL(e.str("createSql").replace("\${TABLE_NAME}", table))
            e["indices"]?.jsonArray?.forEach { db.execSQL(it.jsonObject.str("createSql").replace("\${TABLE_NAME}", table)) }
        }
        schema["setupQueries"]!!.jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
        db.version = schema["version"]!!.jsonPrimitive.int
        db.insert("songs", null, ContentValues().apply {
            put("id", "song1"); put("title", "Title"); put("artistName", "Artist"); put("artistId", "UC1")
            put("durationSec", 200); put("isVideo", 0); put("liked", 1); put("likedAt", 5L); put("lastPlayed", 10L); put("smartDownload", 0)
        })
        for (t in listOf(1_000L, 2_000L)) db.insert("play_events", null, ContentValues().apply {
            put("songId", "song1"); put("timestamp", t); put("playedMs", 100_000L)
        })
        db.close()
    }

    private fun JsonObject.str(key: String) = this[key]!!.jsonPrimitive.content
}

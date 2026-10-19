package com.prism.music.data.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prism.music.data.model.AlbumRef
import com.prism.music.data.model.ArtistRef
import com.prism.music.data.model.Song
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val artistId: String?,
    val albumTitle: String?,
    val albumId: String?,
    val durationSec: Int,
    val thumbnail: String?,
    val isVideo: Boolean = false,
    val liked: Boolean = false,
    val likedAt: Long = 0,
    val lastPlayed: Long = 0,
    val smartDownload: Boolean = false,
) {
    fun toSong() = Song(
        id = id,
        title = title,
        artists = artistName.split(", ").filter { it.isNotBlank() }.mapIndexed { i, n -> ArtistRef(n, if (i == 0) artistId else null) },
        album = albumTitle?.let { AlbumRef(it, albumId) },
        durationSec = durationSec,
        thumbnail = thumbnail,
        isVideo = isVideo,
    )
}

fun Song.toEntity(base: SongEntity? = null) = SongEntity(
    id = id,
    title = title,
    artistName = artistText,
    artistId = artists.firstOrNull()?.id,
    albumTitle = album?.title,
    albumId = album?.id,
    durationSec = durationSec.takeIf { it > 0 } ?: (base?.durationSec ?: 0),
    thumbnail = thumbnail ?: base?.thumbnail,
    isVideo = isVideo,
    liked = base?.liked ?: false,
    likedAt = base?.likedAt ?: 0,
    lastPlayed = base?.lastPlayed ?: 0,
    smartDownload = base?.smartDownload ?: false,
)

@Entity(tableName = "play_events", indices = [Index("songId"), Index("timestamp")])
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val timestamp: Long,
    val playedMs: Long,
    /** What the song was playing as part of: a [com.prism.music.playback.QueueKind] name, or a feature ("smart", "dj", "sing", "blend", "mix:<id>"). */
    val source: String? = null,
    /** From something the listener excluded from their taste profile: Replay counts it, recommendations don't. */
    @ColumnInfo(defaultValue = "0") val excluded: Boolean = false,
)

@Entity(tableName = "lyrics", primaryKeys = ["songId", "source"])
data class LyricsEntity(
    val songId: String,
    val source: String,
    val synced: Boolean,
    val content: String,
    val fetchedAt: Long,
)

@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey val key: String, // "song:<id>", "album:<title|artist>", "artist:<name>"
    val genre: String?,
    val fetchedAt: Long,
)

@Entity(tableName = "motion_art")
data class MotionArtEntity(
    @PrimaryKey val key: String,
    val tallUrl: String?,
    val squareUrl: String?,
    val fetchedAt: Long,
)

/** YouTube's "Most replayed" curve for a video ([com.prism.music.data.Heatmap]); empty [intensity] = it has none. */
@Entity(tableName = "heatmaps")
data class HeatmapEntity(
    @PrimaryKey val videoId: String,
    val segmentMs: Long,
    val intensity: String,
    val fetchedAt: Long,
)

/** A song's lyrics in Latin letters ([com.prism.music.data.lyrics.Romanizer]), keyed by song, lyrics and engine. */
@Entity(tableName = "romanized")
data class RomanizedEntity(
    @PrimaryKey val key: String,
    val content: String,
    val fetchedAt: Long,
)

/**
 * A release on one of the listener's artists' pages (Release Radar): albums, singles and EPs, as first seen there.
 * [baseline]: there when the artist was first looked at (their back catalogue, never announced as new).
 */
@Entity(tableName = "releases", indices = [Index("artistId")])
data class ReleaseEntity(
    /** YouTube's browse id for it (MPREb_…). */
    @PrimaryKey val id: String,
    val artistId: String,
    val artistName: String,
    val title: String,
    /** "Album", "Single" or "EP". */
    val kind: String,
    val year: Int,
    /** "2026-10-16" when Deezer knows it; null until then (or when it doesn't). */
    val releaseDate: String?,
    val thumbnail: String?,
    val playlistId: String?,
    val firstSeenAt: Long,
    val baseline: Boolean,
    @ColumnInfo(defaultValue = "0") val notifiedAt: Long = 0,
)

/** A mix Prism made for the listener (Daily Mix, the time-of-day mix, Release Radar): kept so it's the same all day, offline too. */
@Entity(tableName = "mixes")
data class MixEntity(
    /** "daily:1"…"daily:6", "time", "radar". */
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    /** The artists it's built around (for its cover and the subtitle), "|"-separated. */
    val seedArtists: String,
    /** Accent colour for its cover (ARGB). */
    val color: Int,
    val createdAt: Long,
    /** The songs, as JSON ([com.prism.music.data.foryou.MixSongs]). */
    val songsJson: String,
)

/**
 * A Blend with a friend: their taste code as they sent it, and ours as it was then (what "Send yours back" sends, so
 * both phones work the Blend out from the same two codes). Keyed by the friend's random Blend id.
 */
@Entity(tableName = "blends")
data class BlendEntity(
    @PrimaryKey val friendWho: String,
    val friendName: String,
    val friendCode: String,
    val myCode: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class SongPlays(
    val songId: String,
    val plays: Int,
    val ms: Long,
    val title: String,
    val artistName: String,
    val thumbnail: String?,
    val albumTitle: String?,
    val durationSec: Int,
    val artistId: String?,
)

data class ArtistPlays(val artistName: String, val plays: Int, val ms: Long, val thumbnail: String?, val artistId: String?)
data class MonthTotal(val month: String, val ms: Long)

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun get(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getAll(ids: List<String>): List<SongEntity>

    @Upsert
    suspend fun upsert(song: SongEntity)

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM songs WHERE liked = 1 ORDER BY likedAt DESC")
    fun likedFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE liked = 1 ORDER BY likedAt DESC")
    suspend fun liked(): List<SongEntity>

    @Query("SELECT id FROM songs WHERE liked = 1")
    fun likedIdsFlow(): Flow<List<String>>

    @Query("UPDATE songs SET liked = :liked, likedAt = :at WHERE id = :id")
    suspend fun setLiked(id: String, liked: Boolean, at: Long)

    @Query("UPDATE songs SET lastPlayed = :at WHERE id = :id")
    suspend fun touch(id: String, at: Long)

    @Query("SELECT * FROM songs WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT :limit")
    fun recentFlow(limit: Int = 30): Flow<List<SongEntity>>

    /** The phone's own files, most recently played first (Local files → Recently played). */
    @Query("SELECT id FROM songs WHERE lastPlayed > 0 AND id LIKE 'local:%' ORDER BY lastPlayed DESC LIMIT :limit")
    fun recentLocalIdsFlow(limit: Int = 100): Flow<List<String>>

    @Query("UPDATE songs SET smartDownload = :v WHERE id = :id")
    suspend fun setSmart(id: String, v: Boolean)

    @Query("SELECT * FROM songs WHERE smartDownload = 1")
    suspend fun smartDownloads(): List<SongEntity>
}

@Dao
interface PlayEventDao {
    @Insert
    suspend fun insert(e: PlayEventEntity)

    @Query("SELECT * FROM play_events WHERE timestamp > :since")
    suspend fun eventsSince(since: Long): List<PlayEventEntity>

    @Query("SELECT COALESCE(SUM(playedMs),0) FROM play_events WHERE timestamp BETWEEN :from AND :to")
    suspend fun totalMs(from: Long, to: Long): Long

    @Query("SELECT COUNT(*) FROM play_events WHERE timestamp BETWEEN :from AND :to")
    suspend fun count(from: Long, to: Long): Int

    @Query(
        """SELECT e.songId AS songId, COUNT(*) AS plays, SUM(e.playedMs) AS ms, s.title AS title,
           s.artistName AS artistName, s.thumbnail AS thumbnail, s.albumTitle AS albumTitle,
           s.durationSec AS durationSec, s.artistId AS artistId
           FROM play_events e JOIN songs s ON s.id = e.songId
           WHERE e.timestamp BETWEEN :from AND :to
           GROUP BY e.songId ORDER BY ms DESC, plays DESC LIMIT :limit"""
    )
    suspend fun topSongs(from: Long, to: Long, limit: Int): List<SongPlays>

    @Query(
        """SELECT s.artistName AS artistName, COUNT(*) AS plays, SUM(e.playedMs) AS ms,
           MAX(s.thumbnail) AS thumbnail, MAX(s.artistId) AS artistId
           FROM play_events e JOIN songs s ON s.id = e.songId
           WHERE e.timestamp BETWEEN :from AND :to
           GROUP BY s.artistName ORDER BY ms DESC LIMIT :limit"""
    )
    suspend fun topArtists(from: Long, to: Long, limit: Int): List<ArtistPlays>

    @Query(
        """SELECT strftime('%Y-%m', timestamp / 1000, 'unixepoch', 'localtime') AS month, SUM(playedMs) AS ms
           FROM play_events WHERE timestamp BETWEEN :from AND :to GROUP BY month ORDER BY month"""
    )
    suspend fun monthly(from: Long, to: Long): List<MonthTotal>

    @Query("SELECT COUNT(DISTINCT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime')) FROM play_events WHERE timestamp BETWEEN :from AND :to")
    suspend fun activeDays(from: Long, to: Long): Int

    @Query("SELECT MIN(timestamp) FROM play_events")
    suspend fun firstEvent(): Long?

    @Query("SELECT * FROM play_events WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp")
    suspend fun events(from: Long, to: Long): List<PlayEventEntity>

    @Query("SELECT COUNT(DISTINCT songId) FROM play_events WHERE timestamp BETWEEN :from AND :to")
    suspend fun songCount(from: Long, to: Long): Int

    @Query("SELECT COUNT(DISTINCT s.artistName) FROM play_events e JOIN songs s ON s.id = e.songId WHERE e.timestamp BETWEEN :from AND :to")
    suspend fun artistCount(from: Long, to: Long): Int

    /** Artists first heard between [from] and [to], with their listening in that time, most listened first. */
    @Query(
        """SELECT s.artistName AS artistName, COUNT(*) AS plays, SUM(e.playedMs) AS ms,
           MAX(s.thumbnail) AS thumbnail, MAX(s.artistId) AS artistId
           FROM play_events e JOIN songs s ON s.id = e.songId
           WHERE e.timestamp BETWEEN :from AND :to AND s.artistName IN (
               SELECT s2.artistName FROM play_events e2 JOIN songs s2 ON s2.id = e2.songId
               GROUP BY s2.artistName HAVING MIN(e2.timestamp) BETWEEN :from AND :to)
           GROUP BY s.artistName ORDER BY ms DESC"""
    )
    suspend fun newArtists(from: Long, to: Long): List<ArtistPlays>

    @Query("SELECT songId FROM play_events WHERE timestamp > :since GROUP BY songId ORDER BY COUNT(*) DESC LIMIT :limit")
    suspend fun mostPlayedIds(since: Long, limit: Int): List<String>

    /** [topSongs] for taste: plays the listener left out of it don't count. */
    @Query(
        """SELECT e.songId AS songId, COUNT(*) AS plays, SUM(e.playedMs) AS ms, s.title AS title,
           s.artistName AS artistName, s.thumbnail AS thumbnail, s.albumTitle AS albumTitle,
           s.durationSec AS durationSec, s.artistId AS artistId
           FROM play_events e JOIN songs s ON s.id = e.songId
           WHERE e.timestamp BETWEEN :from AND :to AND e.excluded = 0
           GROUP BY e.songId ORDER BY ms DESC, plays DESC LIMIT :limit"""
    )
    suspend fun tasteTopSongs(from: Long, to: Long, limit: Int): List<SongPlays>

    /** [topArtists] for taste: plays the listener left out of it don't count. */
    @Query(
        """SELECT s.artistName AS artistName, COUNT(*) AS plays, SUM(e.playedMs) AS ms,
           MAX(s.thumbnail) AS thumbnail, MAX(s.artistId) AS artistId
           FROM play_events e JOIN songs s ON s.id = e.songId
           WHERE e.timestamp BETWEEN :from AND :to AND e.excluded = 0
           GROUP BY s.artistName ORDER BY ms DESC LIMIT :limit"""
    )
    suspend fun tasteTopArtists(from: Long, to: Long, limit: Int): List<ArtistPlays>

    /** Plays that count towards the listener's taste (not from anything they excluded from it), oldest first. */
    @Query("SELECT * FROM play_events WHERE timestamp > :since AND excluded = 0 ORDER BY timestamp")
    suspend fun tasteEventsSince(since: Long): List<PlayEventEntity>
}

@Dao
interface ReleaseDao {
    @Query("SELECT id FROM releases WHERE artistId = :artistId")
    suspend fun idsFor(artistId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(releases: List<ReleaseEntity>)

    @Query("UPDATE releases SET releaseDate = :date WHERE id = :id")
    suspend fun setDate(id: String, date: String)

    @Query("UPDATE releases SET notifiedAt = :at WHERE id IN (:ids)")
    suspend fun setNotified(ids: List<String>, at: Long)

    /** Releases that aren't back catalogue, newest first (by date where known, else when they were first seen). */
    @Query("SELECT * FROM releases WHERE baseline = 0 AND year >= :minYear ORDER BY COALESCE(releaseDate, '') DESC, firstSeenAt DESC")
    suspend fun fresh(minYear: Int): List<ReleaseEntity>

    @Query("SELECT * FROM releases WHERE baseline = 0 AND year >= :minYear ORDER BY COALESCE(releaseDate, '') DESC, firstSeenAt DESC")
    fun freshFlow(minYear: Int): Flow<List<ReleaseEntity>>

    @Query("DELETE FROM releases WHERE artistId NOT IN (:keep)")
    suspend fun forgetArtistsExcept(keep: List<String>)
}

@Dao
interface BlendDao {
    @Query("SELECT * FROM blends ORDER BY updatedAt DESC")
    fun allFlow(): Flow<List<BlendEntity>>

    @Query("SELECT * FROM blends WHERE friendWho = :who")
    suspend fun get(who: String): BlendEntity?

    @Upsert
    suspend fun put(b: BlendEntity)

    @Query("DELETE FROM blends WHERE friendWho = :who")
    suspend fun delete(who: String)
}

@Dao
interface MixDao {
    @Query("SELECT * FROM mixes ORDER BY id")
    fun allFlow(): Flow<List<MixEntity>>

    @Query("SELECT * FROM mixes ORDER BY id")
    suspend fun all(): List<MixEntity>

    @Query("SELECT * FROM mixes WHERE id = :id")
    suspend fun get(id: String): MixEntity?

    @Upsert
    suspend fun put(m: MixEntity)

    @Query("DELETE FROM mixes WHERE id LIKE :pattern AND id NOT IN (:keep)")
    suspend fun deleteOthers(pattern: String, keep: List<String>)
}

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE songId = :songId AND source = :source")
    suspend fun get(songId: String, source: String): LyricsEntity?

    @Query("SELECT * FROM lyrics WHERE songId = :songId AND content != ''")
    suspend fun allFor(songId: String): List<LyricsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(e: LyricsEntity)

    /** Forgets cached "not found" answers so they're looked up again. */
    @Query("DELETE FROM lyrics WHERE content = ''")
    suspend fun clearMisses()
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM genres WHERE `key` = :key")
    suspend fun genre(key: String): GenreEntity?

    @Query("SELECT * FROM genres WHERE `key` IN (:keys)")
    suspend fun genres(keys: List<String>): List<GenreEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putGenre(e: GenreEntity)

    /** Forgets genre results saved before [before]; Deezer's per-album genres are facts and stay. */
    @Query("DELETE FROM genres WHERE fetchedAt < :before AND `key` NOT LIKE 'dzalbum:%'")
    suspend fun clearGenresBefore(before: Long)

    @Query("SELECT * FROM motion_art WHERE `key` = :key")
    suspend fun motion(key: String): MotionArtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMotion(e: MotionArtEntity)
}

@Dao
interface HeatmapDao {
    @Query("SELECT * FROM heatmaps WHERE videoId = :id")
    suspend fun get(id: String): HeatmapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(e: HeatmapEntity)
}

@Dao
interface RomanizedDao {
    @Query("SELECT * FROM romanized WHERE `key` = :key")
    suspend fun get(key: String): RomanizedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(e: RomanizedEntity)
}

@Database(
    entities = [SongEntity::class, PlayEventEntity::class, LyricsEntity::class, GenreEntity::class, MotionArtEntity::class, HeatmapEntity::class,
        RomanizedEntity::class, ReleaseEntity::class, MixEntity::class, BlendEntity::class],
    version = 6,
    exportSchema = true,
)
abstract class PrismDatabase : RoomDatabase() {
    abstract fun songs(): SongDao
    abstract fun plays(): PlayEventDao
    abstract fun lyrics(): LyricsDao
    abstract fun meta(): MetaDao
    abstract fun heatmaps(): HeatmapDao
    abstract fun romanized(): RomanizedDao
    abstract fun releases(): ReleaseDao
    abstract fun mixes(): MixDao
    abstract fun blends(): BlendDao

    companion object {
        /**
         * Every schema change gets a migration here (and its exported schema in app/schemas): the database holds the
         * listener's whole play history, so an upgrade must never fall back to wiping it. MigrationTest checks each one.
         */
        val MIGRATIONS = arrayOf(
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE play_events ADD COLUMN source TEXT")
                    db.execSQL("ALTER TABLE play_events ADD COLUMN excluded INTEGER NOT NULL DEFAULT 0")
                }
            },
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `heatmaps` (`videoId` TEXT NOT NULL, `segmentMs` INTEGER NOT NULL, `intensity` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`videoId`))")
                }
            },
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `romanized` (`key` TEXT NOT NULL, `content` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))")
                }
            },
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `releases` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, `artistName` TEXT NOT NULL, `title` TEXT NOT NULL, `kind` TEXT NOT NULL, `year` INTEGER NOT NULL, `releaseDate` TEXT, `thumbnail` TEXT, `playlistId` TEXT, `firstSeenAt` INTEGER NOT NULL, `baseline` INTEGER NOT NULL, `notifiedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_releases_artistId` ON `releases` (`artistId`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `mixes` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `seedArtists` TEXT NOT NULL, `color` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `songsJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
                }
            },
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `blends` (`friendWho` TEXT NOT NULL, `friendName` TEXT NOT NULL, `friendCode` TEXT NOT NULL, `myCode` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`friendWho`))")
                }
            },
        )

        fun build(context: Context): PrismDatabase =
            Room.databaseBuilder(context, PrismDatabase::class.java, "prism.db")
                .addMigrations(*MIGRATIONS)
                // Only a downgrade (installing an older Prism over a newer one) may start the database over.
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
    }
}

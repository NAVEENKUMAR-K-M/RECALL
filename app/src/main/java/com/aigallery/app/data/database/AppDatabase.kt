package com.aigallery.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        ScreenshotEntity::class,
        ScreenshotTextEntity::class,
        TextBlockEntity::class,
        ScreenshotEntityItem::class,
        ScreenshotClassificationEntity::class,
        ScreenshotPlatformEntity::class,
        ScreenshotTagEntity::class,
        ScreenshotCollectionEntity::class,
        ScreenshotCollectionCrossRef::class,
        OrganizedMediaEntity::class,
        ScreenshotLLMUnderstandingEntity::class,
        ScreenshotEmbeddingEntity::class,
        ScreenshotRelationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun screenshotDao(): ScreenshotDao
    abstract fun organizationDao(): OrganizationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Table: screenshot_llm_understandings
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS screenshot_llm_understandings (
                        screenshotId INTEGER PRIMARY KEY NOT NULL,
                        title TEXT,
                        summary TEXT,
                        topic TEXT,
                        intent TEXT NOT NULL,
                        importance TEXT NOT NULL,
                        keywords TEXT NOT NULL,
                        facts TEXT NOT NULL,
                        llmVersion INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(screenshotId) REFERENCES screenshots(screenshotId) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_screenshot_llm_understandings_screenshotId ON screenshot_llm_understandings (screenshotId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_screenshot_llm_understandings_intent ON screenshot_llm_understandings (intent)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_screenshot_llm_understandings_topic ON screenshot_llm_understandings (topic)")

                // Table: screenshot_embeddings
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS screenshot_embeddings (
                        screenshotId INTEGER PRIMARY KEY NOT NULL,
                        embedding BLOB NOT NULL,
                        dimension INTEGER NOT NULL,
                        modelVersion TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(screenshotId) REFERENCES screenshots(screenshotId) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_screenshot_embeddings_screenshotId ON screenshot_embeddings (screenshotId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_screenshot_embeddings_modelVersion ON screenshot_embeddings (modelVersion)")

                // Table: screenshot_relations
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS screenshot_relations (
                        sourceId INTEGER NOT NULL,
                        targetId INTEGER NOT NULL,
                        relationType TEXT NOT NULL,
                        score REAL NOT NULL,
                        reason TEXT,
                        modelVersion TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(sourceId, targetId),
                        FOREIGN KEY(sourceId) REFERENCES screenshots(screenshotId) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(targetId) REFERENCES screenshots(screenshotId) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_screenshot_relations_sourceId ON screenshot_relations (sourceId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_screenshot_relations_targetId ON screenshot_relations (targetId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_screenshot_relations_relationType ON screenshot_relations (relationType)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_gallery.db"
                ).addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build().also {
                    INSTANCE = it
                }
            }
        }
    }
}


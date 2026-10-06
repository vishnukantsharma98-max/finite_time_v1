package com.example.data.focus

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL")
public class AppDatabase_Impl : AppDatabase() {
  private val _focusDao: Lazy<FocusDao> = lazy {
    FocusDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate =
      object :
        RoomOpenDelegate(
          1,
          "ffddd4c341b3d171ba50da3859747a6d",
          "9b9ff30bbeb251e9601a010052dbd45c",
        ) {
        public override fun createAllTables(connection: SQLiteConnection) {
          connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startTimestampMillis` INTEGER NOT NULL, `endTimestampMillis` INTEGER, `currentSegmentStartMillis` INTEGER, `accumulatedDurationMillis` INTEGER NOT NULL, `state` TEXT NOT NULL)"
          )
          connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `focus_segments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `startTimestampMillis` INTEGER NOT NULL, `endTimestampMillis` INTEGER NOT NULL, `durationMillis` INTEGER NOT NULL)"
          )
          connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_focus_segments_sessionId` ON `focus_segments` (`sessionId`)"
          )
          connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_focus_segments_startTimestampMillis_endTimestampMillis` ON `focus_segments` (`startTimestampMillis`, `endTimestampMillis`)"
          )
          connection.execSQL(
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
          )
          connection.execSQL(
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ffddd4c341b3d171ba50da3859747a6d')"
          )
        }

        public override fun dropAllTables(connection: SQLiteConnection) {
          connection.execSQL("DROP TABLE IF EXISTS `focus_sessions`")
          connection.execSQL("DROP TABLE IF EXISTS `focus_segments`")
        }

        public override fun onCreate(connection: SQLiteConnection) {}

        public override fun onOpen(connection: SQLiteConnection) {
          internalInitInvalidationTracker(connection)
        }

        public override fun onPreMigrate(connection: SQLiteConnection) {
          dropFtsSyncTriggers(connection)
        }

        public override fun onPostMigrate(connection: SQLiteConnection) {}

        public override fun onValidateSchema(
          connection: SQLiteConnection
        ): RoomOpenDelegate.ValidationResult {
          val _columnsFocusSessions: MutableMap<String, TableInfo.Column> = mutableMapOf()
          _columnsFocusSessions.put(
            "id",
            TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY),
          )
          _columnsFocusSessions.put(
            "startTimestampMillis",
            TableInfo.Column(
              "startTimestampMillis",
              "INTEGER",
              true,
              0,
              null,
              TableInfo.CREATED_FROM_ENTITY,
            ),
          )
          _columnsFocusSessions.put(
            "endTimestampMillis",
            TableInfo.Column(
              "endTimestampMillis",
              "INTEGER",
              false,
              0,
              null,
              TableInfo.CREATED_FROM_ENTITY,
            ),
          )
          _columnsFocusSessions.put(
            "currentSegmentStartMillis",
            TableInfo.Column(
              "currentSegmentStartMillis",
              "INTEGER",
              false,
              0,
              null,
              TableInfo.CREATED_FROM_ENTITY,
            ),
          )
          _columnsFocusSessions.put(
            "accumulatedDurationMillis",
            TableInfo.Column(
              "accumulatedDurationMillis",
              "INTEGER",
              true,
              0,
              null,
              TableInfo.CREATED_FROM_ENTITY,
            ),
          )
          _columnsFocusSessions.put(
            "state",
            TableInfo.Column("state", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY),
          )
          val _foreignKeysFocusSessions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
          val _indicesFocusSessions: MutableSet<TableInfo.Index> = mutableSetOf()
          val _infoFocusSessions: TableInfo =
            TableInfo(
              "focus_sessions",
              _columnsFocusSessions,
              _foreignKeysFocusSessions,
              _indicesFocusSessions,
            )
          val _existingFocusSessions: TableInfo = read(connection, "focus_sessions")
          if (!_infoFocusSessions.equals(_existingFocusSessions)) {
            return RoomOpenDelegate.ValidationResult(
              false,
              """
              |focus_sessions(com.example.data.focus.FocusSessionEntity).
              | Expected:
              |"""
                .trimMargin() +
                _infoFocusSessions +
                """
              |
              | Found:
              |"""
                  .trimMargin() +
                _existingFocusSessions,
            )
          }
          val _columnsFocusSegments: MutableMap<String, TableInfo.Column> = mutableMapOf()
          _columnsFocusSegments.put(
            "id",
            TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY),
          )
          _columnsFocusSegments.put(
            "sessionId",
            TableInfo.Column("sessionId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY),
          )
          _columnsFocusSegments.put(
            "startTimestampMillis",
            TableInfo.Column(
              "startTimestampMillis",
              "INTEGER",
              true,
              0,
              null,
              TableInfo.CREATED_FROM_ENTITY,
            ),
          )
          _columnsFocusSegments.put(
            "endTimestampMillis",
            TableInfo.Column(
              "endTimestampMillis",
              "INTEGER",
              true,
              0,
              null,
              TableInfo.CREATED_FROM_ENTITY,
            ),
          )
          _columnsFocusSegments.put(
            "durationMillis",
            TableInfo.Column(
              "durationMillis",
              "INTEGER",
              true,
              0,
              null,
              TableInfo.CREATED_FROM_ENTITY,
            ),
          )
          val _foreignKeysFocusSegments: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
          val _indicesFocusSegments: MutableSet<TableInfo.Index> = mutableSetOf()
          _indicesFocusSegments.add(
            TableInfo.Index(
              "index_focus_segments_sessionId",
              false,
              listOf("sessionId"),
              listOf("ASC"),
            )
          )
          _indicesFocusSegments.add(
            TableInfo.Index(
              "index_focus_segments_startTimestampMillis_endTimestampMillis",
              false,
              listOf("startTimestampMillis", "endTimestampMillis"),
              listOf("ASC", "ASC"),
            )
          )
          val _infoFocusSegments: TableInfo =
            TableInfo(
              "focus_segments",
              _columnsFocusSegments,
              _foreignKeysFocusSegments,
              _indicesFocusSegments,
            )
          val _existingFocusSegments: TableInfo = read(connection, "focus_segments")
          if (!_infoFocusSegments.equals(_existingFocusSegments)) {
            return RoomOpenDelegate.ValidationResult(
              false,
              """
              |focus_segments(com.example.data.focus.FocusSegmentEntity).
              | Expected:
              |"""
                .trimMargin() +
                _infoFocusSegments +
                """
              |
              | Found:
              |"""
                  .trimMargin() +
                _existingFocusSegments,
            )
          }
          return RoomOpenDelegate.ValidationResult(true, null)
        }
      }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(
      this,
      _shadowTablesMap,
      _viewTables,
      "focus_sessions",
      "focus_segments",
    )
  }

  public override fun clearAllTables() {
    super.performClear(false, "focus_sessions", "focus_segments")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(FocusDao::class, FocusDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(
    autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
  ): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun focusDao(): FocusDao = _focusDao.value
}

package mik.example.sound_recorder

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import mik.example.sound_recorder.listeners.OnDatabaseChangedListener
import java.util.*

/**
 * Created by Daniel on 12/29/2014.
 */
class DBHelper(val context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    object DBHelperItem : BaseColumns {
        const val TABLE_NAME = "saved_recordings"
        const val COLUMN_NAME_RECORDING_NAME = "recording_name"
        const val COLUMN_NAME_RECORDING_FILE_PATH = "file_path"
        const val COLUMN_NAME_RECORDING_LENGTH = "length"
        const val COLUMN_NAME_TIME_ADDED = "time_added"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun getItemAt(position: Int): RecordingItem? {
        val db = readableDatabase
        val projection = arrayOf(
            BaseColumns._ID,
            DBHelperItem.COLUMN_NAME_RECORDING_NAME,
            DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH,
            DBHelperItem.COLUMN_NAME_RECORDING_LENGTH,
            DBHelperItem.COLUMN_NAME_TIME_ADDED
        )
        val c = db.query(DBHelperItem.TABLE_NAME, projection, null, null, null, null, null)
        if (c.moveToPosition(position)) {
            val item = RecordingItem()
            item.id = c.getInt(c.getColumnIndex(BaseColumns._ID))
            item.name = c.getString(c.getColumnIndex(DBHelperItem.COLUMN_NAME_RECORDING_NAME))
            item.filePath =
                c.getString(c.getColumnIndex(DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH))
            item.length = c.getInt(c.getColumnIndex(DBHelperItem.COLUMN_NAME_RECORDING_LENGTH))
            item.time = c.getLong(c.getColumnIndex(DBHelperItem.COLUMN_NAME_TIME_ADDED))
            c.close()
            return item
        }
        return null
    }

    fun removeItemWithId(id: Int) {
        val db = writableDatabase
        val whereArgs = arrayOf(id.toString())
        db.delete(DBHelperItem.TABLE_NAME, "_ID=?", whereArgs)
    }

    val count: Int
        get() {
            val db = readableDatabase
            val projection = arrayOf(BaseColumns._ID)
            val c = db.query(DBHelperItem.TABLE_NAME, projection, null, null, null, null, null)
            val count = c.count
            c.close()
            return count
        }

    inner class RecordingComparator : Comparator<RecordingItem> {
        override fun compare(item1: RecordingItem, item2: RecordingItem): Int {
            val o1 = item1.time
            val o2 = item2.time
            return o2.compareTo(o1)
        }
    }

    fun addRecording(recordingName: String?, filePath: String?, length: Long): Long {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(DBHelperItem.COLUMN_NAME_RECORDING_NAME, recordingName)
        cv.put(DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH, filePath)
        cv.put(DBHelperItem.COLUMN_NAME_RECORDING_LENGTH, length)
        cv.put(DBHelperItem.COLUMN_NAME_TIME_ADDED, System.currentTimeMillis())
        val rowId = db.insert(DBHelperItem.TABLE_NAME, null, cv)
        if (mOnDatabaseChangedListener != null) {
            mOnDatabaseChangedListener!!.onNewDatabaseEntryAdded()
        }
        return rowId
    }

    fun renameItem(item: RecordingItem, recordingName: String?, filePath: String?) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(DBHelperItem.COLUMN_NAME_RECORDING_NAME, recordingName)
        cv.put(DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH, filePath)
        db.update(
            DBHelperItem.TABLE_NAME, cv,
            BaseColumns._ID + "=" + item.id, null
        )
        if (mOnDatabaseChangedListener != null) {
            mOnDatabaseChangedListener!!.onDatabaseEntryRenamed()
        }
    }

    fun restoreRecording(item: RecordingItem): Long {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(DBHelperItem.COLUMN_NAME_RECORDING_NAME, item.name)
        cv.put(DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH, item.filePath)
        cv.put(DBHelperItem.COLUMN_NAME_RECORDING_LENGTH, item.length)
        cv.put(DBHelperItem.COLUMN_NAME_TIME_ADDED, item.time)
        cv.put(BaseColumns._ID, item.id)
        val rowId = db.insert(DBHelperItem.TABLE_NAME, null, cv)
        if (mOnDatabaseChangedListener != null) {
            //mOnDatabaseChangedListener.onNewDatabaseEntryAdded();
        }
        return rowId
    }

    companion object {
        private const val LOG_TAG = "DBHelper"
        private var mOnDatabaseChangedListener: OnDatabaseChangedListener? = null
        const val DATABASE_NAME = "saved_recordings.db"
        private const val DATABASE_VERSION = 1
        private const val TEXT_TYPE = " TEXT"
        private const val COMMA_SEP = ","
        private const val SQL_CREATE_ENTRIES = "CREATE TABLE " + DBHelperItem.TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                DBHelperItem.COLUMN_NAME_RECORDING_NAME + TEXT_TYPE + COMMA_SEP +
                DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH + TEXT_TYPE + COMMA_SEP +
                DBHelperItem.COLUMN_NAME_RECORDING_LENGTH + " INTEGER " + COMMA_SEP +
                DBHelperItem.COLUMN_NAME_TIME_ADDED + " INTEGER " + ")"
        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + DBHelperItem.TABLE_NAME
        fun setOnDatabaseChangedListener(listener: OnDatabaseChangedListener?) {
            mOnDatabaseChangedListener = listener
        }
    }
}
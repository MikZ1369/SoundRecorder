package mik.example.sound_recorder.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mik.example.sound_recorder.DBHelper
import mik.example.sound_recorder.R
import mik.example.sound_recorder.RecordingItem
import mik.example.sound_recorder.adapters.FileViewerAdapter.RecordingsViewHolder
import mik.example.sound_recorder.fragments.PlaybackFragment
import mik.example.sound_recorder.listeners.OnDatabaseChangedListener
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class FileViewerAdapter(var mContext: Context, linearLayoutManager: LinearLayoutManager) :
    RecyclerView.Adapter<RecordingsViewHolder>(), OnDatabaseChangedListener {
    private val mDatabase: DBHelper
    var item: RecordingItem? = null
    var llm: LinearLayoutManager
    override fun onBindViewHolder(holder: RecordingsViewHolder, position: Int) {
        item = getItem(position)
        val itemDuration = item!!.length.toLong()
        val minutes = TimeUnit.MILLISECONDS.toMinutes(itemDuration)
        val seconds = (TimeUnit.MILLISECONDS.toSeconds(itemDuration)
                - TimeUnit.MINUTES.toSeconds(minutes))
        holder.vName.text = item!!.name
        holder.vLength.text = String.format("%02d:%02d", minutes, seconds)
        holder.vDateAdded.text = DateUtils.formatDateTime(
            mContext,
            item!!.time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR
        )

        // define an on click listener to open PlaybackFragment
        holder.cardView.setOnClickListener {
            try {
                val playbackFragment = PlaybackFragment().newInstance(getItem(holder.position))
                val transaction = (mContext as FragmentActivity)
                    .supportFragmentManager
                    .beginTransaction()
                playbackFragment.show(transaction, "dialog_playback")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "exception", e)
            }
        }
        holder.cardView.setOnLongClickListener {
            val entrys = ArrayList<String>()
            entrys.add(mContext.getString(R.string.dialog_file_share))
            entrys.add(mContext.getString(R.string.dialog_file_rename))
            entrys.add(mContext.getString(R.string.dialog_file_delete))
            val items = entrys.toTypedArray<CharSequence>()


            // File delete confirm
            val builder = AlertDialog.Builder(mContext)
            builder.setTitle(mContext.getString(R.string.dialog_title_options))
            builder.setItems(items) { dialog, item ->
                if (item == 0) {
                    shareFileDialog(holder.position)
                }
                if (item == 1) {
                    renameFileDialog(holder.position)
                } else if (item == 2) {
                    deleteFileDialog(holder.position)
                }
            }
            builder.setCancelable(true)
            builder.setNegativeButton(
                mContext.getString(R.string.dialog_action_cancel)
            ) { dialog, id -> dialog.cancel() }
            val alert = builder.create()
            alert.show()
            false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingsViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.card_view, parent, false)
        mContext = parent.context
        return RecordingsViewHolder(itemView)
    }

    class RecordingsViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var vName: TextView
        var vLength: TextView
        var vDateAdded: TextView
        var cardView: View

        init {
            vName = v.findViewById<View>(R.id.file_name_text) as TextView
            vLength = v.findViewById<View>(R.id.file_length_text) as TextView
            vDateAdded = v.findViewById<View>(R.id.file_date_added_text) as TextView
            cardView = v.findViewById(R.id.card_view)
        }
    }

    override fun getItemCount(): Int {
        return mDatabase.count
    }

    fun getItem(position: Int): RecordingItem {
        return mDatabase.getItemAt(position)!!
    }

    override fun onNewDatabaseEntryAdded() {
        //item added to top of the list
        notifyItemInserted(itemCount - 1)
        llm.scrollToPosition(itemCount - 1)
    }

    override fun onDatabaseEntryRenamed() {}
    fun remove(position: Int) {
        //remove item from database, recyclerview and storage

        //delete file from storage
        val file = File(getItem(position).filePath)
        file.delete()
        Toast.makeText(
            mContext, String.format(
                mContext.getString(R.string.toast_file_delete),
                getItem(position).name
            ),
            Toast.LENGTH_SHORT
        ).show()
        mDatabase.removeItemWithId(getItem(position).id)
        notifyItemRemoved(position)
    }

    fun removeOutOfApp(filePath: String?) {
        //user deletes a saved recording out of the application through another application
    }

    fun rename(position: Int, name: String) {
        //rename a file
        var mFilePath = Environment.getExternalStorageDirectory().absolutePath
        mFilePath += "/SoundRecorder/$name"
        val f = File(mFilePath)
        if (f.exists() && !f.isDirectory) {
            //file name is not unique, cannot rename file.
            Toast.makeText(
                mContext, String.format(mContext.getString(R.string.toast_file_exists), name),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            //file name is unique, rename file
            val oldFilePath = File(getItem(position).filePath)
            oldFilePath.renameTo(f)
            mDatabase.renameItem(getItem(position), name, mFilePath)
            notifyItemChanged(position)
        }
    }

    fun shareFileDialog(position: Int) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(getItem(position).filePath)))
        shareIntent.type = "audio/mp4"
        mContext.startActivity(
            Intent.createChooser(
                shareIntent,
                mContext.getText(R.string.send_to)
            )
        )
    }

    fun renameFileDialog(position: Int) {
        // File rename dialog
        val renameFileBuilder = AlertDialog.Builder(mContext)
        val inflater = LayoutInflater.from(mContext)
        val view = inflater.inflate(R.layout.dialog_rename_file, null)
        val input = view.findViewById<View>(R.id.new_name) as EditText
        renameFileBuilder.setTitle(mContext.getString(R.string.dialog_title_rename))
        renameFileBuilder.setCancelable(true)
        renameFileBuilder.setPositiveButton(
            mContext.getString(R.string.dialog_action_ok)
        ) { dialog, id ->
            try {
                val value = input.text.toString().trim { it <= ' ' } + ".mp4"
                rename(position, value)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "exception", e)
            }
            dialog.cancel()
        }
        renameFileBuilder.setNegativeButton(
            mContext.getString(R.string.dialog_action_cancel)
        ) { dialog, id -> dialog.cancel() }
        renameFileBuilder.setView(view)
        val alert = renameFileBuilder.create()
        alert.show()
    }

    fun deleteFileDialog(position: Int) {
        // File delete confirm
        val confirmDelete = AlertDialog.Builder(mContext)
        confirmDelete.setTitle(mContext.getString(R.string.dialog_title_delete))
        confirmDelete.setMessage(mContext.getString(R.string.dialog_text_delete))
        confirmDelete.setCancelable(true)
        confirmDelete.setPositiveButton(
            mContext.getString(R.string.dialog_action_yes)
        ) { dialog, id ->
            try {
                //remove item from database, recyclerview, and storage
                remove(position)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "exception", e)
            }
            dialog.cancel()
        }
        confirmDelete.setNegativeButton(
            mContext.getString(R.string.dialog_action_no)
        ) { dialog, id -> dialog.cancel() }
        val alert = confirmDelete.create()
        alert.show()
    }

    companion object {
        private const val LOG_TAG = "FileViewerAdapter"
    }

    init {
        mDatabase = DBHelper(mContext)
        DBHelper.setOnDatabaseChangedListener(this)
        llm = linearLayoutManager
    }
}
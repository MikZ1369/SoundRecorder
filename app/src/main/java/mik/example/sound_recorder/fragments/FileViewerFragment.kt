package mik.example.sound_recorder.fragments

import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mik.example.sound_recorder.R
import mik.example.sound_recorder.adapters.FileViewerAdapter

/**
 * Created by Daniel on 12/23/2014.
 */
class FileViewerFragment : Fragment() {
    private var position = 0
    private var mFileViewerAdapter: FileViewerAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        position = arguments!!.getInt(ARG_POSITION)
        observer.startWatching()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_file_viewer, container, false)
        val mRecyclerView = v.findViewById<View>(R.id.recyclerView) as RecyclerView
        mRecyclerView.setHasFixedSize(true)
        val llm = LinearLayoutManager(activity)
        llm.orientation = LinearLayoutManager.VERTICAL

        //newest to oldest order (database stores from oldest to newest)
        llm.reverseLayout = true
        llm.stackFromEnd = true
        mRecyclerView.layoutManager = llm
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mFileViewerAdapter = FileViewerAdapter(activity!!, llm)
        mRecyclerView.adapter = mFileViewerAdapter
        return v
    }

    var observer: FileObserver = object : FileObserver(
        Environment.getExternalStorageDirectory().toString()
                + "/SoundRecorder"
    ) {
        // set up a file observer to watch this directory on sd card
        override fun onEvent(event: Int, file: String?) {
            if (event == DELETE) {
                // user deletes a recording file out of the app
                val filePath = (Environment.getExternalStorageDirectory().toString()
                        + "/SoundRecorder" + file + "]")
                Log.d(
                    LOG_TAG, "File deleted ["
                            + Environment.getExternalStorageDirectory().toString()
                            + "/SoundRecorder" + file + "]"
                )

                // remove file from database and recyclerview
                mFileViewerAdapter!!.removeOutOfApp(filePath)
            }
        }
    }

    companion object {
        private const val ARG_POSITION = "position"
        private const val LOG_TAG = "FileViewerFragment"
        fun newInstance(position: Int): FileViewerFragment {
            val f = FileViewerFragment()
            val b = Bundle()
            b.putInt(ARG_POSITION, position)
            f.arguments = b
            return f
        }
    }
}
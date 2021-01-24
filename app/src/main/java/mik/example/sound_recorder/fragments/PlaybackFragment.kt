package mik.example.sound_recorder.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.ColorFilter
import android.graphics.LightingColorFilter
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mik.example.sound_recorder.R
import mik.example.sound_recorder.RecordingItem
import java.io.IOException
import java.util.concurrent.TimeUnit


class PlaybackFragment : DialogFragment() {
    private var item: RecordingItem? = null
    private val mHandler = Handler()
    private var mMediaPlayer: MediaPlayer? = null
    private var mSeekBar: SeekBar? = null
    private var mPlayButton: FloatingActionButton? = null
    private var mCurrentProgressTextView: TextView? = null
    private var mFileNameTextView: TextView? = null
    private var mFileLengthTextView: TextView? = null

    //stores whether or not the mediaplayer is currently playing audio
    private var isPlaying = false

    //stores minutes and seconds of the length of the file.
    var minutes: Long = 0
    var seconds: Long = 0
    fun newInstance(item: RecordingItem?): PlaybackFragment {
        val f = PlaybackFragment()
        val b = Bundle()
        b.putParcelable(ARG_ITEM, item)
        f.arguments = b
        return f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        item = arguments!!.getParcelable(ARG_ITEM)
        val itemDuration = item!!.length.toLong()
        minutes = TimeUnit.MILLISECONDS.toMinutes(itemDuration)
        seconds = (TimeUnit.MILLISECONDS.toSeconds(itemDuration)
                - TimeUnit.MINUTES.toSeconds(minutes))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val builder = AlertDialog.Builder(activity)
        val view = activity!!.layoutInflater.inflate(R.layout.fragment_media_playback, null)
        mFileNameTextView = view.findViewById<View>(R.id.file_name_text_view) as TextView
        mFileLengthTextView = view.findViewById<View>(R.id.file_length_text_view) as TextView
        mCurrentProgressTextView =
            view.findViewById<View>(R.id.current_progress_text_view) as TextView
        mSeekBar = view.findViewById<View>(R.id.seekbar) as SeekBar
        val filter: ColorFilter = LightingColorFilter(
            resources.getColor(R.color.primary),
            resources.getColor(R.color.primary)
        )
        mSeekBar!!.progressDrawable.colorFilter = filter
        mSeekBar!!.thumb.colorFilter = filter
        mSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mMediaPlayer != null && fromUser) {
                    mMediaPlayer!!.seekTo(progress)
                    mHandler.removeCallbacks(mRunnable)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(
                        mMediaPlayer!!.currentPosition.toLong()
                    )
                    val seconds = (TimeUnit.MILLISECONDS.toSeconds(
                        mMediaPlayer!!.currentPosition.toLong()
                    )
                            - TimeUnit.MINUTES.toSeconds(minutes))
                    mCurrentProgressTextView!!.text = String.format("%02d:%02d", minutes, seconds)
                    updateSeekBar()
                } else if (mMediaPlayer == null && fromUser) {
                    prepareMediaPlayerFromPoint(progress)
                    updateSeekBar()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                if (mMediaPlayer != null) {
                    // remove message Handler from updating progress bar
                    mHandler.removeCallbacks(mRunnable)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (mMediaPlayer != null) {
                    mHandler.removeCallbacks(mRunnable)
                    mMediaPlayer!!.seekTo(seekBar.progress)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(
                        mMediaPlayer!!.currentPosition.toLong()
                    )
                    val seconds = (TimeUnit.MILLISECONDS.toSeconds(
                        mMediaPlayer!!.currentPosition.toLong()
                    )
                            - TimeUnit.MINUTES.toSeconds(minutes))
                    mCurrentProgressTextView!!.text = String.format("%02d:%02d", minutes, seconds)
                    updateSeekBar()
                }
            }
        })
        mPlayButton = view.findViewById<View>(R.id.fab_play) as FloatingActionButton
        mPlayButton!!.setOnClickListener {
            onPlay(isPlaying)
            isPlaying = !isPlaying
        }
        mFileNameTextView!!.text = item!!.name
        mFileLengthTextView!!.text = String.format("%02d:%02d", minutes, seconds)
        builder.setView(view)

        // request a window without the title
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        return builder.create()
    }

    override fun onStart() {
        super.onStart()

        //set transparent background
        val window = dialog!!.window
        window!!.setBackgroundDrawableResource(android.R.color.transparent)

        //disable buttons from dialog
        val alertDialog = dialog as AlertDialog?
        alertDialog!!.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false
        alertDialog.getButton(Dialog.BUTTON_NEGATIVE).isEnabled = false
        alertDialog.getButton(Dialog.BUTTON_NEUTRAL).isEnabled = false
    }

    override fun onPause() {
        super.onPause()
        if (mMediaPlayer != null) {
            stopPlaying()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mMediaPlayer != null) {
            stopPlaying()
        }
    }

    // Play start/stop
    private fun onPlay(isPlaying: Boolean) {
        if (!isPlaying) {
            //currently MediaPlayer is not playing audio
            if (mMediaPlayer == null) {
                startPlaying() //start from beginning
            } else {
                resumePlaying() //resume the currently paused MediaPlayer
            }
        } else {
            //pause the MediaPlayer
            pausePlaying()
        }
    }

    private fun startPlaying() {
        mPlayButton!!.setImageResource(R.drawable.ic_media_pause)
        mMediaPlayer = MediaPlayer()
        try {
            mMediaPlayer!!.setDataSource(item!!.filePath)
            mMediaPlayer!!.prepare()
            mSeekBar!!.max = mMediaPlayer!!.duration
            mMediaPlayer!!.setOnPreparedListener { mMediaPlayer!!.start() }
        } catch (e: IOException) {
            Log.e(LOG_TAG, "prepare() failed")
        }
        mMediaPlayer!!.setOnCompletionListener { stopPlaying() }
        updateSeekBar()

        //keep screen on while playing audio
        activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun prepareMediaPlayerFromPoint(progress: Int) {
        //set mediaPlayer to start from middle of the audio file
        mMediaPlayer = MediaPlayer()
        try {
            mMediaPlayer!!.setDataSource(item!!.filePath)
            mMediaPlayer!!.prepare()
            mSeekBar!!.max = mMediaPlayer!!.duration
            mMediaPlayer!!.seekTo(progress)
            mMediaPlayer!!.setOnCompletionListener { stopPlaying() }
        } catch (e: IOException) {
            Log.e(LOG_TAG, "prepare() failed")
        }

        //keep screen on while playing audio
        activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pausePlaying() {
        mPlayButton!!.setImageResource(R.drawable.ic_media_play)
        mHandler.removeCallbacks(mRunnable)
        mMediaPlayer!!.pause()
    }

    private fun resumePlaying() {
        mPlayButton!!.setImageResource(R.drawable.ic_media_pause)
        mHandler.removeCallbacks(mRunnable)
        mMediaPlayer!!.start()
        updateSeekBar()
    }

    private fun stopPlaying() {
        mPlayButton!!.setImageResource(R.drawable.ic_media_play)
        mHandler.removeCallbacks(mRunnable)
        mMediaPlayer!!.stop()
        mMediaPlayer!!.reset()
        mMediaPlayer!!.release()
        mMediaPlayer = null
        mSeekBar!!.progress = mSeekBar!!.max
        isPlaying = !isPlaying
        mCurrentProgressTextView!!.text = mFileLengthTextView!!.text
        mSeekBar!!.progress = mSeekBar!!.max

        //allow the screen to turn off again once audio is finished playing
        activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    //updating mSeekBar
    private val mRunnable = Runnable {
        if (mMediaPlayer != null) {
            val mCurrentPosition = mMediaPlayer!!.currentPosition
            mSeekBar!!.progress = mCurrentPosition
            val minutes = TimeUnit.MILLISECONDS.toMinutes(mCurrentPosition.toLong())
            val seconds = (TimeUnit.MILLISECONDS.toSeconds(mCurrentPosition.toLong())
                    - TimeUnit.MINUTES.toSeconds(minutes))
            mCurrentProgressTextView!!.text = String.format("%02d:%02d", minutes, seconds)
            updateSeekBar()
        }
    }

    private fun updateSeekBar() {
        mHandler.postDelayed(mRunnable, 1000)
    }

    companion object {
        private const val LOG_TAG = "PlaybackFragment"
        private const val ARG_ITEM = "recording_item"
    }
}
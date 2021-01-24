package mik.example.sound_recorder

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import mik.example.sound_recorder.MySharedPreferences.getPrefHighQuality
import mik.example.sound_recorder.activities.MainActivity
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class RecordingService : Service() {
    private var mFileName: String? = null
    private var mFilePath: String? = null
    private var mRecorder: MediaRecorder? = null
    private var mDatabase: DBHelper? = null
    private var mStartingTimeMillis: Long = 0
    private var mElapsedMillis: Long = 0
    private var mElapsedSeconds = 0
    private val onTimerChangedListener: OnTimerChangedListener? = null
    private var mTimer: Timer? = null
    private var mIncrementTimerTask: TimerTask? = null
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    interface OnTimerChangedListener {
        fun onTimerChanged(seconds: Int)
    }

    override fun onCreate() {
        super.onCreate()
        mDatabase = DBHelper(applicationContext)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startRecording()
        return START_STICKY
    }

    override fun onDestroy() {
        if (mRecorder != null) {
            stopRecording()
        }
        super.onDestroy()
    }

    fun startRecording() {
        setFileNameAndPath()
        mRecorder = MediaRecorder()
        mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mRecorder!!.setOutputFile(mFilePath)
        mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mRecorder!!.setAudioChannels(1)
        if (getPrefHighQuality(this)) {
            mRecorder!!.setAudioSamplingRate(44100)
            mRecorder!!.setAudioEncodingBitRate(192000)
        }
        try {
            mRecorder!!.prepare()
            mRecorder!!.start()
            mStartingTimeMillis = System.currentTimeMillis()

            //startTimer();
            //startForeground(1, createNotification());
        } catch (e: IOException) {
            Log.e(LOG_TAG, "prepare() failed")
        }
    }

    fun setFileNameAndPath() {
        var count = 0
        var f: File
        do {
            count++
            mFileName = (getString(R.string.default_file_name)
                    + "_" + (mDatabase!!.count + count) + ".mp4")
            mFilePath = Environment.getDataDirectory().absolutePath
            mFilePath += "/data/mik.example.sound_recorder/$mFileName"
            f = File(mFilePath)
        } while (f.exists() && !f.isDirectory)
    }

    fun stopRecording() {
        mRecorder!!.stop()
        mElapsedMillis = System.currentTimeMillis() - mStartingTimeMillis
        mRecorder!!.release()
        Toast.makeText(
            this,
            getString(R.string.toast_recording_finish) + " " + mFilePath,
            Toast.LENGTH_LONG
        ).show()

        //remove notification
        if (mIncrementTimerTask != null) {
            mIncrementTimerTask!!.cancel()
            mIncrementTimerTask = null
        }
        mRecorder = null
        try {
            mDatabase!!.addRecording(mFileName, mFilePath, mElapsedMillis)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "exception", e)
        }
    }

    private fun startTimer() {
        mTimer = Timer()
        mIncrementTimerTask = object : TimerTask() {
            override fun run() {
                mElapsedSeconds++
                onTimerChangedListener?.onTimerChanged(mElapsedSeconds)
                val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                mgr.notify(1, createNotification())
            }
        }
        mTimer!!.scheduleAtFixedRate(mIncrementTimerTask, 1000, 1000)
    }

    private fun createNotification(): Notification {
        val mBuilder = NotificationCompat.Builder(applicationContext)
            .setSmallIcon(R.drawable.ic_mic_white_36dp)
            .setContentTitle(getString(R.string.notification_recording))
            .setContentText(mTimerFormat.format(mElapsedSeconds * 1000))
            .setOngoing(true)
        mBuilder.setContentIntent(
            PendingIntent.getActivities(
                applicationContext, 0, arrayOf(
                    Intent(
                        applicationContext, MainActivity::class.java
                    )
                ), 0
            )
        )
        return mBuilder.build()
    }

    companion object {
        private const val LOG_TAG = "RecordingService"
        private val mTimerFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
    }
}
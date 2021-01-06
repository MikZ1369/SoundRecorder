package mik.example.sound_recorder.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import mik.example.sound_recorder.R
import mik.example.sound_recorder.activities.MainActivity
import mik.example.sound_recorder.fragments.FileViewerFragment
import mik.example.sound_recorder.fragments.RecordFragment

class MainActivity : AppCompatActivity() {
    private var pager: ViewPager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pager = findViewById<View>(R.id.pager) as ViewPager
        pager!!.adapter = MyAdapter(supportFragmentManager)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        toolbar.popupTheme = R.style.ThemeOverlay_AppCompat_Light
        if (toolbar != null) {
            setSupportActionBar(toolbar)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(
                        this@MainActivity,
                        "Permission denied to read your External storage",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // Handle presses on the action bar items
        return when (item.itemId) {
            R.id.action_settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class MyAdapter(fm: FragmentManager?) : FragmentPagerAdapter(
        fm!!
    ) {
        private val titles = arrayOf(
            getString(R.string.tab_title_record),
            getString(R.string.tab_title_saved_recordings)
        )

        override fun getItem(position: Int): Fragment {
            when (position) {
                0 -> {
                    return RecordFragment.newInstance(position)
                }
                1 -> {
                    return FileViewerFragment.newInstance(position)
                }
            }
            return Fragment()
        }

        override fun getCount(): Int {
            return titles.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return titles[position]
        }
    }

    companion object {
        private val LOG_TAG = MainActivity::class.java.simpleName
    }
}
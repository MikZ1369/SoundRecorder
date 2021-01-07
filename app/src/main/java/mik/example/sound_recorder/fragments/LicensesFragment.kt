package mik.example.sound_recorder.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import mik.example.sound_recorder.R

/**
 * Created by Daniel on 1/3/2015.
 */
class LicensesFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogInflater = activity!!.layoutInflater
        val openSourceLicensesView = dialogInflater.inflate(R.layout.fragment_licenses, null)
        val dialogBuilder = AlertDialog.Builder(activity)
        dialogBuilder.setView(openSourceLicensesView)
            .setTitle(getString(R.string.dialog_title_licenses))
            .setNeutralButton(android.R.string.ok, null)
        return dialogBuilder.create()
    }
}
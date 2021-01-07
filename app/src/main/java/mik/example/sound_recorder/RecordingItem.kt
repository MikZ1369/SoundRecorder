package mik.example.sound_recorder

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by Daniel on 12/30/2014.
 */
class RecordingItem : Parcelable {
    var name // file name
            : String? = null
    var filePath //file path
            : String? = null
    var id //id in database
            = 0
    var length // length of recording in seconds
            = 0
    var time // date/time of the recording
            : Long = 0

    constructor() {}
    constructor(`in`: Parcel) {
        name = `in`.readString()
        filePath = `in`.readString()
        id = `in`.readInt()
        length = `in`.readInt()
        time = `in`.readLong()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeInt(length)
        dest.writeLong(time)
        dest.writeString(filePath)
        dest.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        val CREATOR: Parcelable.Creator<RecordingItem?> =
            object : Parcelable.Creator<RecordingItem?> {
                override fun createFromParcel(`in`: Parcel): RecordingItem? {
                    return RecordingItem(`in`)
                }

                override fun newArray(size: Int): Array<RecordingItem?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
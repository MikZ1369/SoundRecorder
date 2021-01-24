package mik.example.sound_recorder.listeners


interface OnDatabaseChangedListener {
    fun onNewDatabaseEntryAdded()
    fun onDatabaseEntryRenamed()
}
package com.tsu.hitselka.activity_game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.tsu.hitselka.model.*
import java.util.*

class GameViewModel : ViewModel() {
    private val db =
        Firebase.database("https://hitselka-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val database: DatabaseReference = db.reference

    private val _isRussian = MutableLiveData<Boolean>()
    val isRussian: LiveData<Boolean> get() = _isRussian

    private val _isMusicOn = MutableLiveData<Boolean>()
    val isMusicOn: LiveData<Boolean> get() = _isMusicOn

    private val _isSoundOn = MutableLiveData<Boolean>()
    val isSoundOn: LiveData<Boolean> get() = _isSoundOn

    init {
        val uid = SharedPrefs.getUID()
        setDatabaseListener(uid)
    }

    private fun setDatabaseListener(uid: String) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.child("players").child(uid)
                if (data.value == null) {
                    GameLogic.firstRun(uid)
                } else {
                    val settings = data.child("settings").getValue<Settings>()
                    _isRussian.value = settings?.lang ?: return
                    _isMusicOn.value = settings.music
                    _isSoundOn.value = settings.sound
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }
        database.addValueEventListener(listener)
    }
}
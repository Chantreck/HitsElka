package com.tsu.hitselka.activity_login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tsu.hitselka.R
import com.tsu.hitselka.activity_game.GameActivity
import com.tsu.hitselka.databinding.ActivityLoginBinding
import com.tsu.hitselka.model.SharedPrefs
import com.tsu.hitselka.model.setFullscreen

class LoginActivity : AppCompatActivity(R.layout.activity_login) {
    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = Firebase.auth
        SharedPrefs.init(this)
        checkLoginState()
        setFullscreen()

        binding.signInButton.setOnClickListener {
            login()
        }
    }

    private fun login() {
        auth.createUserWithEmailAndPassword("vladnetaev@gmail.com", "chantreck")
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    SharedPrefs.saveUID("IqNVlP1F9lf1e3NwVhlhW7IYLd52")
                    checkLoginState()
                }
            }
    }

    private fun checkLoginState() {
        if (auth.currentUser != null) {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        } else {
            binding.signInButton.visibility = View.VISIBLE
        }
    }
}
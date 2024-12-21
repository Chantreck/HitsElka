package com.tsu.hitselka.activity_game

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tsu.hitselka.R
import com.tsu.hitselka.activity_settings.SettingsActivity
import com.tsu.hitselka.activity_chest.ChestActivity
import com.tsu.hitselka.activity_gifts.GiftsActivity
import com.tsu.hitselka.databinding.ActivityGameBinding
import com.tsu.hitselka.databinding.ControlsBinding
import com.tsu.hitselka.inventory.InventoryActivity
import com.tsu.hitselka.model.SharedPrefs
import com.tsu.hitselka.model.setFullscreen
import com.tsu.hitselka.activity_record_book.RecordBookActivity
import com.tsu.hitselka.model.GameData
import com.tsu.hitselka.model.GameLogic
import java.util.*
import kotlin.math.floor
import kotlin.math.log10

class GameActivity : AppCompatActivity(R.layout.controls) {
    private val binding by lazy { ActivityGameBinding.inflate(layoutInflater) }
    private val controls by lazy { ControlsBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<GameViewModel>()
    private lateinit var surface: SurfaceView
    private lateinit var clickSound: MediaPlayer
    private lateinit var backgroundMusic: MediaPlayer

    private var isMusicOn = false
    private var isSoundOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GameLogic.init()

        surface = SurfaceView(this)
        setContentView(surface)

        val lp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        addContentView(binding.root, lp)
        addContentView(controls.root, lp)

        initListeners()
        initObservers()
        setProfileImage()
        setFullscreen()
        setMusic()
    }

    private fun initListeners() {
        controls.yearSelectorImageView.setOnClickListener {
            Toast.makeText(this, "Скоро в игре", Toast.LENGTH_SHORT).show()
            playClickSound()
        }

        controls.achievementsImageView.setOnClickListener {
            Toast.makeText(this, "Скоро в игре", Toast.LENGTH_SHORT).show()
            playClickSound()
        }

        controls.giftImageView.setOnClickListener {
            playClickSound()
            val intent = Intent(this, GiftsActivity::class.java)
            startActivity(intent)
        }

        controls.wandView.setOnClickListener {
            playClickSound()
            val intent = Intent(this, RecordBookActivity::class.java)
            startActivity(intent)
        }

        controls.settingsImageView.setOnClickListener {
            playClickSound()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        controls.shopImageView.setOnClickListener {
            playClickSound()
            val intent = Intent(this, ChestActivity::class.java)
            startActivity(intent)
        }

        controls.playImageView.setOnClickListener {
            Toast.makeText(this, "Скоро в игре", Toast.LENGTH_SHORT).show()
            playClickSound()
        }
        controls.inventoryImageView.setOnClickListener {
            playClickSound()
            val intent=Intent(this,InventoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initObservers() {
        GameData.stats.observe(this) { stats ->
            val coefficient = when {
                stats.currentLevel % 100 == 99L -> 700
                stats.currentLevel % 10 == 9L -> 400
                else -> 100
            }
            val levelNeeded = floor(log10(stats.currentLevel.toDouble() + 1) * coefficient * (stats.currentLevel.div(10) + 1)).toLong()
            val progress = stats.currentLevelWandsUsed * 100 / levelNeeded
            val width = getLevelBarWidth(progress)
            if (width > 0) {
                controls.levelFillView.visibility = View.VISIBLE
                controls.levelFillView.layoutParams.width = width
            } else {
                controls.levelFillView.visibility = View.INVISIBLE
            }
        }

        viewModel.isRussian.observe(this) { state ->
            val locale = if (state) Locale("ru", "RU") else Locale.ENGLISH
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }

        viewModel.isMusicOn.observe(this) { state ->
            isMusicOn = state
            if (state) {
                backgroundMusic.start()
            } else {
                backgroundMusic.pause()
            }
        }

        viewModel.isSoundOn.observe(this) { state ->
            isSoundOn = state
        }

        GameData.stats.observe(this) { stats ->
            controls.levelTextView.text = stats.currentLevel.toString()
        }

        GameData.resources.observe(this) { resources ->
            controls.wandTextView.text = resources.wands.toString()
            controls.moneyTextView.text = resources.moneys.toString()
            controls.rubyTextView.text = resources.rubies.toString()
        }
    }

    private fun setProfileImage() {
        controls.profileImageView.clipToOutline = true

        Glide.with(this)
            .load("https://sun9-69.userapi.com/impg/AVrUevQsGu1A77irdQ2NStpmeAAXRzYB9yeI_g/mTBYSuT4MdQ.jpg?size=1442x2160&quality=95&sign=26c66101a053dafca5dd2db1f0651a44&type=album")
            .override(Target.SIZE_ORIGINAL)
            .into(controls.profileImageView)
    }

    private fun setMusic() {
        clickSound = MediaPlayer.create(this, R.raw.click)

        backgroundMusic = MediaPlayer.create(this, R.raw.music)
        backgroundMusic.isLooping = true
        backgroundMusic.setVolume(20f, 20f)
        playMusic()
    }

    private fun playMusic() {
        backgroundMusic.start()
        if (!isMusicOn) {
            backgroundMusic.pause()
        }
    }

    private fun playClickSound() {
        if (isSoundOn) {
            clickSound.start()
        }
    }

    override fun onRestart() {
        playMusic()
        super.onRestart()
    }

    override fun onResume() {
        if (SharedPrefs.getUID().isBlank()) {
            finish()
        }

        surface.start()
        super.onResume()
    }

    override fun onPause() {
        surface.stop()
        super.onPause()
    }

    override fun onDestroy() {
        backgroundMusic.stop()
        backgroundMusic.release()
        clickSound.release()
        super.onDestroy()
    }

    private fun getLevelBarWidth(percentage: Long): Int {
        val maxWidth = resources.getDimensionPixelSize(R.dimen.level_progress_width)
        return maxWidth * percentage.toInt() / 100
    }
}
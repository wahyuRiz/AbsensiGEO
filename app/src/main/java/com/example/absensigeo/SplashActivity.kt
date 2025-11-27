package com.example.absensigeo

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

/**
 * Activity splash screen yang menampilkan animasi saat aplikasi pertama kali dibuka.
 *
 * Fitur utama:
 * - Menampilkan animasi fade in dan fade out
 * - Memeriksa apakah aplikasi pertama kali dijalankan
 * - Mengarahkan ke GetStartedActivity atau LoginActivity sesuai kondisi
 * - Menyimpan status first run di SharedPreferences
 */
class SplashActivity : AppCompatActivity() {

    // Nama file SharedPreferences
    private val PREFS_NAME = "app_prefs"
    // Key untuk menyimpan status first run
    private val KEY_FIRST_RUN = "isFirstRun"

    /**
     * Dipanggil saat activity pertama kali dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Inisialisasi view dan animasi
        setupSplashAnimation()
    }

    /**
     * Menyiapkan dan menjalankan animasi splash screen.
     */
    private fun setupSplashAnimation() {
        val splashImage = findViewById<ImageView>(R.id.splashImage)

        // Animasi fade in saat splash screen muncul
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        splashImage.startAnimation(fadeIn)

        // Handler untuk mengatur durasi splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            // Animasi fade out setelah 1.5 detik
            val fadeSlideOut = AnimationUtils.loadAnimation(this, R.anim.fade_slide_out_left)
            splashImage.startAnimation(fadeSlideOut)

            // Listener untuk animasi fade out
            fadeSlideOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                /**
                 * Dipanggil saat animasi selesai.
                 */
                override fun onAnimationEnd(animation: Animation?) {
                    navigateToNextActivity()
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }, 1500) // Delay 1.5 detik sebelum memulai animasi fade out
    }

    /**
     * Mengarahkan ke activity berikutnya berdasarkan status first run.
     */
    private fun navigateToNextActivity() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean(KEY_FIRST_RUN, true)

        if (isFirstRun) {
            // Jika pertama kali dijalankan, arahkan ke GetStartedActivity
            prefs.edit {
                putBoolean(KEY_FIRST_RUN, false)
                commit()
            }
            startActivity(Intent(this, GetStartedActivity::class.java))
        } else {
            // Jika sudah pernah dijalankan, arahkan langsung ke LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Tutup SplashActivity agar tidak bisa kembali dengan tombol back
        finish()
    }
}
package com.example.absensigeo

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

/**
 * Activity untuk menampilkan gambar dalam mode layar penuh.
 *
 * Fitur utama:
 * - Menampilkan gambar dari URL dengan library Glide
 * - Mendukung placeholder saat gambar sedang dimuat
 * - Menampilkan gambar error jika gagal dimuat
 * - Dapat ditutup dengan menekan gambar
 */
class FullscreenImageActivity : AppCompatActivity() {

    /**
     * Dipanggil saat activity dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan, null jika pertama kali dibuat
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mengatur tampilan edge-to-edge (fullscreen)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fullscreen_image)

        // Inisialisasi komponen UI
        val imageView = findViewById<ImageView>(R.id.fullscreenImageView)

        // Ambil URL gambar dari intent
        val imageUrl = intent.getStringExtra("imageUrl")

        // Load gambar menggunakan Glide
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_image) // Gambar placeholder saat loading
            .error(R.drawable.error_image) // Gambar error jika load gagal
            .into(imageView)

        // Set listener untuk menutup activity saat gambar diklik
        imageView.setOnClickListener {
            finish() // Tutup activity dan kembali ke sebelumnya
        }
    }
}
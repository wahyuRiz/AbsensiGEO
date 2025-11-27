package com.example.absensigeo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Activity untuk tampilan awal (Get Started) aplikasi.
 *
 * Fungsi utama:
 * - Menampilkan tampilan pembuka aplikasi
 * - Navigasi ke halaman login ketika tombol ditekan
 * - Mengimplementasikan edge-to-edge display
 */
class GetStartedActivity : AppCompatActivity() {

    // Deklarasi komponen UI
    private lateinit var mulai: Button

    /**
     * Dipanggil ketika activity pertama kali dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan, null jika pertama kali dibuat
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mengaktifkan tampilan edge-to-edge
        enableEdgeToEdge()

        // Mengatur layout dari XML
        setContentView(R.layout.activity_get_started)

        // Mengatur padding untuk menghindari tumpang tindih dengan system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi tombol mulai
        mulai = findViewById(R.id.mulaibutton)

        // Menambahkan listener untuk tombol mulai
        mulai.setOnClickListener {
            // Membuat intent untuk pindah ke LoginActivity
            val loginIntent = Intent(this, LoginActivity::class.java)

            // Memulai activity login
            startActivity(loginIntent)

            // Menutup activity saat ini agar tidak bisa kembali dengan tombol back
            finish()
        }
    }
}
package com.example.absensigeo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.OnBackPressedCallback
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity untuk menampilkan dan mengelola profil pengguna.
 *
 * Fitur utama:
 * - Menampilkan informasi profil pengguna (nama, NIP, role)
 * - Menampilkan foto profil
 * - Logout dari aplikasi
 * - Navigasi ke riwayat kehadiran dan edit profil
 */
class ProfileActivity : AppCompatActivity() {

    // Komponen UI
    private lateinit var btnLogout: Button
    private lateinit var linearbtn: LinearLayout
    private lateinit var tvNama: TextView
    private lateinit var tvNip: TextView
    private lateinit var tvRole: TextView
    private lateinit var profileImageView: ImageView

    /**
     * Dipanggil saat activity pertama kali dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Inisialisasi komponen UI
        initViews()

        // Ambil dan tampilkan data user
        fetchUserInfo()

        // Setup back button behavior
        setupBackButton()

        // Setup listener untuk tombol dan card
        setupClickListeners()
    }

    /**
     * Inisialisasi semua komponen view.
     */
    private fun initViews() {
        profileImageView = findViewById(R.id.profileImageView)
        tvNama = findViewById(R.id.tv_nama)
        tvNip = findViewById(R.id.tv_nip)
        tvRole = findViewById(R.id.tv_role)
        btnLogout = findViewById(R.id.logoutbutton)
        linearbtn = findViewById(R.id.linear01)
    }

    /**
     * Mengambil data user dari Firestore dan menampilkannya.
     */
    private fun fetchUserInfo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        currentUser?.email?.let { email ->
            db.collection("user")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val doc = result.documents[0]
                        // Set data user ke TextView
                        tvNama.text = doc.getString("name") ?: "N/A"
                        tvNip.text = doc.getString("nip") ?: "N/A"
                        tvRole.text = doc.getString("role")?.uppercase() ?: "N/A"

                        // Load foto profil dari Base64
                        loadProfileImage(doc.getString("photo"))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Gagal mengambil data user", e)
                }
        }
    }

    /**
     * Memuat gambar profil dari string Base64.
     *
     * @param photoBase64 String Base64 dari gambar profil
     */
    private fun loadProfileImage(photoBase64: String?) {
        if (!photoBase64.isNullOrEmpty()) {
            try {
                val decodedBytes = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                // Buat gambar circular dan tampilkan dengan Glide
                Glide.with(this)
                    .asBitmap()
                    .load(bitmap)
                    .circleCrop()
                    .into(profileImageView)

            } catch (e: Exception) {
                Log.e("ProfileActivity", "Format foto tidak valid", e)
            }
        }
    }

    /**
     * Menyiapkan behavior untuk tombol back.
     */
    private fun setupBackButton() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(RESULT_OK) // Kirim RESULT_OK ke MainActivity
                finish()             // Tutup Activity
            }
        })
    }

    /**
     * Menyiapkan listener untuk semua tombol dan card.
     */
    private fun setupClickListeners() {
        // Tombol logout
        btnLogout.setOnClickListener {
            logoutUser()
        }

        // Tombol back (custom)
        linearbtn.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        // Card riwayat kehadiran
        findViewById<androidx.cardview.widget.CardView>(R.id.card_riwayat).setOnClickListener {
            startActivity(Intent(this, RiwayatKehadiranActivity::class.java))
        }

        // Card edit identitas
        findViewById<androidx.cardview.widget.CardView>(R.id.card_identitas).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
    }

    /**
     * Proses logout user:
     * - Hapus data login dari SharedPreferences
     * - Logout dari Firebase Auth
     * - Redirect ke LoginActivity dan clear stack
     */
    private fun logoutUser() {
        // Hapus data login
        getSharedPreferences("loginPrefs", MODE_PRIVATE).edit().clear().apply()

        // Logout dari Firebase
        FirebaseAuth.getInstance().signOut()

        // Redirect ke LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
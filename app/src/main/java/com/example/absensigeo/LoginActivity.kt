package com.example.absensigeo

import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.absensigeo.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity untuk proses login pengguna.
 *
 * Fitur utama:
 * - Autentikasi menggunakan Firebase Auth
 * - Pengecekan role dari Firestore
 * - Penyimpanan preferensi login (remember me)
 * - Auto-logout setelah periode inaktif
 * - Manajemen session pengguna
 */
class LoginActivity : AppCompatActivity() {

    // View Binding
    private lateinit var binding: ActivityLoginBinding

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // SharedPreferences keys
    private val PREFS_NAME = "loginPrefs"
    private val PREF_REMEMBER = "rememberMe"

    /**
     * Dipanggil saat activity pertama kali dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Cek status remember me dan session
        checkRememberedLogin()

        // Setup listener untuk tombol login
        setupLoginButton()

    }

    /**
     * Memeriksa status login sebelumnya (remember me).
     */
    private fun checkRememberedLogin() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isRemembered = prefs.getBoolean(PREF_REMEMBER, false)

        // Cek waktu inaktif terakhir
        val lastActiveTime = prefs.getLong("lastActiveTime", 0)
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - lastActiveTime

        // Auto-logout jika inaktif lebih dari 2 menit
        val maxInactiveTime = 2 * 60 * 1000
        if (timeDifference > maxInactiveTime) {
            prefs.edit().clear().apply()
            auth.signOut()
            Log.d("LOGIN", "Auto-logout karena inaktif terlalu lama")
        }

        // Redirect ke MainActivity jika sudah login dan remember me aktif
        if (isRemembered && auth.currentUser != null) {
            Log.d("LOGIN", "User sudah login. Redirect ke MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    /**
     * Menyiapkan listener untuk tombol login.
     */
    private fun setupLoginButton() {
        binding.loginButton.setOnClickListener {
            val nip = binding.nipEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            Log.d("LOGIN", "Mencoba login dengan NIP: $nip")

            // Validasi input
            if (nip.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Tolong isi NIP/NUPTK dan Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)

            // Cari user di Firestore berdasarkan NIP
            findUserByNip(nip, password)
        }
    }

    /**
     * Mencari data user di Firestore berdasarkan NIP.
     *
     * @param nip NIP/NUPTK yang dimasukkan
     * @param password Password yang dimasukkan
     */
    private fun findUserByNip(nip: String, password: String) {
        FirebaseFirestore.getInstance().collection("user")
            .whereEqualTo("nip", nip)
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("LOGIN", "Query Firestore berhasil")

                if (!querySnapshot.isEmpty) {
                    val userDoc = querySnapshot.documents[0]
                    val email = userDoc.getString("email")
                    val role = userDoc.getString("role")

                    Log.d("LOGIN", "User ditemukan: $email dengan role: $role")

                    // Simpan preferensi login
                    saveLoginPreferences(role)

                    if (email.isNullOrEmpty()) {
                        Toast.makeText(this, "Email tidak ditemukan di Database", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Lakukan autentikasi dengan Firebase Auth
                    authenticateWithFirebase(email, password, userDoc)
                } else {
                    Log.w("LOGIN", "NIP tidak ditemukan di Firestore")
                    Toast.makeText(this, "NIP/NUPTK Tidak Ditemukan", Toast.LENGTH_SHORT).show()
                }
                showLoading(false)
            }
            .addOnFailureListener {
                Log.e("LOGIN", "Error Firestore: ${it.message}")
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }

    /**
     * Menyimpan preferensi login ke SharedPreferences.
     *
     * @param role Role pengguna yang akan disimpan
     */
    private fun saveLoginPreferences(role: String?) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(PREF_REMEMBER, true)
            .putString("userRole", role)
            .apply()
    }

    /**
     * Melakukan autentikasi dengan Firebase Auth.
     *
     * @param email Email pengguna
     * @param password Password pengguna
     * @param userDoc Dokumen user dari Firestore
     */
    private fun authenticateWithFirebase(email: String, password: String, userDoc: DocumentSnapshot) {
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d("LOGIN", "Autentikasi Firebase berhasil")
                handleSuccessfulLogin(userDoc)
            }
            .addOnFailureListener {
                Log.e("LOGIN", "Autentikasi Firebase gagal: ${it.message}")
                Toast.makeText(this, "Password Salah", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }

    /**
     * Menangani proses setelah login berhasil.
     *
     * @param userDoc Dokumen user dari Firestore
     */
    private fun handleSuccessfulLogin(userDoc: DocumentSnapshot) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val name = userDoc.getString("name") ?: "Tanpa Nama"
        val userId = currentUser?.uid ?: ""
        val role = userDoc.getString("role")

        // Simpan data user ke SharedPreferences
        saveUserData(name, userDoc.getString("nip") ?: "", userId)

        Log.d("LOGIN", "Menyimpan nama ke sharedPrefs: $name")

        // Update profil Firebase Auth
        updateFirebaseProfile(currentUser, name)

        showLoading(false)

        // Redirect berdasarkan role
        redirectBasedOnRole(role)
    }

    /**
     * Menyimpan data user ke SharedPreferences.
     *
     * @param name Nama pengguna
     * @param nip NIP pengguna
     * @param userId ID pengguna dari Firebase Auth
     */
    private fun saveUserData(name: String, nip: String, userId: String) {
        getSharedPreferences("user", Context.MODE_PRIVATE).edit()
            .putString("name", name)
            .putString("nip", nip)
            .putString("userId", userId)
            .apply()
    }

    /**
     * Mengupdate profil Firebase Auth dengan nama pengguna.
     *
     * @param currentUser User yang sedang login
     * @param name Nama yang akan diupdate
     */
    private fun updateFirebaseProfile(currentUser: FirebaseUser?, name: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()

        currentUser?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LOGIN", "Nama tampilan diupdate ke: $name")
                } else {
                    Log.w("LOGIN", "Gagal mengupdate nama tampilan")
                }
            }
    }

    /**
     * Mengarahkan pengguna berdasarkan role.
     *
     * @param role Role pengguna (guru/staf/kepala)
     */
    private fun redirectBasedOnRole(role: String?) {
        when (role) {
            "guru", "admin", "staf", "kepala" -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("ROLE", role)
                startActivity(intent)
                finish()
            }
            else -> {
                Log.e("LOGIN", "Role tidak dikenali: $role")
                Toast.makeText(this, "Role Tidak Ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Menampilkan/menyembunyikan loading indicator.
     *
     * @param isLoading True untuk menampilkan loading, false untuk menyembunyikan
     */
    private fun showLoading(isLoading: Boolean) {
        binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
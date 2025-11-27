package com.example.absensigeo

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Activity untuk pendaftaran pengguna baru oleh admin.
 *
 * Fitur utama:
 * - Pendaftaran akun baru dengan berbagai role (guru, staf, kepala)
 * - Upload foto profil dengan kompresi otomatis
 * - Validasi input dan ukuran gambar
 * - Penyimpanan data ke Firebase Auth dan Firestore
 */
class RegisterActivity : AppCompatActivity() {

    // Komponen UI
    private lateinit var nameEditText: EditText
    private lateinit var nipEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var roleSpinner: Spinner
    private lateinit var registerButton: Button
    private lateinit var profileImageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var imageSizeTextView: TextView
    private lateinit var linearbtn: LinearLayout

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Data gambar
    private var selectedImageBase64: String? = null
    private var imageBase64SizeKB: Int = 0

    /**
     * Dipanggil saat activity pertama kali dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inisialisasi komponen UI
        initViews()

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Setup spinner role
        setupRoleSpinner()

        // Setup listener tombol
        setupButtonListeners()

        linearbtn = findViewById(R.id.linear01)
        // Tombol back (custom)
        linearbtn.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    /**
     * Inisialisasi semua komponen view.
     */
    private fun initViews() {
        nameEditText = findViewById(R.id.nameEditText)
        nipEditText = findViewById(R.id.nipEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        roleSpinner = findViewById(R.id.roleSpinner)
        registerButton = findViewById(R.id.registerButton)
        profileImageView = findViewById(R.id.profileImageView)
        selectImageButton = findViewById(R.id.selectImageButton)
        imageSizeTextView = findViewById(R.id.imageSizeTextView)
    }

    /**
     * Menyiapkan spinner untuk memilih role.
     */
    private fun setupRoleSpinner() {
        val roles = listOf("guru", "staf", "kepala")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        roleSpinner.adapter = adapter
    }

    /**
     * Menyiapkan listener untuk tombol.
     */
    private fun setupButtonListeners() {
        // Tombol pilih gambar
        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        // Tombol register
        registerButton.setOnClickListener {
            registerNewUser()
        }
    }

    /**
     * Membuka image picker untuk memilih foto profil.
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 1001)
    }

    /**
     * Proses pendaftaran pengguna baru.
     */
    private fun registerNewUser() {
        val name = nameEditText.text.toString().trim()
        val nip = nipEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val role = roleSpinner.selectedItem.toString()

        // Validasi input
        if (name.isEmpty() || nip.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Tolong isi semua data", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi ukuran gambar
        if (imageBase64SizeKB > 800) {
            Toast.makeText(this, "Gambar terlalu besar, pilih ukuran yang lebih kecil", Toast.LENGTH_LONG).show()
            return
        }

        // Buat akun di Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // Siapkan data user untuk Firestore
                val userMap = mutableMapOf(
                    "uid" to uid,
                    "name" to name,
                    "nip" to nip,
                    "email" to email,
                    "role" to role
                )

                // Tambahkan foto profil jika ada
                selectedImageBase64?.let { userMap["photo"] = it }

                // Simpan data user ke Firestore
                saveUserData(uid, userMap)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Pendaftaran gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Menyimpan data user ke Firestore.
     *
     * @param uid User ID dari Firebase Auth
     * @param userMap Data user dalam bentuk Map
     */
    private fun saveUserData(uid: String, userMap: MutableMap<String, String>) {
        db.collection("user").document(uid).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Pendaftaran user berhasil", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menyimpan user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Menangani hasil dari image picker.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data

            try {
                // Decode gambar yang dipilih
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                profileImageView.setImageBitmap(bitmap)

                // Kompresi gambar ke format JPEG (50% quality)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                val byteArray = stream.toByteArray()

                // Konversi ke Base64
                selectedImageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)

                // Hitung dan tampilkan ukuran gambar
                imageBase64SizeKB = selectedImageBase64?.length?.div(1024) ?: 0
                imageSizeTextView.text = "Ukuran gambar: ${imageBase64SizeKB} KB"

            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
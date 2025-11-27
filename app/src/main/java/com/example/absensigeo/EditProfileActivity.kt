package com.example.absensigeo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

/**
 * Activity untuk mengedit profil pengguna.
 *
 * Fitur utama:
 * - Mengubah data profil (nama, NIP, password)
 * - Mengubah role (hanya untuk kepala)
 * - Mengupload foto profil
 * - Validasi input dan ukuran gambar
 */
class EditProfileActivity : AppCompatActivity() {

    // Komponen UI
    private lateinit var nameEditText: EditText
    private lateinit var nipEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var roleSpinner: Spinner
    private lateinit var saveButton: Button
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
    private lateinit var uid: String
    private var currentUserRole: String? = null

    /**
     * Dipanggil saat activity dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Inisialisasi komponen UI
        initViews()

        // Setup spinner role
        setupRoleSpinner()

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        uid = auth.currentUser?.uid ?: return

        // Load data pengguna
        loadUserData()

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
     * Inisialisasi semua komponen view
     */
    private fun initViews() {
        nameEditText = findViewById(R.id.nameEditText)
        nipEditText = findViewById(R.id.nipEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        roleSpinner = findViewById(R.id.roleSpinner)
        saveButton = findViewById(R.id.saveButton)
        profileImageView = findViewById(R.id.profileImageView)
        selectImageButton = findViewById(R.id.selectImageButton)
        imageSizeTextView = findViewById(R.id.imageSizeTextView)
    }

    /**
     * Menyiapkan spinner untuk memilih role
     */
    private fun setupRoleSpinner() {
        val roles = listOf("guru", "staf", "kepala", "admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        roleSpinner.adapter = adapter
    }

    /**
     * Memuat data pengguna dari Firestore
     */
    private fun loadUserData() {
        db.collection("user").document(uid).get().addOnSuccessListener { doc ->
            currentUserRole = doc.getString("role")
            nameEditText.setText(doc.getString("name"))
            nipEditText.setText(doc.getString("nip"))

            // Set role di spinner
            val roles = listOf("guru", "staf", "kepala", "admin")
            val userRole = doc.getString("role")
            userRole?.let {
                val index = roles.indexOf(it)
                if (index >= 0) {
                    roleSpinner.setSelection(index)
                }
            }

            // Hanya kepala yang bisa mengubah role
            if (currentUserRole != "kepala") {
                roleSpinner.isEnabled = false
            }

            // Load foto profil jika ada
            selectedImageBase64 = doc.getString("photo")
            selectedImageBase64?.let {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                profileImageView.setImageBitmap(bmp)
                imageBase64SizeKB = it.length / 1024
                imageSizeTextView.text = "Ukuran gambar: ${imageBase64SizeKB} KB"
            }
        }
    }

    /**
     * Menyiapkan listener untuk tombol
     */
    private fun setupButtonListeners() {
        // Tombol pilih gambar
        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1001)
        }

        // Tombol simpan
        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val nip = nipEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validasi input
            if (name.isEmpty() || nip.isEmpty()) {
                Toast.makeText(this, "Nama dan NIP tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validasi ukuran gambar
            if (imageBase64SizeKB > 800) {
                Toast.makeText(this, "Gambar terlalu besar (>800KB)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Persiapkan data update
            val updates = mutableMapOf<String, Any>(
                "name" to name,
                "nip" to nip
            )

            // Jika kepala, update role juga
            if (currentUserRole == "kepala") {
                val role = roleSpinner.selectedItem.toString()
                updates["role"] = role
            }

            // Jika ada gambar baru, tambahkan ke update
            selectedImageBase64?.let { updates["photo"] = it }

            // Update data ke Firestore
            db.collection("user").document(uid).update(updates)
                .addOnSuccessListener {
                    // Jika password diubah, update password juga
                    if (password.isNotEmpty()) {
                        auth.currentUser?.updatePassword(password)
                    }
                    Toast.makeText(this, "Profil berhasil diupdate", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal update: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Menangani hasil dari pemilihan gambar
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            // Dapatkan URI gambar yang dipilih
            val imageUri: Uri? = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)

            // Tampilkan gambar di ImageView
            profileImageView.setImageBitmap(bitmap)

            // Kompres gambar dan konversi ke Base64
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
            val byteArray = stream.toByteArray()
            selectedImageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)

            // Hitung dan tampilkan ukuran gambar
            imageBase64SizeKB = selectedImageBase64?.length?.div(1024) ?: 0
            imageSizeTextView.text = "Ukuran gambar: ${imageBase64SizeKB} KB"
        }
    }
}
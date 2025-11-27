package com.example.absensigeo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.data.BuktiMengajar
import com.example.absensigeo.data.HistoriAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.os.CountDownTimer

/**
 * Activity untuk mengunggah bukti mengajar dan melihat histori upload.
 *
 * Fitur utama:
 * - Memilih file dari penyimpanan perangkat
 * - Mengupload file ke GoFile.io
 * - Menyimpan URL file ke Firebase Realtime Database
 * - Menampilkan histori 7 hari terakhir
 * - Validasi waktu upload (maksimal jam 17:00)
 */
class UploadBuktiActivity : AppCompatActivity() {

    // Konstanta untuk request code
    private val pickFileRequest = 101

    // Variabel untuk menyimpan URI file yang dipilih
    private var fileUri: Uri? = null

    // ID pengguna yang sedang login
    private lateinit var userId: String

    // Format tanggal untuk penyimpanan
    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val tanggalHariIni = formatter.format(Date())

    // Komponen RecyclerView untuk histori
    private lateinit var recyclerView: RecyclerView
    private lateinit var historiAdapter: HistoriAdapter
    private val listHistori = mutableListOf<BuktiMengajar>()

    private lateinit var materiEditText: EditText
    private lateinit var deskripsiEditText: EditText
    private lateinit var linearbtn: LinearLayout

    private lateinit var countdownTextView: TextView
    private var countdownTimer: CountDownTimer? = null

    /**
     * Dipanggil saat activity pertama kali dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_bukti)

        countdownTextView = findViewById(R.id.tvCountdown)
        startCountdownToUploadDeadline()

        materiEditText = findViewById(R.id.etMateri)
        deskripsiEditText = findViewById(R.id.etDeskripsi)
        linearbtn = findViewById(R.id.linear01)

        // Inisialisasi user ID
        initUserId()

        cekRoleDanBatasiUpload()

        // Setup RecyclerView untuk histori
        setupRecyclerView()

        // Load data histori
        loadHistoriUpload()

        // Listener untuk tombol pilih file
        setupPickFileButton()

        // Listener untuk tombol upload
        setupUploadButton()

        // Tombol back (custom)
        linearbtn.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    /**
     * Menginisialisasi ID pengguna dari Firebase Auth.
     */
    private fun initUserId() {
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Log.d("UPLOAD_BUKTI", "User ID: $userId")
    }

    private fun cekRoleDanBatasiUpload() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    if (role == "admin") {
                        findViewById<Button>(R.id.btnPickFile).isEnabled = false
                        findViewById<Button>(R.id.btnUpload).isEnabled = false
                        Toast.makeText(this, "Admin tidak diperbolehkan mengunggah bukti", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat role user", Toast.LENGTH_SHORT).show()
                Log.e("UploadBukti", "Error ambil role: ${it.message}")
            }
    }

    /**
     * Menyiapkan RecyclerView untuk menampilkan histori.
     */
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerHistori)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    /**
     * Menyiapkan listener untuk tombol pilih file.
     */
    private fun setupPickFileButton() {
        findViewById<Button>(R.id.btnPickFile).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*" // Menerima semua jenis file
            startActivityForResult(intent, pickFileRequest)
        }
    }

    /**
     * Menyiapkan listener untuk tombol upload.
     */
    private fun setupUploadButton() {
        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            val materi = materiEditText.text.toString().trim()
            val deskripsi = deskripsiEditText.text.toString().trim()
            Log.d("UploadBukti", "Tombol upload diklik")

            if (materi.isEmpty() || deskripsi.isEmpty()) {
                Toast.makeText(this, "Materi dan Deskripsi wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validasi waktu upload
            if (!isBefore16()) {
                Toast.makeText(this, "Waktu upload sudah lewat (maks jam 16:00)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Validasi file yang dipilih
            if (fileUri != null) {
                processFileUpload()
            } else {
                Toast.makeText(this, "Silakan pilih file terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }
    }


    /**
     * Memproses upload file yang dipilih.
     */
    private fun processFileUpload() {
        Log.d("UploadBukti", "File URI: $fileUri")
        Toast.makeText(this, "Mengupload...", Toast.LENGTH_SHORT).show()

        uploadFileToGoFile(this, fileUri!!,
            onSuccess = { url ->
                handleUploadSuccess(url)
            },
            onError = { error ->
                handleUploadError(error)
            }
        )
    }

    /**
     * Menangani hasil upload yang sukses.
     *
     * @param url URL file yang berhasil diupload
     */
    private fun handleUploadSuccess(url: String) {
        val materi = materiEditText.text.toString().trim()
        val deskripsi = deskripsiEditText.text.toString().trim()
        Log.d("UPLOAD_BUKTI", "URL berhasil: $url")
        Toast.makeText(this, "Upload sukses", Toast.LENGTH_SHORT).show()
        Log.d("UPLOAD", "Menyimpan ke Firebase: url=$url")

        // Siapkan data untuk disimpan ke Firebase
        val data = mapOf(
            "nama_file" to getFileNameFromUri(fileUri!!),
            "url" to url,
            "materi" to materi,
            "deskripsi" to deskripsi
        )

        // Referensi ke database Firebase
        val dbRef = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("bukti_mengajar/$userId/$tanggalHariIni")

        // Simpan data ke Firebase
        dbRef.setValue(data)
            .addOnSuccessListener {
                Log.d("UploadBukti", "URL berhasil diambil: $url")
                Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                loadHistoriUpload() // Refresh tampilan histori
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal simpan ke database: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Menangani error saat upload.
     *
     * @param error Pesan error yang diterima
     */
    private fun handleUploadError(error: String) {
        Log.e("UploadBukti", "Gagal upload: $error")
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }

    /**
     * Mengambil nama file dari URI.
     *
     * @param uri URI file yang dipilih
     * @return Nama file atau "file" jika tidak ditemukan
     */
    private fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow("_display_name"))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "file"
    }

    /**
     * Memuat histori upload 7 hari terakhir dari Firebase.
     */
    private fun loadHistoriUpload() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("bukti_mengajar/$userId")

        // Siapkan daftar 7 hari terakhir
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val last7Days = mutableListOf<String>()
        for (i in 0..6) {
            last7Days.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Ambil data dari Firebase
        dbRef.get().addOnSuccessListener { snapshot ->
            listHistori.clear()
            for (tanggal in last7Days) {
                val node = snapshot.child(tanggal)
                if (node.exists()) {
                    val materi = node.child("materi").value?.toString() ?: ""
                    val deskripsi = node.child("deskripsi").value?.toString() ?: ""
                    val namaFile = node.child("nama_file").value?.toString() ?: ""
                    val url = node.child("url").value?.toString() ?: ""
                    listHistori.add(BuktiMengajar(tanggal, namaFile, url, materi, deskripsi))
                }
            }
            // Update RecyclerView
            historiAdapter = HistoriAdapter(listHistori)
            recyclerView.adapter = historiAdapter
        }
    }

    /**
     * Mengupload file ke GoFile.io.
     *
     * @param context Konteks aplikasi
     * @param uri URI file yang akan diupload
     * @param onSuccess Callback ketika upload sukses
     * @param onError Callback ketika upload gagal
     */
    fun uploadFileToGoFile(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val contentResolver = context.contentResolver
        val fileName = uri.lastPathSegment ?: "bukti"
        val inputStream = contentResolver.openInputStream(uri)
        val fileBytes = inputStream?.readBytes()

        if (fileBytes == null) {
            onError("Gagal membaca file")
            return
        }

        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", fileName,
                fileBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://store1.gofile.io/uploadFile")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    onError("Gagal upload: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "")
                    val status = json.getString("status")
                    if (status == "ok") {
                        val data = json.getJSONObject("data")
                        val downloadUrl = data.getString("downloadPage")
                        Handler(Looper.getMainLooper()).post {
                            onSuccess(downloadUrl)
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            onError("Upload gagal: $status")
                        }
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post {
                        onError("Gagal parsing response")
                    }
                }
            }
        })
    }

    private fun startCountdownToUploadDeadline() {
        val now = Calendar.getInstance()
        val batas = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val millisUntilDeadline = batas.timeInMillis - now.timeInMillis

        if (millisUntilDeadline <= 0) {
            countdownTextView.text = "Waktu upload telah berakhir"
            return
        }

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(millisUntilDeadline, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / (1000 * 60 * 60)
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                countdownTextView.text = String.format("Sisa waktu upload: %02d:%02d:%02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                countdownTextView.text = "Waktu upload telah berakhir"
            }
        }.start()
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        super.onDestroy()
    }


    /**
     * Memeriksa apakah waktu saat ini sebelum jam 16:00.
     *
     * @return true jika sebelum jam 16:00, false jika sudah lewat
     */
    private fun isBefore16(): Boolean {
        val now = Calendar.getInstance()
        val batas = Calendar.getInstance()
        batas.set(Calendar.HOUR_OF_DAY, 16)
        batas.set(Calendar.MINUTE, 0)
        batas.set(Calendar.SECOND, 0)
        batas.set(Calendar.MILLISECOND, 0)
        return now.before(batas)
    }

    /**
     * Menangani hasil dari pemilihan file.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickFileRequest && resultCode == RESULT_OK) {
            fileUri = data?.data
            Toast.makeText(this, "File dipilih: ${fileUri?.lastPathSegment}", Toast.LENGTH_SHORT).show()
        }
    }
}
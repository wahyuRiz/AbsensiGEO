package com.example.absensigeo.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.R
import com.example.absensigeo.data.TemplateAdapter
import com.example.absensigeo.data.TemplateSuratModel
import com.google.firebase.database.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Fragment untuk manajemen template surat.
 *
 * Fitur utama:
 * - Menampilkan daftar template surat dari Firebase
 * - Upload template baru (hanya untuk admin)
 * - Download template melalui URL
 * - Sortir template berdasarkan timestamp terbaru
 */
class TemplateSuratFragment : Fragment() {

    // Komponen UI
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TemplateAdapter
    private lateinit var btnAdd: ImageView
    private var tvFileDipilih: TextView? = null

    // Variabel untuk file
    private var selectedFileUri: Uri? = null
    private val FILE_PICK_CODE = 1234

    // Firebase
    private lateinit var dbRef: DatabaseReference

    // Data template
    private val listTemplate = mutableListOf<TemplateSuratModel>()

    /**
     * Membuat tampilan fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_template_surat, container, false)
    }

    /**
     * Dipanggil setelah tampilan dibuat.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("TemplateFragment", "Fragment dimuat")

        // Inisialisasi komponen UI
        initViews(view)

        // Setup RecyclerView
        setupRecyclerView()

        // Cek role dan tampilkan tombol add sesuai role
        checkUserRole()

        // Load data dari Firebase
        loadTemplateFromFirebase()
    }

    /**
     * Inisialisasi komponen view.
     */
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rvTemplateSurat)
        btnAdd = view.findViewById(R.id.fabAddTemplate)
    }

    /**
     * Menyiapkan RecyclerView dan adapter.
     */
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TemplateAdapter(listTemplate)
        recyclerView.adapter = adapter
    }

    /**
     * Memeriksa role user dan menyesuaikan tampilan.
     */
    private fun checkUserRole() {
        val sharedPref = requireActivity().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val role = sharedPref.getString("userRole", "guru")
        Log.d("TemplateFragment", "Role login saat ini: $role")

        // Sembunyikan tombol jika role adalah "guru" atau "kepala"
        btnAdd.visibility = if (role == "guru" || role == "kepala") View.GONE else View.VISIBLE

        btnAdd.setOnClickListener { showUploadDialog() }
    }


    /**
     * Memuat data template dari Firebase Realtime Database.
     */
    private fun loadTemplateFromFirebase() {
        dbRef = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("TemplateSurat")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseData", "Jumlah data: ${snapshot.childrenCount}")
                listTemplate.clear()

                // Loop melalui semua data template
                for (data in snapshot.children) {
                    val template = data.getValue(TemplateSuratModel::class.java)
                    template?.let { listTemplate.add(it) }
                }

                // Urutkan berdasarkan timestamp terbaru
                listTemplate.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal ambil data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Menampilkan dialog untuk upload template baru.
     */
    private fun showUploadDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_upload_template, null)
        val etJudul = dialogView.findViewById<EditText>(R.id.etJudulTemplate)
        val btnPilih = dialogView.findViewById<Button>(R.id.btnPilihFile)
        tvFileDipilih = dialogView.findViewById(R.id.tvFileDipilih)

        // Buat dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Upload Template Surat")
            .setView(dialogView)
            .setPositiveButton("Upload", null)
            .setNegativeButton("Batal", null)
            .create()

        // Listener untuk tombol pilih file
        btnPilih.setOnClickListener {
            openFilePicker()
        }

        // Custom listener untuk tombol upload
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val judul = etJudul.text.toString().trim()
                if (validateInput(judul)) {
                    uploadToGofileAndSave(judul, selectedFileUri!!)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    /**
     * Membuka file picker untuk memilih file.
     */
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // Menerima semua jenis file
        startActivityForResult(intent, FILE_PICK_CODE)
    }

    /**
     * Validasi input sebelum upload.
     */
    private fun validateInput(judul: String): Boolean {
        return when {
            judul.isEmpty() -> {
                Toast.makeText(requireContext(), "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
                false
            }
            selectedFileUri == null -> {
                Toast.makeText(requireContext(), "Silakan pilih file terlebih dahulu", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    /**
     * Menangani hasil dari file picker.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data
            Toast.makeText(requireContext(), "File dipilih", Toast.LENGTH_SHORT).show()

            // Tampilkan nama file yang dipilih
            val fileName = getFileNameFromUri(requireContext(), selectedFileUri!!)
            tvFileDipilih?.text = fileName ?: "Nama file tidak ditemukan"
        }
    }

    /**
     * Mengupload file ke GoFile.io dan menyimpan ke Firebase.
     */
    private fun uploadToGofileAndSave(judul: String, fileUri: Uri) {
        Toast.makeText(requireContext(), "Mengunggah...", Toast.LENGTH_SHORT).show()

        val fileName = getFileNameFromUri(requireContext(), fileUri)
        if (fileName == null) {
            Toast.makeText(requireContext(), "Gagal mendapatkan nama file", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Baca file
            val inputStream = requireContext().contentResolver.openInputStream(fileUri)
            val fileBytes = inputStream?.readBytes()
            inputStream?.close()

            if (fileBytes == null) {
                Toast.makeText(requireContext(), "File tidak bisa dibaca", Toast.LENGTH_SHORT).show()
                return
            }

            // Buat request upload
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
                .build()

            val client = OkHttpClient()
            val uploadRequest = Request.Builder()
                .url("https://store1.gofile.io/uploadFile")
                .post(requestBody)
                .build()

            // Eksekusi request
            client.newCall(uploadRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showToast("Gagal upload file")
                }

                override fun onResponse(call: Call, response: Response) {
                    handleUploadResponse(response, judul)
                }
            })

        } catch (e: Exception) {
            showToast("Error: ${e.message}")
        }
    }

    /**
     * Menangani response dari GoFile.io.
     */
    private fun handleUploadResponse(response: Response, judul: String) {
        val responseBody = response.body?.string()
        try {
            val json = JSONObject(responseBody ?: "")
            if (json.getString("status") == "ok") {
                val url = json.getJSONObject("data").getString("downloadPage")
                simpanKeFirebase(judul, url)
            } else {
                showToast("Upload gagal: ${json.getString("status")}")
            }
        } catch (e: Exception) {
            showToast("Gagal parsing respons upload")
        }
    }

    /**
     * Menyimpan data template ke Firebase.
     */
    private fun simpanKeFirebase(judul: String, url: String) {
        val ref = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("TemplateSurat")

        val id = ref.push().key!!
        val data = TemplateSuratModel(judul, url, System.currentTimeMillis())

        ref.child(id).setValue(data).addOnCompleteListener {
            showToast(if (it.isSuccessful) "Upload berhasil" else "Gagal upload")
        }
    }

    /**
     * Mendapatkan nama file dari URI.
     */
    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }

        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }

        return result
    }

    /**
     * Menampilkan toast di UI thread.
     */
    private fun showToast(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
}
package com.example.absensigeo.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.absensigeo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import android.provider.OpenableColumns
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

/**
 * Fragment untuk mengajukan izin atau cuti.
 * Menangani:
 * - Pemilihan jenis izin/cuti
 * - Upload file pendukung (opsional)
 * - Pengiriman data ke Firebase
 *
 * @property spinnerJenis Dropdown untuk memilih jenis izin/cuti
 * @property etKeterangan Input field untuk keterangan izin
 * @property btnKirim Tombol untuk mengirim permohonan
 * @property btnPilihFile Tombol untuk memilih file pendukung
 * @property tvNamaFile TextView untuk menampilkan nama file terpilih
 * @property fileUri URI file yang dipilih
 * @property uploadedFileUrl URL file yang telah diupload ke GoFile
 */
class IzinCutiFragment : Fragment() {

    // Komponen UI
    private lateinit var spinnerJenis: Spinner
    private lateinit var etKeterangan: EditText
    private lateinit var btnKirim: Button
    private lateinit var btnPilihFile: Button
    private lateinit var tvNamaFile: TextView

    // Variabel untuk manajemen file
    private var fileUri: Uri? = null
    private var uploadedFileUrl: String? = null

    // Konstanta
    private val FILE_PICK_CODE = 1001
    private val TAG = "IzinCutiFragment"

    /**
     * Dipanggil saat Fragment membuat tampilan UI-nya.
     * Menginisialisasi semua komponen UI dan menyiapkan listener.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_izin_cuti, container, false)

        // Inisialisasi komponen UI
        spinnerJenis = view.findViewById(R.id.spinnerJenis)
        etKeterangan = view.findViewById(R.id.etKeterangan)
        btnKirim = view.findViewById(R.id.btnKirim)
        btnPilihFile = view.findViewById(R.id.btnPilihFile)
        tvNamaFile = view.findViewById(R.id.tvNamaFile)

        // Setup spinner jenis izin
        val jenisIzin = arrayOf("Izin", "Cuti")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, jenisIzin)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerJenis.adapter = adapter

        // Listener untuk tombol pilih file
        btnPilihFile.setOnClickListener {
            pilihFile()
        }

        // Listener untuk tombol kirim
        btnKirim.setOnClickListener {
            if (fileUri != null) {
                uploadFileToGofile(fileUri!!)
            } else {
                kirimDataKeFirebase(null)
            }
        }

        return view
    }

    /**
     * Membuka file picker untuk memilih file pendukung.
     * File yang dipilih akan disimpan di [fileUri].
     */
    private fun pilihFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, FILE_PICK_CODE)
    }

    /**
     * Menangani hasil dari file picker.
     * Menyimpan URI file yang dipilih dan menampilkan nama file.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            fileUri = data?.data
            fileUri?.let {
                val fileName = it.lastPathSegment?.split("/")?.last() ?: "file_terpilih"
                tvNamaFile.text = "File: $fileName"
            }
        }
    }

    /**
     * Mengupload file ke GoFile.io.
     * Setelah upload berhasil, akan memanggil [kirimDataKeFirebase] dengan URL file.
     *
     * @param fileUri URI file yang akan diupload
     */
    private fun uploadFileToGofile(fileUri: Uri) {
        Toast.makeText(requireContext(), "Mengunggah file...", Toast.LENGTH_SHORT).show()

        // Dapatkan nama file dari URI
        val fileName = getFileNameFromUri(requireContext(), fileUri)
        if (fileName == null) {
            Toast.makeText(requireContext(), "Gagal mendapatkan nama file", Toast.LENGTH_SHORT).show()
            return
        }

        // Baca file ke dalam byte array
        val inputStream = requireContext().contentResolver.openInputStream(fileUri)
        val fileBytes = inputStream?.readBytes()
        inputStream?.close()

        if (fileBytes == null) {
            Toast.makeText(requireContext(), "File tidak bisa dibaca", Toast.LENGTH_SHORT).show()
            return
        }

        // Persiapkan request upload
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        // Eksekusi request upload
        val client = OkHttpClient()
        val uploadRequest = Request.Builder()
            .url("https://store1.gofile.io/uploadFile")
            .post(requestBody)
            .build()

        client.newCall(uploadRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Gagal mengunggah file", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val uploadBody = response.body?.string()
                Log.d(TAG, "Upload response: $uploadBody")

                try {
                    val jsonUpload = JSONObject(uploadBody ?: "")
                    val status = jsonUpload.getString("status")

                    if (status == "ok") {
                        val downloadPage = jsonUpload.getJSONObject("data").getString("downloadPage")
                        uploadedFileUrl = downloadPage

                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Upload berhasil", Toast.LENGTH_SHORT).show()
                            kirimDataKeFirebase(downloadPage)
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Upload gagal: $status", Toast.LENGTH_SHORT).show()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Upload gagal: respons tidak valid", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    /**
     * Mendapatkan nama file dari URI.
     *
     * @param context Context aplikasi
     * @param uri URI file
     * @return Nama file atau null jika gagal
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
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
     * Mengirim data izin/cuti ke Firebase Realtime Database.
     * Data yang dikirim termasuk jenis izin, keterangan, waktu, dan URL file (jika ada).
     *
     * @param fileUrl URL file pendukung (opsional)
     */
    private fun kirimDataKeFirebase(fileUrl: String?) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        // Persiapkan data yang akan dikirim
        val userId = user.uid
        val waktuSekarang = System.currentTimeMillis()
        val jenis = spinnerJenis.selectedItem.toString()
        val keterangan = etKeterangan.text.toString().trim()

        // Validasi input
        if (keterangan.isEmpty()) {
            Toast.makeText(requireContext(), "Keterangan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Buat payload data
        val izinData = mutableMapOf(
            "name" to user.displayName,
            "jenis" to jenis,
            "keterangan" to keterangan,
            "waktu" to waktuSekarang,
            "suratUrl" to uploadedFileUrl
        )

        if (fileUrl != null) izinData["url"] = fileUrl

        // Kirim ke Firebase
        val dbRef = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Absensi")
            .child(userId)

        val izinId = dbRef.push().key!!
        dbRef.child(izinId).setValue(izinData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Izin/Cuti berhasil dikirim", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Gagal mengirim izin", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
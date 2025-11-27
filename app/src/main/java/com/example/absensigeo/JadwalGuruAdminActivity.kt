package com.example.absensigeo

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.data.JadwalGuruAdapter
import com.example.absensigeo.data.JadwalGuru
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

/**
 * Activity untuk mengelola jadwal guru (admin).
 *
 * Fitur utama:
 * - Menampilkan daftar jadwal guru dalam RecyclerView
 * - Menambah, mengedit, dan menghapus jadwal
 * - Filter jadwal berdasarkan guru
 * - Input waktu dengan TimePickerDialog
 */
class JadwalGuruAdminActivity : AppCompatActivity() {

    // Komponen UI
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: JadwalGuruAdapter
    private lateinit var linearbtn: LinearLayout

    // Data
    private lateinit var listJadwal: MutableList<JadwalGuru>

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("jadwal")

    /**
     * Dipanggil saat activity pertama kali dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jadwal_guru_admin)

        // Inisialisasi RecyclerView
        setupRecyclerView()

        // Setup Floating Action Button untuk menambah jadwal
        findViewById<FloatingActionButton>(R.id.fab_add_jadwal).setOnClickListener {
            showFormDialog(null)
        }

        // Load data jadwal awal
        loadJadwal()

        linearbtn = findViewById(R.id.linear01)
        // Tombol back (custom)
        linearbtn.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    /**
     * Menyiapkan RecyclerView dan adapter untuk menampilkan data jadwal.
     */
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewJadwal)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inisialisasi list data dan adapter dengan callback untuk edit/hapus
        listJadwal = mutableListOf()
        adapter = JadwalGuruAdapter(listJadwal, this::editJadwal, this::deleteJadwal)
        recyclerView.adapter = adapter
    }

    /**
     * Memuat daftar nama guru dari Firestore.
     *
     * @param callback Fungsi yang dipanggil ketika data berhasil diambil
     */
    private fun loadNamaGuru(callback: (List<String>) -> Unit) {
        FirebaseFirestore.getInstance().collection("user")
            .whereEqualTo("role", "guru")
            .get()
            .addOnSuccessListener { result ->
                val namaGuruList = result.documents.mapNotNull { it.getString("name") }
                Log.d("loadNamaGuru", "Jumlah guru ditemukan: ${namaGuruList.size}")
                callback(namaGuruList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil nama guru", Toast.LENGTH_SHORT).show()
                Log.e("loadNamaGuru", "Gagal mengambil nama guru", it)
                callback(emptyList())
            }
    }

    /**
     * Memuat data jadwal dari Firestore.
     */
    private fun loadJadwal() {
        collection.get().addOnSuccessListener { result ->
            Log.d("loadJadwal", "Jumlah data: ${listJadwal.size}")
            listJadwal.clear()
            for (doc in result) {
                val jadwal = doc.toObject(JadwalGuru::class.java).apply {
                    id = doc.id // Simpan ID dokumen
                }
                listJadwal.add(jadwal)
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Menampilkan dialog form untuk menambah/mengedit jadwal.
     *
     * @param jadwal Objek JadwalGuru yang akan diedit (null untuk tambah baru)
     */
    private fun showFormDialog(jadwal: JadwalGuru? = null) {
        val isEdit = jadwal != null
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tambah_ubah_jadwal, null)

        // Binding view
        val etNama = dialogView.findViewById<AutoCompleteTextView>(R.id.et_nama)
        val etKelas = dialogView.findViewById<EditText>(R.id.et_kelas)
        val etHari = dialogView.findViewById<EditText>(R.id.et_hari)
        val etJamMulai = dialogView.findViewById<EditText>(R.id.et_jam_mulai)
        val etJamSelesai = dialogView.findViewById<EditText>(R.id.et_jam_selesai)
        val etMapel = dialogView.findViewById<EditText>(R.id.et_mapel)

        // Setup TimePicker untuk jam mulai dan selesai
        setupTimePicker(etJamMulai)
        setupTimePicker(etJamSelesai)

        // Jika mode edit, isi form dengan data yang ada
        if (isEdit) {
            jadwal?.let {
                etHari.setText(it.hari)
                etKelas.setText(it.kelas)
                etJamMulai.setText(it.jamMulai)
                etJamSelesai.setText(it.jamSelesai)
                etMapel.setText(it.mataPelajaran)
                Log.d("FormDialog", "Edit jadwal untuk: ${it.namaGuru}")
            }
        }

        // Load daftar guru untuk AutoCompleteTextView
        loadNamaGuru { namaGuruList ->
            Log.d("FormDialog", "Daftar guru berhasil diambil: $namaGuruList")
            val adapterNama = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, namaGuruList)
            etNama.setAdapter(adapterNama)

            // Tampilkan dropdown saat focus
            etNama.threshold = 0
            etNama.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) etNama.showDropDown()
            }

            // Jika edit, set nama guru
            if (isEdit) {
                etNama.setText(jadwal?.namaGuru, false)
            }

            // Tampilkan dialog
            showFormAlertDialog(dialogView, isEdit, etNama, etKelas, etHari, etJamMulai, etJamSelesai, etMapel, jadwal)
        }
    }

    /**
     * Menyiapkan TimePicker untuk input waktu.
     *
     * @param editText EditText yang akan menampilkan waktu
     */
    private fun setupTimePicker(editText: EditText) {
        editText.inputType = InputType.TYPE_NULL
        editText.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                editText.setText(String.format("%02d:%02d", hour, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }
    }

    /**
     * Menampilkan AlertDialog untuk form jadwal.
     */
    private fun showFormAlertDialog(
        dialogView: View,
        isEdit: Boolean,
        etNama: AutoCompleteTextView,
        etKelas: EditText,
        etHari: EditText,
        etJamMulai: EditText,
        etJamSelesai: EditText,
        etMapel: EditText,
        jadwal: JadwalGuru?
    ) {
        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit Jadwal" else "Tambah Jadwal")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Ubah" else "Simpan") { _, _ ->
                handleFormSubmit(
                    etNama.text.toString().trim(),
                    etKelas.text.toString().trim(),
                    etHari.text.toString().trim(),
                    etJamMulai.text.toString().trim(),
                    etJamSelesai.text.toString().trim(),
                    etMapel.text.toString().trim(),
                    isEdit,
                    jadwal
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Menangani submit form jadwal.
     */
    private fun handleFormSubmit(
        nama: String,
        kelas: String,
        hari: String,
        mulai: String,
        selesai: String,
        mapel: String,
        isEdit: Boolean,
        jadwal: JadwalGuru?
    ) {
        // Validasi input
        if (nama.isEmpty() || kelas.isEmpty() || hari.isEmpty() || mulai.isEmpty() || selesai.isEmpty() || mapel.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            Log.w("FormValidation", "Field kosong ditemukan.")
            return
        }

        val newJadwal = JadwalGuru(nama, kelas, hari, mulai, selesai, mapel)

        if (isEdit) {
            // Update jadwal yang ada
            newJadwal.id = jadwal!!.id
            collection.document(jadwal.id).set(newJadwal)
                .addOnSuccessListener {
                    Log.i("FirestoreUpdate", "Berhasil mengupdate jadwal dengan ID: ${jadwal.id}")
                    Toast.makeText(this, "Jadwal diubah", Toast.LENGTH_SHORT).show()
                    loadJadwal()
                }
                .addOnFailureListener {
                    Log.e("FirestoreUpdate", "Gagal mengubah jadwal: ${it.message}", it)
                    Toast.makeText(this, "Gagal mengubah: ${it.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            // Tambah jadwal baru
            collection.add(newJadwal)
                .addOnSuccessListener { documentRef ->
                    Log.i("FirestoreAdd", "Jadwal ditambahkan dengan ID: ${documentRef.id}")
                    Toast.makeText(this, "Jadwal ditambahkan", Toast.LENGTH_SHORT).show()
                    loadJadwal()
                }
                .addOnFailureListener {
                    Log.e("FirestoreAdd", "Gagal menambahkan jadwal: ${it.message}", it)
                    Toast.makeText(this, "Gagal menambahkan: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    /**
     * Dipanggil saat activity kembali ke foreground.
     */
    override fun onResume() {
        super.onResume()
        loadJadwal()
    }

    /**
     * Memulai proses edit jadwal.
     *
     * @param jadwal Objek JadwalGuru yang akan diedit
     */
    private fun editJadwal(jadwal: JadwalGuru) {
        showFormDialog(jadwal)
    }

    /**
     * Memulai proses hapus jadwal dengan konfirmasi.
     *
     * @param jadwal Objek JadwalGuru yang akan dihapus
     */
    private fun deleteJadwal(jadwal: JadwalGuru) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Jadwal")
            .setMessage("Yakin ingin menghapus jadwal ini?")
            .setPositiveButton("Hapus") { _, _ ->
                collection.document(jadwal.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Jadwal dihapus", Toast.LENGTH_SHORT).show()
                        loadJadwal()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
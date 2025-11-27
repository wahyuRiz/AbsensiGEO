package com.example.absensigeo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.data.JadwalAdapter
import com.example.absensigeo.data.JadwalGuru
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity untuk menampilkan daftar jadwal mengajar guru.
 *
 * Fitur utama:
 * - Menampilkan jadwal mengajar berdasarkan nama guru yang login
 * - Data diambil dari Firestore dan ditampilkan dalam RecyclerView
 * - Mendukung refresh otomatis ketika data berubah
 */
class JadwalGuruActivity : AppCompatActivity() {

    // Komponen UI
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: JadwalAdapter
    private lateinit var linearbtn: LinearLayout

    // Data
    private lateinit var listJadwal: MutableList<JadwalGuru>

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("jadwal")

    /**
     * Dipanggil saat activity pertama kali dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan, null jika pertama kali dibuat
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jadwal_guru)

        // Inisialisasi RecyclerView
        setupRecyclerView()

        // Ambil data nama guru dari SharedPreferences
        val sharedPref = getSharedPreferences("user", Context.MODE_PRIVATE)
        val namaGuru = sharedPref.getString("name", null)

        if (namaGuru != null) {
            // Jika nama guru tersedia, load jadwal
            loadJadwalGuru(namaGuru)
        } else {
            Toast.makeText(this, "Data login tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

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
        recyclerView = findViewById(R.id.recyclerViewJadwalGuru)

        // Menggunakan LinearLayoutManager untuk tampilan daftar vertikal
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inisialisasi list data dan adapter
        listJadwal = mutableListOf()
        adapter = JadwalAdapter(listJadwal)
        recyclerView.adapter = adapter
    }

    /**
     * Memuat data jadwal guru dari Firestore berdasarkan nama guru.
     *
     * @param namaGuru Nama guru yang digunakan sebagai filter
     */
    private fun loadJadwalGuru(namaGuru: String) {
        collection.whereEqualTo("namaGuru", namaGuru)
            .get()
            .addOnSuccessListener { result ->
                Log.d("DEBUG_JADWAL", "Jumlah data ditemukan: ${result.size()}")

                // Kosongkan list sebelum menambahkan data baru
                listJadwal.clear()

                // Konversi setiap dokumen ke object JadwalGuru
                for (doc in result.documents) {
                    Log.d("DEBUG_JADWAL", "Dokumen ditemukan: ${doc.data}")
                    val jadwal = doc.toObject(JadwalGuru::class.java)
                    if (jadwal != null) {
                        listJadwal.add(jadwal)
                    }
                }

                // Memberi tahu adapter bahwa data telah berubah
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat jadwal", Toast.LENGTH_SHORT).show()
                Log.e("JADWAL_ERROR", "Gagal memuat jadwal: ${it.message}")
            }
    }
}
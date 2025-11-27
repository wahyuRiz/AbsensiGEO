package com.example.absensigeo

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.data.JadwalGuruAdapter
import com.example.absensigeo.data.JadwalGuru
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity untuk menampilkan jadwal guru (readonly mode).
 */
class JadwalGuruKepalaActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: JadwalGuruAdapter
    private lateinit var listJadwal: MutableList<JadwalGuru>
    private lateinit var linearbtn: LinearLayout

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("jadwal")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jadwal_guru_admin)

        setupRecyclerView()
        loadJadwal()

        // Sembunyikan tombol tambah jika ada
        val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton?>(R.id.fab_add_jadwal)
        fab?.hide()

        linearbtn = findViewById(R.id.linear01)
        // Tombol back (custom)
        linearbtn.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewJadwal)
        recyclerView.layoutManager = LinearLayoutManager(this)

        listJadwal = mutableListOf()
        adapter = JadwalGuruAdapter(listJadwal, null, null) // Nonaktifkan edit/delete callbacks
        recyclerView.adapter = adapter
    }

    private fun loadJadwal() {
        collection.get().addOnSuccessListener { result ->
            Log.d("loadJadwal", "Jumlah data: ${listJadwal.size}")
            listJadwal.clear()
            for (doc in result) {
                val jadwal = doc.toObject(JadwalGuru::class.java).apply {
                    id = doc.id
                }
                listJadwal.add(jadwal)
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadJadwal()
    }
}
package com.example.absensigeo

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.data.BuktiMengajar
import com.example.absensigeo.data.HistoriAdapter
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class LihatBuktiActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historiAdapter: HistoriAdapter
    private val listHistori = mutableListOf<BuktiMengajar>()

    private lateinit var spinnerGuru: Spinner
    private val userIdList = mutableListOf<String>()
    private val displayNameMap = mutableMapOf<String, String>()

    private lateinit var linearbtn: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lihat_bukti)

        setupRecyclerView()
        setupSpinner()
        loadDaftarGuru()

        linearbtn = findViewById(R.id.linear01)
        // Tombol back (custom)
        linearbtn.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerHistori)
        recyclerView.layoutManager = LinearLayoutManager(this)
        historiAdapter = HistoriAdapter(listHistori)
        recyclerView.adapter = historiAdapter
    }

    private fun setupSpinner() {
        spinnerGuru = findViewById(R.id.spinnerGuru)
        spinnerGuru.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedUserId = userIdList[position]
                loadHistoriGuru(selectedUserId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadDaftarGuru() {
        val dbRef = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("bukti_mengajar")

        val firestore = FirebaseFirestore.getInstance()

        dbRef.get().addOnSuccessListener { snapshot ->
            val tempUserIds = snapshot.children.mapNotNull { it.key }

            if (tempUserIds.isEmpty()) return@addOnSuccessListener

            val uidToName = mutableMapOf<String, String>()
            var counter = 0

            for (uid in tempUserIds) {
                firestore.collection("user").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val nama = doc.getString("name") ?: uid  // Pastikan pakai "name" field
                        uidToName[uid] = nama
                        counter++
                        if (counter == tempUserIds.size) {
                            populateSpinnerWithNames(uidToName)
                        }
                    }
                    .addOnFailureListener {
                        uidToName[uid] = uid
                        counter++
                        if (counter == tempUserIds.size) {
                            populateSpinnerWithNames(uidToName)
                        }
                    }
            }
        }
    }

    private lateinit var displayUserIds: List<String>
    private fun populateSpinnerWithNames(uidToName: Map<String, String>) {
        displayUserIds = uidToName.keys.toList()
        val displayNames = uidToName.values.toList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, displayNames)
        spinnerGuru.adapter = adapter

        spinnerGuru.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedUserId = displayUserIds[position]
                loadHistoriGuru(selectedUserId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadHistoriGuru(userId: String) {
        val dbRef = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("bukti_mengajar/$userId")

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val last7Days = mutableSetOf<String>()
        for (i in 0..6) {
            last7Days.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        dbRef.get().addOnSuccessListener { snapshot ->
            listHistori.clear()
            for (tanggalSnap in snapshot.children) {
                val tanggal = tanggalSnap.key ?: continue
                if (tanggal !in last7Days) continue

                val materi = tanggalSnap.child("materi").value?.toString() ?: ""
                val deskripsi = tanggalSnap.child("deskripsi").value?.toString() ?: ""
                val namaFile = tanggalSnap.child("nama_file").value?.toString() ?: ""
                val url = tanggalSnap.child("url").value?.toString() ?: ""

                listHistori.add(BuktiMengajar(tanggal, namaFile, url, materi, deskripsi))
            }
            historiAdapter.notifyDataSetChanged()
        }
    }
}

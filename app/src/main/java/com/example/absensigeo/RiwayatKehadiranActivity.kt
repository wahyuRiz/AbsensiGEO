package com.example.absensigeo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.R
import com.example.absensigeo.data.Absensi
import com.example.absensigeo.data.AbsensiAdapter
import com.example.absensigeo.FullscreenImageActivity
import com.example.absensigeo.data.AbsensiDisplayItem
import com.example.absensigeo.data.AbsensiGroupAdapter
import com.example.absensigeo.data.AbsensiGroupItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity untuk menampilkan riwayat kehadiran pengguna.
 *
 * Fitur utama:
 * - Menampilkan daftar riwayat absensi dalam RecyclerView
 * - Mengambil data dari Firebase Realtime Database
 * - Menampilkan gambar bukti absensi dalam mode fullscreen
 * - Auto-refresh saat data berubah
 */
class RiwayatKehadiranActivity : AppCompatActivity() {

    // Komponen UI
    private lateinit var recyclerView: RecyclerView

    // Adapter untuk RecyclerView
    private var absensiAdapter: AbsensiGroupAdapter? = null

    // List untuk menyimpan data absensi
    private val absensiGroupList = mutableListOf<AbsensiGroupItem>()

    private lateinit var linearbtn: LinearLayout

    /**
     * Dipanggil saat activity pertama kali dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("RiwayatKehadiran", "Activity dibuat")
        setContentView(R.layout.activity_riwayat_kehadiran)

        // Inisialisasi RecyclerView
        setupRecyclerView()

        // Ambil data dari Firebase
//        loadAttendanceData()
        cekRoleDanAmbilData()

        linearbtn = findViewById(R.id.linear01)
        // Tombol back (custom)
        linearbtn.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    /**
     * Menyiapkan RecyclerView untuk menampilkan data absensi.
     */
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvRiwayatAbsensi)

        // Menggunakan LinearLayoutManager untuk tampilan daftar vertikal
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    /**
     * Memuat data absensi dari Firebase Realtime Database.
     */
    private fun loadAttendanceData() {
        Log.d("RiwayatKehadiran", "Memulai pengambilan data dari Firebase")

        // Dapatkan UID pengguna yang sedang login
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "Pengguna tidak terautentikasi", Toast.LENGTH_SHORT).show()
            return
        }

        // Referensi ke path absensi pengguna
        val ref = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Absensi").child(uid)


        // Listener untuk mengambil data sekali
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("RiwayatKehadiran", "Data diterima, jumlah: ${snapshot.childrenCount}")

                // Kosongkan list sebelum menambahkan data baru
                val tempList = mutableListOf<Absensi>()

                // Loop melalui semua data absensi
                for (child in snapshot.children) {
                    val absensi = child.getValue(Absensi::class.java)
                    absensi?.let {
                        // Tambahkan ke list jika data valid
                        tempList.add(it)
                        Log.d("RiwayatKehadiran", "Data ditambahkan: $it")
                    }
                }
                absensiGroupList.clear()
                absensiGroupList.addAll(groupAbsensiByDate(tempList))

                // Setup adapter dengan callback untuk klik gambar
                setupAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RiwayatKehadiran", "Gagal mengambil data: ${error.message}")
                Toast.makeText(
                    this@RiwayatKehadiranActivity,
                    "Gagal mengambil data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun cekRoleDanAmbilData() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("user").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val role = doc.getString("role")
                    if (role == "kepala") {
                        loadAllAttendanceData() // <-- admin mode
                    } else {
                        loadAttendanceData() // <-- user biasa
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal cek role", Toast.LENGTH_SHORT).show()
                loadAttendanceData()
            }
    }

    private fun loadAllAttendanceData() {
        val ref = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Absensi")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<Absensi>()
                for (userSnapshot in snapshot.children) {
                    for (absenSnapshot in userSnapshot.children) {
                        val absensi = absenSnapshot.getValue(Absensi::class.java)
                        absensi?.let { tempList.add(it) }
                    }
                }

                absensiGroupList.clear()
                absensiGroupList.addAll(groupAbsensiByDate(tempList))
                setupAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RiwayatKehadiran", "Gagal mengambil semua data: ${error.message}")
            }
        })
    }


    /**
     * Menyiapkan adapter untuk RecyclerView dengan callback klik gambar.
     */
//    private fun setupAdapter() {
//        absensiAdapter = AbsensiAdapter(absensiList) { imageUrl ->
//            // Callback ketika gambar di klik
//            showFullscreenImage(imageUrl)
//        }
//
//        // Set adapter ke RecyclerView
//        recyclerView.adapter = absensiAdapter
//
//        // Beri tahu adapter bahwa data telah berubah
//        absensiAdapter?.notifyDataSetChanged()
//        Log.d("RiwayatKehadiran", "Adapter di-set dengan ${absensiList.size} item")
//    }
    private fun setupAdapter() {
        absensiAdapter = AbsensiGroupAdapter(absensiGroupList) { imageUrl ->
            showFullscreenImage(imageUrl)
        }
        recyclerView.adapter = absensiAdapter
        absensiAdapter?.notifyDataSetChanged()
    }


    private fun groupAbsensiByDate(list: List<Absensi>): List<AbsensiGroupItem> {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        return list.groupBy { dateFormat.format(Date(it.waktu)) }
            .map { (tanggal, items) -> AbsensiGroupItem(tanggal, items.sortedByDescending { it.waktu }) }
            .sortedByDescending {
                // Parse tanggal grup kembali ke Date untuk sorting
                dateFormat.parse(it.tanggal)
            }
    }


    /**
     * Menampilkan gambar dalam mode fullscreen.
     *
     * @param imageUrl URL gambar yang akan ditampilkan
     */
    private fun showFullscreenImage(imageUrl: String) {
        Log.d("RiwayatKehadiran", "Membuka gambar fullscreen: $imageUrl")
        val intent = Intent(this, FullscreenImageActivity::class.java)
        intent.putExtra("imageUrl", imageUrl)
        startActivity(intent)
    }
}
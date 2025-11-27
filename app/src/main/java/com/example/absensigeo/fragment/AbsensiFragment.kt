package com.example.absensigeo.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.absensigeo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.UUID
import android.util.Log
import androidx.navigation.fragment.findNavController
import com.example.absensigeo.CameraPreviewFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog
import android.location.Location
import android.widget.EditText
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import java.util.Calendar

/**
 * Fragment yang menangani proses absensi berdasarkan lokasi pengguna.
 * Menggunakan OpenStreetMap untuk menampilkan peta, serta Firebase untuk menyimpan data absensi.
 *
 * Pengguna dapat melakukan absen masuk dan pulang, dengan validasi lokasi dan waktu.
 */
class AbsensiFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var btnAbsenmsk: Button
    private lateinit var btnAbsenplg: Button
    private lateinit var btnIzinCuti: Button

    //Koordinat lokasi sekolah
    private val SCHOOL_LATITUDE = -1.8522909597985264
    private val SCHOOL_LONGITUDE = 106.1316275965487

//    private val SCHOOL_LATITUDE = -1.8901692
//    private val SCHOOL_LONGITUDE = 106.1134350

    // Radius maksimum lokasi absen (dalam meter)
    private val ALLOWED_RADIUS_METERS = 1000.0

    private lateinit var locationOverlay: MyLocationNewOverlay
    private val TAG = "AbsensiFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inisialisasi konfigurasi peta OSM
        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osm_prefs", 0))
        val view = inflater.inflate(R.layout.fragment_absensi, container, false)

        // Inisialisasi view
        mapView = view.findViewById(R.id.map)
        btnAbsenmsk = view.findViewById(R.id.btn_msk)
        btnAbsenplg = view.findViewById(R.id.btn_plg)
        btnIzinCuti = view.findViewById(R.id.btnIzinCuti)

        cekSudahIzinHariIni()
        cekRoleUserDanSetTombol()

        // Konfigurasi peta
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        val mapController = mapView.controller
        mapController.setZoom(18.0)
        val startPoint = GeoPoint(SCHOOL_LATITUDE, SCHOOL_LONGITUDE)
        mapController.setCenter(startPoint)

        // Cek dan minta izin lokasi
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            val provider = GpsMyLocationProvider(requireContext())
            locationOverlay = MyLocationNewOverlay(provider, mapView)
            locationOverlay.enableMyLocation()
            locationOverlay.enableFollowLocation()
            mapView.overlays.add(locationOverlay)
        }

        // Aksi tombol absen masuk
        btnAbsenmsk.setOnClickListener {
            val loc = locationOverlay.myLocation
            if (loc != null) {
                if (isInsideSchoolArea(loc.latitude, loc.longitude)) {
                    val waktuSekarang = System.currentTimeMillis()
                    val batasMasuk = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 7)
                        set(Calendar.MINUTE, 15)
                    }.timeInMillis

                    if (waktuSekarang > batasMasuk) {
                        // Telat → tampilkan dialog alasan
                        showAlasanDialog { alasan ->
                            if (alasan.isNotBlank()) {
                                saveAttendance(loc.latitude, loc.longitude, "masuk", alasan)
                            } else {
                                Toast.makeText(requireContext(), "Alasan wajib diisi saat terlambat", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        saveAttendance(loc.latitude, loc.longitude, "masuk", null)
                    }
                } else {
                    Toast.makeText(requireContext(), "Di luar area sekolah!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Lokasi belum terdeteksi", Toast.LENGTH_SHORT).show()
            }
        }

        // Aksi tombol absen pulang
        btnAbsenplg.setOnClickListener {
            val loc = locationOverlay.myLocation
            if (loc != null) {
                if (isInsideSchoolArea(loc.latitude, loc.longitude)) {
                    saveAttendance(
                        loc.latitude, loc.longitude, "pulang",
                        alasan = null // FIXED: absensi pulang tidak butuh alasan
                    )
                } else {
                    Toast.makeText(requireContext(), "Di luar area sekolah!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Lokasi belum terdeteksi", Toast.LENGTH_SHORT).show()
            }
        }


        btnIzinCuti.setOnClickListener {
            findNavController().navigate(R.id.action_absensiFragment_to_izinCutiFragment)
        }


        return view
    }

    private fun showAlasanDialog(onSubmit: (String) -> Unit) {
        val input = EditText(requireContext())
        input.hint = "Masukkan alasan keterlambatan"

        AlertDialog.Builder(requireContext())
            .setTitle("Terlambat Absen")
            .setMessage("Silakan isi alasan keterlambatan:")
            .setView(input)
            .setPositiveButton("Kirim") { _, _ ->
                onSubmit(input.text.toString().trim())
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Mengecek apakah sudah melakukan absensi hari ini
     */
    private fun cekSudahIzinHariIni() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val awalHari = calendar.timeInMillis
        val akhirHari = awalHari + 24 * 60 * 60 * 1000

        val dbRef = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Absensi").child(userId)

        dbRef.orderByChild("waktu")
            .startAt(awalHari.toDouble())
            .endAt(akhirHari.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var sudahMasuk = false
                    var sudahPulang = false
                    var sudahIzinAtauCuti = false

                    for (data in snapshot.children) {
                        when (data.child("jenis").getValue(String::class.java)) {
                            "masuk" -> sudahMasuk = true
                            "pulang" -> sudahPulang = true
                            "Izin", "Cuti" -> sudahIzinAtauCuti = true
                        }
                    }

                    if (sudahIzinAtauCuti) {
                        btnAbsenmsk.isEnabled = false
                        btnAbsenplg.isEnabled = false
                        btnIzinCuti.isEnabled = false
                        Toast.makeText(requireContext(), "Anda sudah mengajukan izin/cuti hari ini", Toast.LENGTH_SHORT).show()
                        return
                    }

                    if (sudahMasuk) {
                        btnAbsenmsk.isEnabled = false
                    }

                    val sekarang = Calendar.getInstance()
                    val jamSekarang = sekarang.get(Calendar.HOUR_OF_DAY)
                    val menitSekarang = sekarang.get(Calendar.MINUTE)
                    val sudahLewatJam16 = jamSekarang > 16 || (jamSekarang == 16 && menitSekarang >= 0)

                    if (sudahPulang && !sudahMasuk) {
                        btnAbsenplg.isEnabled = false
                        Toast.makeText(requireContext(), "Tidak bisa absen pulang tanpa absen masuk", Toast.LENGTH_SHORT).show()
                    } else {
                        btnAbsenplg.isEnabled = sudahLewatJam16 && !sudahPulang && sudahMasuk
                    }

                    btnIzinCuti.isEnabled = !sudahMasuk && !sudahPulang
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Gagal cek izin/absen: ${error.message}")
                }
            })
    }


    /**
     * Mengecek apakah apakah role=admin
     */
    private fun cekRoleUserDanSetTombol() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("user").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    if (role == "admin") {
                        btnAbsenmsk.isEnabled = false
                        btnAbsenplg.isEnabled = false
                        btnIzinCuti.isEnabled = false
                        Toast.makeText(requireContext(), "Admintidak bisa melakukan absen atau izin", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "Dokumen user tidak ditemukan")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal mengambil role dari Firestore: ${e.message}")
            }
    }

    /**
     * Mengecek apakah posisi saat ini berada dalam radius yang diizinkan dari lokasi sekolah.
     */
    private fun isInsideSchoolArea(currentLat: Double, currentLon: Double): Boolean {
        val result = FloatArray(1)
        Location.distanceBetween(
            currentLat, currentLon,
            SCHOOL_LATITUDE, SCHOOL_LONGITUDE,
            result
        )
        return result[0] <= ALLOWED_RADIUS_METERS
    }

    /**
     * Menyimpan data absensi ke Firebase jika pengguna belum melakukan absen untuk jenis yang sama pada hari tersebut.
     * Juga melakukan validasi waktu masuk dan pulang.
     */
    private fun saveAttendance(currentLat: Double, currentLon: Double, jenis: String, alasan: String?) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e(TAG, "User null / belum login")
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        val waktuSekarang = System.currentTimeMillis()
        val userId = user.uid

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val awalHari = calendar.timeInMillis
        val akhirHari = awalHari + 24 * 60 * 60 * 1000

        val dbRef = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Absensi").child(userId)

        dbRef.orderByChild("waktu")
            .startAt(awalHari.toDouble())
            .endAt(akhirHari.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var sudahAbsen = false
                    for (data in snapshot.children) {
                        val jenisAbsen = data.child("jenis").getValue(String::class.java)
                        if (jenisAbsen == jenis) {
                            sudahAbsen = true
                            break
                        }
                    }

                    if (sudahAbsen) {
                        Toast.makeText(requireContext(), "Anda sudah absen $jenis hari ini", Toast.LENGTH_SHORT).show()
                        return
                    }

                    var keterangan = "Hadir"
                    if (jenis == "masuk") {
                        val batasMasuk = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 7)
                            set(Calendar.MINUTE, 15)
                        }
                        if (waktuSekarang > batasMasuk.timeInMillis) {
                            keterangan = "Terlambat"
                        }
                    } else if (jenis == "pulang") {
                        val batasPulang = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 16)
                            set(Calendar.MINUTE, 0)
                        }

                        if (waktuSekarang < batasPulang.timeInMillis) {
                            Toast.makeText(requireContext(), "Belum bisa absen pulang sebelum jam 16:00", Toast.LENGTH_SHORT).show()
                            return
                        }
                    }

                    val absenData = mutableMapOf(
                        "name" to user.displayName,
                        "waktu" to waktuSekarang,
                        "latitude" to currentLat,
                        "longitude" to currentLon,
                        "jenis" to jenis,
                        "keterangan" to keterangan
                    )

                    if (!alasan.isNullOrBlank()) {
                        absenData["alasan"] = alasan
                    }

                    val absenId = dbRef.push().key!!
                    dbRef.child(absenId).setValue(absenData)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(requireContext(), "Absen berhasil, silakan ambil foto", Toast.LENGTH_SHORT).show()
                                Log.d(TAG, "Data yang dikirim: $absenData")
                                val action = AbsensiFragmentDirections.actionAbsensiFragmentToCameraPreviewFragment(userId, absenId)
                                findNavController().navigate(action)
                            } else {
                                Log.e(TAG, "Gagal menyimpan absen: ${task.exception?.message}")
                                Toast.makeText(requireContext(), "Gagal menyimpan absen", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Error saat menyimpan absen: ${it.message}")
                            Toast.makeText(requireContext(), "Terjadi kesalahan saat menyimpan", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Database error: ${error.message}")
                    Toast.makeText(requireContext(), "Gagal mengakses database", Toast.LENGTH_SHORT).show()
                }
            })
    }


    /**
     * Callback saat izin lokasi diberikan.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val provider = GpsMyLocationProvider(requireContext())
            locationOverlay = MyLocationNewOverlay(provider, mapView)
            locationOverlay.enableMyLocation()
            locationOverlay.enableFollowLocation()
            mapView.overlays.add(locationOverlay)
        }
    }

    /**
     * Lifecycle method saat Fragment aktif.
     */
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    /**
     * Lifecycle method saat Fragment tidak aktif.
     */
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}

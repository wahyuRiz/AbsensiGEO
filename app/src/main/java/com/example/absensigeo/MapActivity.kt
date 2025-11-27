package com.example.absensigeo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import androidx.preference.PreferenceManager

/**
 * Activity untuk menampilkan peta dan lokasi pengguna.
 *
 * Fitur utama:
 * - Menampilkan peta menggunakan OpenStreetMap
 * - Menampilkan lokasi sekolah sebagai pusat peta
 * - Menampilkan lokasi pengguna secara real-time
 * - Meminta izin akses lokasi
 * - Zoom otomatis ke lokasi sekolah
 */
class MapActivity : AppCompatActivity() {

    // Komponen UI
    private lateinit var mapView: MapView

    // Overlay untuk lokasi pengguna
    private lateinit var locationOverlay: MyLocationNewOverlay

    // Koordinat sekolah (SMK Negeri 1 Sungailiat)
    private val SCHOOL_LATITUDE = -1.8522909597985264
    private val SCHOOL_LONGITUDE = 106.1316275965487


    /**
     * Dipanggil saat activity pertama kali dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Konfigurasi awal osmdroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_map)

        // Inisialisasi peta
        setupMapView()

        // Cek dan minta izin lokasi
        checkLocationPermission()
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        val mapTitle: TextView = findViewById(R.id.mapTitle)
        mapTitle.setOnClickListener {
            finish()
        }
    }

    /**
     * Menyiapkan tampilan peta dan mengatur lokasi sekolah sebagai pusat peta.
     */
    private fun setupMapView() {
        mapView = findViewById(R.id.map)

        // Menggunakan tile source MAPNIK (OpenStreetMap default)
        mapView.setTileSource(TileSourceFactory.MAPNIK)

        // Mengaktifkan kontrol multi-touch
        mapView.setMultiTouchControls(true)

        // Mengatur controller untuk zoom dan pusat peta
        val mapController = mapView.controller
        mapController.setZoom(18.0) // Level zoom cukup dekat
        mapController.setCenter(GeoPoint(SCHOOL_LATITUDE, SCHOOL_LONGITUDE)) // Pusatkan ke sekolah
    }

    /**
     * Memeriksa dan meminta izin akses lokasi.
     */
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Minta izin jika belum diberikan
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1)
        } else {
            // Jika izin sudah diberikan, setup overlay lokasi
            setupLocationOverlay()
        }
    }

    /**
     * Menyiapkan overlay untuk menampilkan lokasi pengguna.
     */
    private fun setupLocationOverlay() {
        // Provider lokasi menggunakan GPS
        val provider = GpsMyLocationProvider(this)

        // Buat overlay lokasi pengguna
        locationOverlay = MyLocationNewOverlay(provider, mapView)

        // Aktifkan fitur lokasi
        locationOverlay.enableMyLocation()

        // Aktifkan follow location (pusat peta mengikuti lokasi pengguna)
        locationOverlay.enableFollowLocation()

        // Tambahkan overlay ke peta
        mapView.overlays.add(locationOverlay)
    }

    /**
     * Menangani hasil permintaan izin.
     *
     * @param requestCode Kode permintaan
     * @param permissions Daftar izin yang diminta
     * @param grantResults Hasil pemberian izin
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Jika izin diberikan, setup overlay lokasi
            setupLocationOverlay()
        } else {
            // Jika izin ditolak, tampilkan pesan
            Toast.makeText(this,
                "Izin lokasi diperlukan untuk menampilkan posisi",
                Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Dipanggil saat activity kembali ke foreground.
     */
    override fun onResume() {
        super.onResume()
        // Resume mapView
        mapView.onResume()
    }

    /**
     * Dipanggil saat activity masuk ke background.
     */
    override fun onPause() {
        super.onPause()
        // Pause mapView
        mapView.onPause()
    }
}
package com.example.absensigeo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.FirebaseApp
import com.example.absensigeo.databinding.ActivityMainBinding
import com.simform.custombottomnavigation.Model

/**
 * Activity utama yang menampilkan bottom navigation dan mengelola navigasi antar fragment.
 *
 * Fitur utama:
 * - Menampilkan bottom navigation berdasarkan role pengguna
 * - Mengelola navigasi antar fragment utama
 * - Menangani perubahan profil dan update UI
 * - Menyimpan waktu aktif terakhir untuk auto-logout
 */
class MainActivity : AppCompatActivity() {

    // View Binding
    private lateinit var binding: ActivityMainBinding

    //variabel untuk menyimpan waktu terakhir tombol Back ditekan
    private var lastBackPressedTime: Long = 0

    // Activity result launcher untuk update profil
    private lateinit var profileLauncher: ActivityResultLauncher<Intent>

    // Flag untuk reset home fragment setelah update profil
    private var needResetHome = false

    // ID destination home berdasarkan role
    private var homeDestinationId: Int = R.id.navigation_home

    // Daftar menu bottom navigation
    private lateinit var menuItems: Array<Model>

    /**
     * Dipanggil saat activity pertama kali dibuat.
     *
     * @param savedInstanceState State sebelumnya yang disimpan
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi Firebase
        FirebaseApp.initializeApp(this)

        // Setup View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Navigation Controller
        setupNavigationController()

        // Setup Bottom Navigation
        setupBottomNavigation()

        // Setup Profile Launcher
        setupProfileLauncher()


    }

    /**
     * Menyiapkan Navigation Controller dan menentukan home fragment berdasarkan role.
     */
    private fun setupNavigationController() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // Ambil role pengguna dari intent atau SharedPreferences
        val role = intent.getStringExtra("ROLE") ?: getSharedPreferences("loginPrefs", MODE_PRIVATE)
            .getString("userRole", "guru")

        // Tentukan home fragment berdasarkan role
        homeDestinationId = when (role) {
            "admin", "kepala" -> R.id.navigation_home_admin
            "guru", "staf" -> R.id.navigation_home
            else -> {
                Toast.makeText(this, "Role tidak dikenali", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }
        }

        // Navigasi ke home fragment yang sesuai
        navController.navigate(homeDestinationId)
    }

    /**
     * Menyiapkan Bottom Navigation Menu berdasarkan role.
     */
    private fun setupBottomNavigation() {
        // Inisialisasi menu items
        menuItems = arrayOf(
            Model(R.drawable.ic_home, homeDestinationId, 0, R.string.title_home),
            Model(R.drawable.ic_calendar, R.id.navigation_calendar, 1, R.string.title_calendar),
            Model(R.drawable.ic_camera, R.id.navigation_absensi, 2, R.string.title_absensi),
            Model(R.drawable.ic_document, R.id.navigation_upload_document, 3, R.string.title_upload_document),
            Model(R.drawable.ic_user, R.id.navigation_profile, 4, R.string.title_profile)
        )

        // Setup bottom navigation dengan nav controller
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        binding.bottomNavigation.setMenuItems(menuItems, 0)
        binding.bottomNavigation.setupWithNavController(navHostFragment.navController)

        // Listener untuk klik menu
        binding.bottomNavigation.setOnMenuItemClickListener { _, index ->
            handleMenuItemClick(index)
        }

        // Listener untuk perubahan destination
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            handleDestinationChange(destination.id)
        }
    }

    /**
     * Menangani klik item menu bottom navigation.
     *
     * @param index Index menu yang diklik
     * @return Boolean apakah event sudah ditangani
     */
    private fun handleMenuItemClick(index: Int): Boolean {
        val destinationId = menuItems[index].destinationId
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // Jika menu profile diklik
        if (destinationId == R.id.navigation_profile) {
            val role = getSharedPreferences("loginPrefs", MODE_PRIVATE).getString("userRole", "guru")
            val intent = when (role) {
                "guru", "staf" -> Intent(this, ProfileActivity::class.java)
                "admin", "kepala" -> Intent(this, ProfileAdminActivity::class.java)
                else -> null
            }

            intent?.let { profileLauncher.launch(it) }
            return false
        }

        // Navigasi ke fragment tujuan jika belum aktif
        if (navController.currentDestination?.id != destinationId) {
            navController.navigate(destinationId)
        }
        return true
    }

    /**
     * Menangani perubahan destination fragment.
     *
     * @param destinationId ID destination yang aktif
     */
    private fun handleDestinationChange(destinationId: Int) {
        // Sembunyikan bottom nav di fullscreen image
        binding.bottomNavigation.visibility =
            if (destinationId == R.id.fullscreenImageFragment) View.GONE else View.VISIBLE

        // Update selected index
        val index = menuItems.indexOfFirst { it.destinationId == destinationId }
        if (index != -1) {
            binding.bottomNavigation.setSelectedIndex(index)
        }
    }

    /**
     * Menyiapkan Activity Result Launcher untuk update profil.
     */
    private fun setupProfileLauncher() {
        profileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                needResetHome = true
            }
        }
    }

    /**
     * Dipanggil saat activity kembali ke foreground.
     */
    override fun onResume() {
        super.onResume()

        // Reset home fragment jika diperlukan
        if (needResetHome) {
            resetHomeFragment()
            needResetHome = false
        }
    }

    /**
     * Mengarahkan kembali ke home fragment.
     */
    private fun resetHomeFragment() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        if (navController.currentDestination?.id != homeDestinationId) {
            navController.navigate(homeDestinationId)
        }

        binding.bottomNavigation.setSelectedIndex(0)
    }

    /**
     * Dipanggil saat activity berhenti.
     */
    override fun onStop() {
        super.onStop()
        // Simpan waktu aktif terakhir untuk auto-logout
        getSharedPreferences("loginPrefs", MODE_PRIVATE).edit()
            .putLong("lastActiveTime", System.currentTimeMillis())
            .apply()
    }

    /**
     * 2x tekan untuk keluar
     */
    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressedTime < 2000) {
            super.onBackPressed() // Keluar aplikasi
        } else {
            lastBackPressedTime = currentTime
            Toast.makeText(this, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * Menyimpan state activity.
     *
     * @param outState Bundle untuk menyimpan state
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("activeIndex", binding.bottomNavigation.getSelectedIndex())
    }
}
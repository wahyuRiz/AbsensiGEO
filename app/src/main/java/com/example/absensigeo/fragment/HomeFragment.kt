package com.example.absensigeo.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.absensigeo.JadwalGuruActivity
import com.example.absensigeo.MapActivity
import com.example.absensigeo.R
import com.example.absensigeo.RiwayatKehadiranActivity
import com.example.absensigeo.UploadBuktiActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fragment utama yang menampilkan informasi profil pengguna, salam waktu, hari dalam seminggu,
 * serta menu navigasi ke fitur lain seperti Jadwal Guru, Riwayat Kehadiran, Dokumen Mengajar, dan Lokasi.
 *
 * Fragment ini juga menampilkan nama pengguna, NIP, peran, serta gambar profil yang diambil dari Firebase Firestore.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var tvNama: TextView
    private lateinit var tvNip: TextView
    private lateinit var tvRole: TextView
    private lateinit var greetingText: TextView
    private lateinit var timeText: TextView
    private var handler = Handler(Looper.getMainLooper())

    /**
     * Membuat tampilan fragment dan menginisialisasi UI, termasuk menu, salam, dan jam real-time.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        /**
         * Menentukan hari saat ini dan menampilkan daftar hari dalam seminggu
          */
        val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        val todayIndex = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
        val container = view.findViewById<LinearLayout>(R.id.dayContainer)

        days.forEachIndexed { index, day ->
            val textView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = if (index != days.lastIndex) 8 else 0
                }
                text = day
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(12, 12, 12, 12)
                setTextColor(if (index == todayIndex) Color.WHITE else Color.parseColor("#1F2937"))
                background = ContextCompat.getDrawable(
                    requireContext(),
                    if (index == todayIndex) R.drawable.bg_day_active else R.drawable.bg_day_normal
                )
            }
            container.addView(textView)
        }

        /**
         * Inisialisasi elemen UI untuk profil dan salam
          */
        tvNama = view.findViewById(R.id.tv_nama)
        tvNip = view.findViewById(R.id.tv_nip)
        tvRole = view.findViewById(R.id.tv_role)
        greetingText = view.findViewById(R.id.tv_greeting)
        timeText = view.findViewById(R.id.tv_time)

        tvNama.viewTreeObserver.addOnGlobalLayoutListener {
            val isEllipsized = tvNama.layout?.let {
                it.getEllipsisCount(it.lineCount - 1) > 0
            } ?: false

            if (isEllipsized) {
                tvNama.gravity = Gravity.START
                tvNama.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            } else {
                tvNama.gravity = Gravity.CENTER
                tvNama.textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
        }

        setupMenu(view)
        fetchUserInfo(view)
        updateGreeting()
        startClock()

        return view
    }

    /**
     * Menyiapkan navigasi dan tampilan menu dengan ikon dan teks.
     */
    private fun setupMenu(view: View) {
        val menuItems = listOf(
            Triple(R.id.cardMenu1, "Jadwal Guru", R.drawable.ic_jadwalguru),
            Triple(R.id.cardMenu2, "Riwayat Kehadiran", R.drawable.ic_riwayatkehadiran),
            Triple(R.id.cardMenu3, "Upload Dokumen Mengajar", R.drawable.ic_document),
            Triple(R.id.cardMenu4, "Lokasi", R.drawable.ic_lokasi)
        )

        for ((cardId, title, icon) in menuItems) {
            val cardLayout = view.findViewById<View>(cardId)
            val textView = cardLayout.findViewById<TextView>(R.id.menu_text)
            val imageView = cardLayout.findViewById<ImageView>(R.id.menu_icon)

            textView.text = title
            imageView.setImageResource(icon)

            cardLayout.setOnClickListener {
                when (title) {
                    "Jadwal Guru" -> startActivity(Intent(requireContext(), JadwalGuruActivity::class.java))
                    "Riwayat Kehadiran" -> startActivity(Intent(requireContext(), RiwayatKehadiranActivity::class.java))
                    "Upload Dokumen Mengajar" -> startActivity(Intent(requireContext(), UploadBuktiActivity::class.java))
                    "Lokasi" -> startActivity(Intent(requireContext(), MapActivity::class.java))
                }
            }
        }
    }

    /**
     * Mengambil data pengguna dari Firestore dan menampilkan ke UI.
     * Termasuk nama, NIP, role, dan foto profil (jika ada).
     */
    private fun fetchUserInfo(view: View) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        val profileImageView = view.findViewById<ImageView>(R.id.profileImageView)

        currentUser?.email?.let { email ->
            db.collection("user")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val doc = result.documents[0]
                        tvNama.text = doc.getString("name") ?: "N/A"
                        tvNip.text = doc.getString("nip") ?: "N/A"
                        tvRole.text = doc.getString("role")?.uppercase() ?: "N/A"

                        val photoBase64 = doc.getString("photo")
                        if (!photoBase64.isNullOrEmpty()) {
                            try {
                                val decodedBytes = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT)
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                Glide.with(this)
                                    .asBitmap()
                                    .load(bitmap)
                                    .circleCrop()
                                    .into(profileImageView)
                            } catch (e: Exception) {
                                Log.e("HomeFragment", "Invalid photo data", e)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HomeFragment", "Failed to fetch user info", e)
                }
        }
    }

    /**
     * Mengatur teks ucapan berdasarkan waktu saat ini (pagi, siang, sore, malam).
     */
    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 4..10 -> "Selamat Pagi"
            in 11..14 -> "Selamat Siang"
            in 15..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }

        val userName = view?.findViewById<TextView>(R.id.tv_nama)?.text ?: ""
        greetingText.text = "$greeting, $userName"
    }

    /**
     * Menampilkan jam real-time di UI yang diperbarui setiap detik.
     */
    private fun startClock() {
        handler.post(object : Runnable {
            override fun run() {
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                timeText.text = currentTime
                handler.postDelayed(this, 1000)
            }
        })
    }
}

package com.example.absensigeo.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.absensigeo.R

/**
 * Fragment untuk menampilkan gambar dalam mode layar penuh.
 *
 * Fitur utama:
 * - Menampilkan gambar dari URL dengan library Glide
 * - Mendukung placeholder dan error image
 * - Bisa ditutup dengan klik gambar atau tombol back
 * - Menggunakan navigation component untuk kembali ke fragment sebelumnya
 */
class FullscreenImageFragment : Fragment() {

    /**
     * Membuat tampilan fragment dengan menginflate layout
     *
     * @param inflater LayoutInflater untuk menginflate layout
     * @param container ViewGroup parent
     * @param savedInstanceState Bundle yang menyimpan state sebelumnya
     * @return View yang telah diinflate
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fullscreen_image, container, false)
    }

    /**
     * Dipanggil setelah tampilan dibuat, menginisialisasi komponen dan menampilkan gambar
     *
     * @param view View yang telah dibuat
     * @param savedInstanceState Bundle yang menyimpan state sebelumnya
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Ambil URL gambar dari arguments
        val imageUrl = arguments?.getString("imageUrl") ?: return
        val imageView = view.findViewById<ImageView>(R.id.fullscreenImageView)

        // Load gambar dengan Glide
        Glide.with(requireContext())
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_image) // Gambar placeholder saat loading
            .error(R.drawable.error_image) // Gambar error jika load gagal
            .into(imageView)

        // Handle klik pada gambar untuk kembali
        imageView.setOnClickListener {
            findNavController().popBackStack()
        }

        // Handle tombol back device
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                /**
                 * Dipanggil ketika tombol back ditekan
                 */
                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }
            }
        )
    }
}
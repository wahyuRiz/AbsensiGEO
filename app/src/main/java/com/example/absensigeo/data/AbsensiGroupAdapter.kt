package com.example.absensigeo.data

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.absensigeo.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Menampilkan 1 grup tanggal + semua absensi dalam 1 blok dengan background.
 */
class AbsensiGroupAdapter(
    private val groupedItems: List<AbsensiGroupItem>,
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<AbsensiGroupAdapter.GroupViewHolder>() {

    private val expandedStates = mutableMapOf<String, Boolean>()

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvIcon: TextView = view.findViewById(R.id.tvTipe)
        val containerIsi: LinearLayout = view.findViewById(R.id.containerIsi)
        val headerLayout: LinearLayout = view.findViewById(R.id.headerLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_absensi_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groupedItems[position]
        val tanggal = group.tanggal
        val context = holder.itemView.context
        val isExpanded = expandedStates[tanggal] ?: true

        holder.tvTanggal.text = "Tanggal: $tanggal"
        holder.tvIcon.text = if (isExpanded) "\u25BC" else "\u25B6"
        holder.containerIsi.removeAllViews()

        if (isExpanded) {
            for (absensi in group.absensiList) {
                val itemView = LayoutInflater.from(context)
                    .inflate(R.layout.item_absensi, holder.containerIsi, false)

                val tvNamaPegawai = itemView.findViewById<TextView>(R.id.tvNamaPegawai)
                val tvTanggalJam = itemView.findViewById<TextView>(R.id.tvTanggalJam)
                val tvJenisAbsensi = itemView.findViewById<TextView>(R.id.tvJenisAbsensi)
                val tvKeterangan = itemView.findViewById<TextView>(R.id.tvKeterangan)
                val ivFoto = itemView.findViewById<ImageView>(R.id.ivFotoAbsensi)
                val tvSurat = itemView.findViewById<TextView>(R.id.tvSuratIzin)
                val tvAlasan = itemView.findViewById<TextView>(R.id.tvAlasan)


                val fullDateTimeFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale("id", "ID"))
                tvNamaPegawai.text = "${absensi.name ?: "Tidak diketahui"}"
                tvTanggalJam.text = "Timestamp: ${fullDateTimeFormat.format(Date(absensi.waktu))}"
                tvJenisAbsensi.text = "Absensi: ${absensi.jenis.capitalize()}"
                tvKeterangan.text = "Keterangan: ${absensi.keterangan}"
                tvAlasan.text = "Alasan: ${absensi.alasan}"

                if (absensi.jenis.lowercase() == "izin" || absensi.jenis.lowercase() == "cuti") {
                    ivFoto.visibility = View.GONE
                    if (!absensi.suratUrl.isNullOrEmpty()) {
                        tvSurat.visibility = View.VISIBLE
                        tvSurat.text = "Lihat Surat"
                        tvSurat.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = android.net.Uri.parse(absensi.suratUrl)
                            context.startActivity(intent)
                        }
                    } else {
                        tvSurat.visibility = View.GONE
                    }
                } else {
                    tvSurat.visibility = View.GONE
                    if (!absensi.fotoUrl.isNullOrEmpty()) {
                        ivFoto.visibility = View.VISIBLE
                        Glide.with(context)
                            .load(absensi.fotoUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(ivFoto)

                        ivFoto.setOnClickListener {
                            onImageClick(absensi.fotoUrl!!)
                        }
                    } else {
                        ivFoto.visibility = View.GONE
                    }
                }

                when (absensi.keterangan.lowercase()) {
                    "hadir" -> tvKeterangan.setTextColor(0xFF388E3C.toInt())
                    "terlambat" -> tvKeterangan.setTextColor(0xFFF57C00.toInt())
                    "izin" -> tvKeterangan.setTextColor(0xFF1976D2.toInt())
                    else -> tvKeterangan.setTextColor(0xFFFF0000.toInt())
                }

                holder.containerIsi.addView(itemView)
            }
        }

        holder.headerLayout.setOnClickListener {
            expandedStates[tanggal] = !isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = groupedItems.size
}

/**
 * Model grup tanggal
 */
data class AbsensiGroupItem(
    val tanggal: String,
    val absensiList: List<Absensi>
)

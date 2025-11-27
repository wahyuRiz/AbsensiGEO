package com.example.absensigeo.data

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.absensigeo.R
import java.text.SimpleDateFormat
import java.util.*

class AbsensiAdapter(
    private val items: List<AbsensiDisplayItem>,
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    private val expandedStates = mutableMapOf<String, Boolean>()

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvIcon: TextView = view.findViewById(R.id.tvTipe)
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTanggalJam: TextView = view.findViewById(R.id.tvTanggalJam)
        val tvJenisAbsensi: TextView = view.findViewById(R.id.tvJenisAbsensi)
        val tvKeterangan: TextView = view.findViewById(R.id.tvKeterangan)
        val ivFoto: ImageView = view.findViewById(R.id.ivFotoAbsensi)
        val tvSurat: TextView = view.findViewById(R.id.tvSuratIzin)
        val tvAlasan: TextView = view.findViewById(R.id.tvAlasan)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AbsensiDisplayItem.Header -> TYPE_HEADER
            is AbsensiDisplayItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_riwayat_bukti, parent, false)
                view.setBackgroundResource(R.drawable.bg_item)
                return HeaderViewHolder(view)
            }
            TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_absensi, parent, false)
                return ItemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AbsensiDisplayItem.Header -> {
                val viewHolder = holder as HeaderViewHolder
                val tanggal = item.tanggal
                val isExpanded = expandedStates[tanggal] ?: true

                viewHolder.tvTanggal.text = "Tanggal: $tanggal"
                viewHolder.tvIcon.visibility = View.VISIBLE
                viewHolder.tvIcon.text = if (isExpanded) "\u25BC" else "\u25B6"

                viewHolder.itemView.setOnClickListener {
                    expandedStates[tanggal] = !isExpanded
                    notifyDataSetChanged()
                }
            }
            is AbsensiDisplayItem.Item -> {
                val absensi = item.absensi
                val tanggal = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                    .format(Date(absensi.waktu))
                val isExpanded = expandedStates[tanggal] ?: true
                if (!isExpanded) {
                    holder.itemView.visibility = View.GONE
                    holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
                    return
                } else {
                    holder.itemView.visibility = View.VISIBLE
                    holder.itemView.layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                val viewHolder = holder as ItemViewHolder
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

                viewHolder.tvTanggalJam.text = dateFormat.format(Date(absensi.waktu))
                viewHolder.tvJenisAbsensi.text = "Absensi: ${absensi.jenis.capitalize()}"
                viewHolder.tvKeterangan.text = "Keterangan: ${absensi.keterangan}"
                viewHolder.tvAlasan.text = "Alasan: ${absensi.alasan}"

                if (absensi.jenis.lowercase() == "izin" || absensi.jenis.lowercase() == "cuti") {
                    viewHolder.ivFoto.visibility = View.GONE
                    if (!absensi.suratUrl.isNullOrEmpty()) {
                        viewHolder.tvSurat.visibility = View.VISIBLE
                        viewHolder.tvSurat.text = "Lihat Surat"
                        viewHolder.tvSurat.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = android.net.Uri.parse(absensi.suratUrl)
                            viewHolder.itemView.context.startActivity(intent)
                        }
                    } else {
                        viewHolder.tvSurat.visibility = View.GONE
                    }
                } else {
                    viewHolder.tvSurat.visibility = View.GONE
                    if (!absensi.fotoUrl.isNullOrEmpty()) {
                        viewHolder.ivFoto.visibility = View.VISIBLE
                        Glide.with(viewHolder.itemView.context)
                            .load(absensi.fotoUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(viewHolder.ivFoto)

                        viewHolder.ivFoto.setOnClickListener {
                            onImageClick(absensi.fotoUrl!!)
                        }
                    } else {
                        viewHolder.ivFoto.visibility = View.GONE
                    }
                }

                when (absensi.keterangan.lowercase()) {
                    "hadir" -> viewHolder.tvKeterangan.setTextColor(Color.parseColor("#388E3C"))
                    "terlambat" -> viewHolder.tvKeterangan.setTextColor(Color.parseColor("#F57C00"))
                    "izin" -> viewHolder.tvKeterangan.setTextColor(Color.parseColor("#1976D2"))
                    else -> viewHolder.tvKeterangan.setTextColor(Color.RED)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}

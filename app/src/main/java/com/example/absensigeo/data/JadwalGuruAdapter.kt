package com.example.absensigeo.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.R

class JadwalGuruAdapter(
    private val listJadwal: List<JadwalGuru>,
    private val onEdit: ((JadwalGuru) -> Unit)?,
    private val onDelete: ((JadwalGuru) -> Unit)?
) : RecyclerView.Adapter<JadwalGuruAdapter.JadwalViewHolder>() {

    inner class JadwalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaGuru: TextView = itemView.findViewById(R.id.tv_namaGuru)
        val tvHari: TextView = itemView.findViewById(R.id.tv_hari)
        val tvKelas: TextView = itemView.findViewById(R.id.tv_kelas)
        val tvJam: TextView = itemView.findViewById(R.id.tv_jam)
        val tvMapel: TextView = itemView.findViewById(R.id.tv_mapel)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jadwal_guru, parent, false)
        return JadwalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JadwalViewHolder, position: Int) {
        val jadwal = listJadwal[position]
        holder.tvNamaGuru.text = jadwal.namaGuru
        holder.tvKelas.text = jadwal.kelas
        holder.tvHari.text = jadwal.hari
        holder.tvJam.text = "${jadwal.jamMulai} - ${jadwal.jamSelesai}"
        holder.tvMapel.text = jadwal.mataPelajaran

        holder.btnEdit.setOnClickListener { onEdit?.invoke(jadwal) }
        holder.btnDelete.setOnClickListener { onDelete?.invoke(jadwal) }
    }

    override fun getItemCount(): Int = listJadwal.size
}

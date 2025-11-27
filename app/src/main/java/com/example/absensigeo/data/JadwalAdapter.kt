package com.example.absensigeo.data

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.R

class JadwalAdapter(
    private val listJadwal: List<JadwalGuru>,
) : RecyclerView.Adapter<JadwalAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHari: TextView = view.findViewById(R.id.tvHari)
        val tvKelas: TextView = view.findViewById(R.id.tvKelas)
        val tvJam: TextView = view.findViewById(R.id.tvJam)
        val tvMapel: TextView = view.findViewById(R.id.tvMapel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.jadwal_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listJadwal.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listJadwal[position]
        holder.tvHari.text = "Hari: ${item.hari}"
        holder.tvKelas.text = item.kelas
        holder.tvJam.text = "${item.jamMulai} - ${item.jamSelesai}"
        holder.tvMapel.text = item.mataPelajaran
    }
}

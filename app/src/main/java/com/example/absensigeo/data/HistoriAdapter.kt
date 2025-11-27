package com.example.absensigeo.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.R
import java.text.SimpleDateFormat
import java.util.*

class HistoriAdapter(private val list: List<BuktiMengajar>) :
    RecyclerView.Adapter<HistoriAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvNamaFile: TextView = view.findViewById(R.id.tvNamaFile)
        val tvMateri: TextView = view.findViewById(R.id.tvMateri) ?: throw NullPointerException("tvMateri not found")
        val tvDeskripsi: TextView = view.findViewById(R.id.tvDeskripsi) ?: throw NullPointerException("tvDeskripsi not found")
        val btnLihat: Button = view.findViewById(R.id.btnLihat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_histori_upload, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        Log.d("HistoriAdapter", "onBindViewHolder: materi=${item.materi}, deskripsi=${item.deskripsi}")

        holder.tvMateri.text = "Materi: ${item.materi}"
        holder.tvDeskripsi.text = "Deskripsi: ${item.deskripsi}"
        holder.tvTanggal.text = item.tanggal
        holder.tvNamaFile.text = item.nama_file

        holder.btnLihat.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
            holder.itemView.context.startActivity(intent)
        }
    }

}

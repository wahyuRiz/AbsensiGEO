package com.example.absensigeo.data

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.R

class TemplateAdapter(
    private val listTemplate: List<TemplateSuratModel>
) : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {

    inner class TemplateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJudul: TextView = itemView.findViewById(R.id.tvJudulTemplate)
        val btnDownload: Button = itemView.findViewById(R.id.btnDownloadTemplate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_template_surat, parent, false)
        return TemplateViewHolder(view)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        val template = listTemplate[position]
        holder.tvJudul.text = template.judul

        holder.btnDownload.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(template.url)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = listTemplate.size
}

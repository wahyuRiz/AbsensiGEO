package com.example.absensigeo.data

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.absensigeo.R

class DayAdapter(
    private val days: List<String>,
    private val currentDayIndex: Int
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tv_day)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.tvDay.text = day

        val context = holder.tvDay.context
        if (position == currentDayIndex) {
            holder.tvDay.background = ContextCompat.getDrawable(context, R.drawable.bg_day_active)
            holder.tvDay.setTextColor(Color.WHITE)
        } else {
            holder.tvDay.background = ContextCompat.getDrawable(context, R.drawable.bg_day_normal)
            holder.tvDay.setTextColor(Color.parseColor("#374151"))
        }
    }

    override fun getItemCount(): Int = days.size
}

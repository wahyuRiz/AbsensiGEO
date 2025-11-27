package com.example.absensigeo.fragment

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import biweekly.Biweekly
import com.example.absensigeo.R
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.core.DayPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

/**
 * Fragment yang menampilkan kalender dengan event-event dari sumber eksternal.
 *
 * Fitur utama:
 * - Menampilkan kalender bulanan dengan tampilan 12 bulan ke belakang dan depan
 * - Menyinkronkan event dari file ICS (iCalendar)
 * - Menandai tanggal-tanggal penting dan hari libur
 * - Menampilkan detail event ketika tanggal diklik
 */
class CalendarFragment : Fragment() {

    // Komponen UI
    private lateinit var calendarView: CalendarView
    private lateinit var eventText: TextView
    private lateinit var monthYearText: TextView

    // Map untuk menyimpan event berdasarkan tanggal
    private val eventMap = mutableMapOf<LocalDate, MutableList<String>>()

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
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        calendarView = view.findViewById(R.id.calendarView)
        eventText = view.findViewById(R.id.eventText)
        monthYearText = view.findViewById(R.id.monthYearText)
        return view
    }

    /**
     * Memperbarui judul bulan dan tahun yang ditampilkan
     *
     * @param month YearMonth yang akan ditampilkan
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateMonthYearTitle(month: YearMonth) {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id"))
        monthYearText.text = month.format(formatter)
    }

    /**
     * Dipanggil setelah tampilan dibuat, menginisialisasi kalender dan komponen lainnya
     *
     * @param view View yang telah dibuat
     * @param savedInstanceState Bundle yang menyimpan state sebelumnya
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val currentMonth = YearMonth.now()

        // Setup tampilan kalender
        calendarView.dayViewResource = R.layout.item_calendar_day
        setupDayHeaders(view)

        // Konfigurasi range bulan yang ditampilkan (12 bulan ke belakang dan depan)
        calendarView.setup(
            currentMonth.minusMonths(12),
            currentMonth.plusMonths(12),
            DayOfWeek.MONDAY
        )
        calendarView.scrollToMonth(currentMonth)

        // Listener untuk scroll bulan
        calendarView.monthScrollListener = { month ->
            updateMonthYearTitle(month.yearMonth)
        }

        updateMonthYearTitle(currentMonth)

        // Binder untuk menangani tampilan setiap hari di kalender
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            /**
             * Membuat container untuk tampilan hari
             */
            override fun create(view: View) = DayViewContainer(view)

            /**
             * Mengikat data ke tampilan hari
             *
             * @param container View container untuk hari
             * @param day Data hari yang akan ditampilkan
             */
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                val isCurrentMonth = day.position == DayPosition.MonthDate

                // Highlight hari ini
                if (day.date == LocalDate.now()) {
                    container.textView.setBackgroundResource(R.drawable.today_circle)
                    container.textView.setTextColor(Color.WHITE)
                } else {
                    container.textView.background = null
                    container.textView.setTextColor(getTextColorPrimary())
                }

                // Tampilkan angka hari hanya untuk bulan saat ini
                container.textView.text = if (isCurrentMonth) {
                    day.date.dayOfMonth.toString()
                } else {
                    ""
                }

                // Tampilkan dot indicator jika ada event
                container.dotView.visibility =
                    if (isCurrentMonth && eventMap.containsKey(day.date)) View.VISIBLE else View.GONE

                // Handle klik pada hari
                container.view.setOnClickListener {
                    if (isCurrentMonth) {
                        val events = eventMap[day.date]
                        eventText.text =
                            events?.joinToString("\n") ?: "Tidak ada event pada tanggal ini"
                    }
                }
            }
        }

        // Ambil data event dari ICS
        fetchICSData()
    }

    /**
     * Mendapatkan warna teks primary dari tema
     *
     * @return Warna teks primary
     */
    private fun getTextColorPrimary(): Int {
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        return ContextCompat.getColor(requireContext(), typedValue.resourceId)
    }

    /**
     * Menyiapkan header hari (Senin, Selasa, dst) di atas kalender
     *
     * @param view View parent
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDayHeaders(view: View) {
        val dayHeaderLayout = view.findViewById<LinearLayout>(R.id.dayHeaderLayout)
        val daysOfWeek = DayOfWeek.values().toList()

        // Buat TextView untuk setiap hari
        for (day in daysOfWeek) {
            val textView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = day.getDisplayName(TextStyle.SHORT, Locale("id"))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(getTextColorPrimary())
                textSize = 14f
            }
            dayHeaderLayout.addView(textView)
        }
    }

    /**
     * Mengambil data event dari file ICS (iCalendar) dari berbagai sumber
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchICSData() {
        val urls = listOf(
            "https://calendar.google.com/calendar/ical/en.indonesian%23holiday%40group.v.calendar.google.com/public/basic.ics",
            "https://calendar.google.com/calendar/ical/en.islamic%23holiday%40group.v.calendar.google.com/public/basic.ics",
        )

        CoroutineScope(Dispatchers.IO).launch {
            val errors = mutableListOf<String>()

            urls.forEach { icsUrl ->
                try {
                    val stream = URL(icsUrl).openStream()
                    val calendar = Biweekly.parse(stream).first()
                    val events = calendar.events

                    // Proses setiap event dan simpan ke map
                    for (event in events) {
                        val date = event.dateStart?.value?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                        if (date != null) {
                            val summary = event.summary?.value ?: "Event Tidak Diketahui"
                            eventMap.getOrPut(date) { mutableListOf() }.add(summary)
                        }
                    }

                } catch (e: Exception) {
                    errors.add("Gagal load dari $icsUrl: ${e.message}")
                }
            }

            // Update UI di thread utama
            withContext(Dispatchers.Main) {
                if (errors.isEmpty()) {
                    eventText.text = "Klik tanggal untuk melihat semua event"
                } else {
                    eventText.text = "Beberapa ICS gagal dimuat:\n" + errors.joinToString("\n")
                }

                // Perbarui tampilan kalender
                calendarView.notifyCalendarChanged()
            }
        }
    }

    /**
     * Container untuk tampilan hari di kalender
     *
     * @property textView TextView untuk menampilkan angka hari
     * @property dotView View untuk indikator event
     */
    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.dayText)
        val dotView: View = view.findViewById(R.id.eventDot)
    }
}
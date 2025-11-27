package com.example.absensigeo.data

sealed class AbsensiDisplayItem {
    data class Header(val tanggal: String) : AbsensiDisplayItem()
    data class Item(val absensi: Absensi) : AbsensiDisplayItem()
}

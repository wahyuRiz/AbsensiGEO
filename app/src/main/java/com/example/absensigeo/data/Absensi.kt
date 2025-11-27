package com.example.absensigeo.data

data class Absensi(
    val waktu: Long = 0L,
    val jenis: String = "",
    val keterangan: String = "",
    val alasan: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val name: String? = null,
    val fotoUrl: String? = null,
    val suratUrl: String? = null
)


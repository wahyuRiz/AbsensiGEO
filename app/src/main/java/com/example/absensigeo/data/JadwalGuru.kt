package com.example.absensigeo.data

import com.google.firebase.firestore.Exclude

data class JadwalGuru(
    var namaGuru: String = "",
    var kelas: String = "",
    var hari: String = "",
    var jamMulai: String = "",
    var jamSelesai: String = "",
    var mataPelajaran: String = ""
) {
    @get:Exclude
    var id: String = ""
}




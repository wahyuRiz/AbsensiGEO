package com.example.absensigeo.data

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ImgurApi {
    @Multipart
    @POST("image")
    fun uploadImage(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part
    ): Call<ImgurResponse>

    companion object {
        fun create(): ImgurApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.imgur.com/3/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(OkHttpClient())
                .build()

            return retrofit.create(ImgurApi::class.java)
        }
    }
}

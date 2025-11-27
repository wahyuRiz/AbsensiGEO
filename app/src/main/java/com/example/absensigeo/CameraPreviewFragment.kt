package com.example.absensigeo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreviewFragment : Fragment() {

    private lateinit var takePictureButton: Button
    private lateinit var previewView: PreviewView
    private lateinit var progressBar: ProgressBar

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var outputDirectory: File
    private lateinit var storage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera_preview, container, false)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1002)
        }

        progressBar = view.findViewById(R.id.progressBar)
        previewView = view.findViewById(R.id.previewView)
        takePictureButton = view.findViewById(R.id.btn_shutter)

        storage = FirebaseStorage.getInstance()
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        takePictureButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            takePhoto()
        }

        return view
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraPreview", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            "${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraPreview", "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(requireContext(), "Gagal ambil gambar", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }

                @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val userId = arguments?.getString("userId")
                    val absenId = arguments?.getString("absenId")

                    if (userId == null || absenId == null) {
                        progressBar.visibility = View.GONE
                        return
                    }

                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location ->
                            val lat = location?.latitude
                            val lon = location?.longitude
                            val compressedFile = compressAndResizeImage(photoFile, lat, lon)

                            uploadToImgur(compressedFile,
                                onSuccess = { imgurUrl ->
                                    val dbRef = FirebaseDatabase.getInstance("https://appabsensigeo-default-rtdb.asia-southeast1.firebasedatabase.app")
                                        .getReference("Absensi").child(userId).child(absenId)

                                    dbRef.child("fotoUrl").setValue(imgurUrl)
                                        .addOnSuccessListener {
                                            progressBar.visibility = View.GONE
                                            findNavController().navigate(R.id.action_cameraPreviewFragment_to_absensiFragment)
                                        }
                                        .addOnFailureListener {
                                            progressBar.visibility = View.GONE
                                        }
                                },
                                onFailure = {
                                    progressBar.visibility = View.GONE
                                }
                            )
                        }
                        .addOnFailureListener {
                            val compressedFile = compressAndResizeImage(photoFile, null, null)
                            uploadToImgur(compressedFile,
                                onSuccess = { /* handle */ },
                                onFailure = { /* handle */ })
                        }
                }
            }
        )
    }

    private fun compressAndResizeImage(file: File, latitude: Double?, longitude: Double?): File {
        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 56f
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(1f, 1f, 1f, Color.BLACK)
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("$timestamp", 20f, mutableBitmap.height - 40f, paint)

        if (latitude != null && longitude != null) {
            val koordinat = "Location: %.6f, %.6f".format(latitude, longitude)
            canvas.drawText(koordinat, 20f, mutableBitmap.height - 90f, paint)
        }

        val maxWidth = 800
        val resizedBitmap = if (mutableBitmap.width > maxWidth) {
            val aspectRatio = mutableBitmap.height.toFloat() / mutableBitmap.width
            val newHeight = (maxWidth * aspectRatio).toInt()
            Bitmap.createScaledBitmap(mutableBitmap, maxWidth, newHeight, true)
        } else {
            mutableBitmap
        }

        val compressedFile = File(requireContext().cacheDir, "compressed_${file.name}")
        FileOutputStream(compressedFile).use { out ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, out)
        }

        return compressedFile
    }

    private fun uploadToImgur(imageFile: File, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("https://api.imgur.com/3/image")
            .addHeader("Authorization", "Client-ID 63b004fa178d2b4")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread { onFailure(e.message ?: "Unknown error") }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val imageUrl = jsonObject.getJSONObject("data").getString("link")
                        requireActivity().runOnUiThread { onSuccess(imageUrl) }
                    } catch (e: Exception) {
                        requireActivity().runOnUiThread { onFailure("JSON parsing error") }
                    }
                } else {
                    requireActivity().runOnUiThread { onFailure("Upload failed: ${response.message}") }
                }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else if (requestCode == 1002 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Izin lokasi diberikan", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Izin diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireContext().externalMediaDirs.firstOrNull()?.let {
            File(it, "absensigeo").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else requireContext().filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
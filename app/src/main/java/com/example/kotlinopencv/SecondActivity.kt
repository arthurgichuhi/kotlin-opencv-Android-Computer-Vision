package com.example.kotlinopencv

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.kotlinopencv.databinding.ActivitySecondBinding
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class SecondActivity : AppCompatActivity(),HandLandMarkHelper.LandmarkerListener {
    private lateinit var binding: ActivitySecondBinding
    private var imageAnalyzer:ImageAnalysis?=null
    private lateinit var cameraExecutor:ExecutorService
    private lateinit var previewCamera:Preview

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mpImageView.visibility= View.INVISIBLE
        binding.mpCamera.visibility=View.VISIBLE
        setListeners()
        lifecycleScope.launch {
            cameraExecutor= Executors.newSingleThreadExecutor()
            cameraProviderStart()
        }
    }

    private fun setListeners(){
        binding.mpBtn.setOnClickListener{
            if(binding.mpImageView.isVisible){
                binding.mpImageView.visibility=View.INVISIBLE
                binding.mpCamera.visibility=View.VISIBLE
            }
            else{
                binding.mpImageView.visibility=View.VISIBLE
                binding.mpCamera.visibility=View.INVISIBLE
            }
        }
    }

    private suspend fun cameraProviderStart(){
        val cameraProvider= ProcessCameraProvider.getInstance(this).await()
        val preview= Preview.Builder().build()
        preview.setSurfaceProvider(binding.mpCamera.surfaceProvider)
        val cameraSelector= CameraSelector.DEFAULT_FRONT_CAMERA
        imageAnalyzer=ImageAnalysis.Builder().setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888).build()
        imageAnalyzer?.setAnalyzer(cameraExecutor,ImageAnalysis.Analyzer { image->

        })
        try{
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this,cameraSelector,preview,imageAnalyzer)
        } catch (e:Exception){

        }

    }

    override fun onResults(resultBundle: HandLandMarkHelper.ResultBundle) {
        Log.d("ResultBundle","${resultBundle}")
        TODO("Not yet implemented")
    }
    external fun stringFromJNI(): String
    //grayscale image
    private external fun grayScaleImage(src: Bitmap, outPut: Bitmap)

}
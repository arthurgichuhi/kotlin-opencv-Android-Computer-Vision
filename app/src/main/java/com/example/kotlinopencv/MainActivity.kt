package com.example.kotlinopencv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.kotlinopencv.databinding.ActivityMainBinding
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

open class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    //permission activity launcher
    private lateinit var permissionLauncher:ActivityResultLauncher<Array<String>>
    //if using camera controller
    //private lateinit var  cameraController:LifecycleCameraController
    //if using camera provider
    private var imageCapture:ImageCapture?=null
    //Image analysis
    private var imageAnalyzer:ImageAnalysis?=null
    private  lateinit var cameraExecutor:ExecutorService
    //
    private lateinit var backgroundExecutor: ExecutorService

    private lateinit var imageProcessingExecuter:ExecutorService
    //bitmaps
    private lateinit var previewView: PreviewView
    private lateinit var grayView:ImageView
    //
    private lateinit var bitmap:Bitmap
//    private lateinit var bmpGrayScale:Bitmap
    //hand land marks
    var minHandDetectionConfidence: Float = 0.5F
    var minHandTrackingConfidence: Float = 0.5F
    var minHandPresenceConfidence: Float = 0.5F
    var maxNumHands: Int = 1
    var currentDelegate: Int = 0
    var runningMode: RunningMode = RunningMode.LIVE_STREAM
    val context: Context=this
    var handLandmarker: HandLandmarker? = null
    // this listener is only used when running in RunningMode.LIVE_STREAM
    val handLandmarkerHelperListener: LandmarkerListener? = null
    var x:Float=0.0F
    var y:Float=0.0F
    var imageHeight:Int=0
    var imageWidth:Int=0
    var lx= mutableListOf<Double>()
    var ly= mutableListOf<Double>()
    private lateinit var opencvMat:Mat
    var game:SnakeGame=SnakeGame()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //hiding gray preview
        binding.grayPreview.visibility=View.INVISIBLE
        binding.viewFinder.visibility=View.VISIBLE
        //
        previewView=binding.viewFinder
        grayView=binding.grayPreview
        //permission launcher
        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        }
        //load opencv library
        try{
            if(OpenCVLoader.initDebug()) {
                Log.d("OPENCV-4", "Success loadig opencv")
                opencvMat=Mat()
            }
        }
        catch(e:Exception){
            Log.d("OPENCV-4","Failed to load opencv $e")
        }


        //set button listeners
        setListeners()
        //request camera permission
        requestPermissions()
        //setup handland mark
        backgroundExecutor = Executors.newSingleThreadExecutor()
        backgroundExecutor.execute{
            setupHandLandmarker()
        }
    }

    override fun onResume() {
        super.onResume()
        //set button listeners
        setListeners()
        //request camera permission
        requestPermissions()

    }

    //button listeners
    private fun setListeners(){

        val grayPreview=binding.grayPreview
        val colorPreview=binding.viewFinder
        binding.switchBtn.setOnClickListener {
            Log.d("Switch-Button","Is gray scale image ${grayPreview.isVisible}")
           if(grayPreview.isVisible){
               grayPreview.visibility= View.INVISIBLE
               colorPreview.visibility=View.VISIBLE
           }
            else{
                grayPreview.visibility=View.VISIBLE
               colorPreview.visibility=View.INVISIBLE
           }
        }

        binding.switchScreenBtn.setOnClickListener {
            val intent=Intent(this,SecondActivity::class.java)
            startActivity(intent)
        }
    }
    //setup media pipe hand land marks
    private fun setupHandLandmarker() {
        // Set general hand landmarker options
        val baseOptionBuilder = BaseOptions.builder()

        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            currentDelegate -> {
                baseOptionBuilder.setDelegate(Delegate.CPU)
            }
            currentDelegate -> {
                baseOptionBuilder.setDelegate(Delegate.GPU)
            }
        }

        baseOptionBuilder.setModelAssetPath("hand_landmarker.task")

        // Check if runningMode is consistent with handLandmarkerHelperListener
        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                if (handLandmarkerHelperListener == null) {
//                    throw IllegalStateException(
//                        "handLandmarkerHelperListener must be set when runningMode is LIVE_STREAM."
//                    )
                }
            }
            else -> {
                // no-op
            }
        }

        try {
            val baseOptions = baseOptionBuilder.build()
            // Create an option builder with base options and specific
            // options only use for Hand Landmarker.
            val optionsBuilder =
                HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinHandDetectionConfidence(minHandDetectionConfidence)
                    .setMinTrackingConfidence(minHandTrackingConfidence)
                    .setMinHandPresenceConfidence(minHandPresenceConfidence)
                    .setNumHands(maxNumHands)
                    .setRunningMode(runningMode)

            // The ResultListener and ErrorListener only use for LIVE_STREAM mode.
            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }

            val options = optionsBuilder.build()
            handLandmarker =
                HandLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            handLandmarkerHelperListener?.onError(
                "Hand Landmarker failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                "HandLandMark", "MediaPipe failed to load the task with error: " + e
                    .message
            )
        } catch (e: RuntimeException) {
            // This occurs if the model being used does not support GPU
            handLandmarkerHelperListener?.onError(
                "Hand Landmarker failed to initialize. See error logs for " +
                        "details",
            )
            Log.e(
                "HandLandMarker",
                "Image classifier failed to load model with error: " + e.message
            )
        }
    }


    private fun returnLivestreamResult(
        result: HandLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        handLandmarkerHelperListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )
        if(result.landmarks().isNotEmpty()){
            Log.d("LiveStreamResult","Got livestream result ${result.landmarks()[0][8]}")
            x=result.landmarks()[0][8].x()
            y=result.landmarks()[0][8].y()
            lx.add(x.toDouble()*imageHeight)
            ly.add(y.toDouble()*imageWidth)
        }
    }


    private fun returnLivestreamError(error: RuntimeException) {
        handLandmarkerHelperListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    fun detectLiveStream(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean
    ) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException(
                "Attempting to call detectLiveStream" +
                        " while not using RunningMode.LIVE_STREAM"
            )
        }
        val frameTime = SystemClock.uptimeMillis()

        // Copy out RGB bits from the frame to a bitmap buffer
         bitmap =convertToBitMap(imageProxy)

        //imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        //imageProxy.close()

        val matrix = Matrix().apply {
            // Rotate the frame received from the camera to be in the same direction as it'll be shown
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            // flip image if user use front camera
            if (isFrontCamera) {
                postScale(
                    -1f,
                    1f,
                    imageProxy.width.toFloat(),
                    imageProxy.height.toFloat()
                )
            }
        }
        val rotatedBitMap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            matrix, true
        )

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(rotatedBitMap).build()
        detectAsync(mpImage, frameTime)
    }

    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        handLandmarker?.detectAsync(mpImage, frameTime)
        // As we're using running mode LIVE_STREAM, the landmark result will
        // be returned in returnLivestreamResult function
    }


    //request permissions
    private fun requestPermissions(){
        val grantedCamera:Boolean=ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
        var permissionRequest= arrayListOf<String>()
        if(!grantedCamera){
            permissionRequest.add(android.Manifest.permission.CAMERA)
        }
        val grantedStorage:Boolean=ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED
        if(!grantedStorage){
            permissionRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if(!grantedCamera || !grantedStorage){
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }
        lifecycleScope.launch {
            cameraExecutor=Executors.newSingleThreadExecutor()
            cameraProviderStart()
        }
//        cameraStart()
    }
    private suspend fun cameraProviderStart(){
        val cameraProvider=ProcessCameraProvider.getInstance(this).await()
        val preview=Preview.Builder().build()
        imageAnalyzer=ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
//            .setTargetRotation(cameraProvider.ROTATION_0)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888).build()
        imageAnalyzer?.setAnalyzer(cameraExecutor,ImageAnalysis.Analyzer { image ->
            imageHeight=image.height
            imageWidth=image.width
            detectLiveStream(image,true)
            var bmpGrayScale=Bitmap.createBitmap(bitmap.width,bitmap.height,Bitmap.Config.ARGB_8888)
            Utils.bitmapToMat(bitmap,opencvMat)
            Core.flip(opencvMat,opencvMat,1)
            Core.flip(opencvMat,opencvMat,0)
            try{
                game.gameFunction(opencvMat,org.opencv.core.Point(x.toDouble(),y.toDouble()),bitmap.height,bitmap.width)
                Imgproc.circle(opencvMat,org.opencv.core.Point(game.foodPoint.y*bitmap.width,game.foodPoint.x*bitmap.height),
                    10,Scalar(0.0,255.0,255.0),10)
            }catch(e:Exception){
                Log.d("GameError","$e")
            }
            Core.flip(opencvMat,opencvMat,0)
            Utils.matToBitmap(opencvMat,bmpGrayScale)
            val matrix = Matrix()

            //grayScaleImage(bitmap,bmpGrayScale,x,y,bitmap.width,bitmap.height)
            image.close()
            matrix.postRotate(90f)
            var rotatedBitmap=Bitmap.createBitmap(bmpGrayScale,0,0,imageWidth,imageHeight,matrix,false)

            runOnUiThread {

                binding.grayPreview.setImageBitmap(rotatedBitmap)
            }
        })
        preview.setSurfaceProvider(previewView.surfaceProvider)
        imageCapture=ImageCapture.Builder().build()
        val cameraSelector=CameraSelector.DEFAULT_FRONT_CAMERA
        try{
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this,cameraSelector,preview,imageAnalyzer)
        } catch (e:Exception){

        }
    }
    private fun convertToBitMap(image:ImageProxy):Bitmap{
        val yBuffer = image.planes[0].buffer
        val vuBuffer = image.planes[2].buffer

        val ySize=yBuffer.remaining()
        val vuSize=vuBuffer.remaining()

        val nv21=ByteArray(ySize+vuSize)

        yBuffer.get(nv21,0,ySize)
        vuBuffer.get(nv21,ySize,vuSize)

        val yuvImage=YuvImage(nv21,ImageFormat.NV21,image.width,image.height,null)
        val out=ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0,0,yuvImage.width,yuvImage.height),50,out)
        val imageBytes=out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.size)
    }
//    private fun cameraStart(){
//        val previewView:PreviewView=binding.viewFinder
//        cameraController= LifecycleCameraController(baseContext)
//        cameraController.bindToLifecycle(this)
//        cameraController.cameraSelector=CameraSelector.DEFAULT_FRONT_CAMERA
//        previewView.controller=cameraController
//    }
    /**
     * A native method that is implemented by the 'shika' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
    //grayscale image
    private external fun grayScaleImage(src:Bitmap, outPut:Bitmap,x:Float,y:Float,width:Int,height:Int)
    //docScanner function
//    private external fun docScanner(src:Bitmap,outPut: Bitmap,w:Float,h:Float,cropValue:Int)
    private external fun docScanner(src:Bitmap,outPut: Bitmap)

    private external fun drawCircle(src: Bitmap,outPut: Bitmap,x:Float,y:Float)

    //snake game
    private external fun drawSnake(
        src:Bitmap,
        outPut: Bitmap,
        cx:Float,
        cy:Float, )
    companion object {
        // Used to load the 'kotlinopencv' library on application startup.
        init {
            System.loadLibrary("kotlinopencv")
        }
    }
}

    data class ResultBundle(
        val results: List<HandLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )
    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = HandLandMarkHelper.OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }
#include <jni.h>
#include <string>
#include "opencv-utils.h"
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include "android/bitmap.h"
#include <iostream>
//bitmap to Map
void bitmapToMat
        (JNIEnv * env, jobject bitmap, Mat& dst, jboolean needUnPremultiplyAlpha)
{
    AndroidBitmapInfo  info;
    void*              pixels = 0;

    try {
        CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        CV_Assert( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                   info.format == ANDROID_BITMAP_FORMAT_RGB_565 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );
        dst.create(info.height, info.width, CV_8UC4);
        if( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 )
        {
            Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if(needUnPremultiplyAlpha) cvtColor(tmp, dst, COLOR_mRGBA2RGBA);
            else tmp.copyTo(dst);
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            cvtColor(tmp, dst, COLOR_BGR5652RGBA);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch(const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);

        jclass je = env->FindClass("java/lang/Exception");
        if(!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);

        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nBitmapToMat}");
        return;
    }
}

//Mat to bitmap
void MatToBitmap2
        (JNIEnv * env, Mat src, jobject bitmap, jboolean needPremultiplyAlpha)
{
    AndroidBitmapInfo  info;
    void*              pixels = 0;

    try {
        CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        CV_Assert( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                   info.format == ANDROID_BITMAP_FORMAT_RGB_565 );
        CV_Assert( src.dims == 2 && info.height == (uint32_t)src.rows && info.width == (uint32_t)src.cols );
        CV_Assert( src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );
        if( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 )
        {
            Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if(src.type() == CV_8UC1)
            {
                cvtColor(src, tmp, COLOR_GRAY2RGBA);
            } else if(src.type() == CV_8UC3){
                cvtColor(src, tmp, COLOR_RGB2RGBA);
            } else if(src.type() == CV_8UC4){

                if(needPremultiplyAlpha) cvtColor(src, tmp, COLOR_RGBA2mRGBA);
                else src.copyTo(tmp);
            }
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if(src.type() == CV_8UC1)
            {
                cvtColor(src, tmp, COLOR_GRAY2BGR565);
            } else if(src.type() == CV_8UC3){
                cvtColor(src, tmp, COLOR_RGB2BGR565);
            } else if(src.type() == CV_8UC4){
                cvtColor(src, tmp, COLOR_RGBA2BGR565);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch(const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        if(!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return;
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_kotlinopencv_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI()
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_kotlinopencv_MainActivity_grayScaleImage(JNIEnv *env, jobject thiz, jobject inputBitmap,jobject outPut,jfloat x, jfloat y,
                                                          jint width, jint height) {
    Mat src;
    bitmapToMat(env,inputBitmap,src,false);
    grayScaleImage(src,x,y,width,height);
    MatToBitmap2(env,src,outPut,false);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_kotlinopencv_MainActivity_docScanner(JNIEnv *env,jobject thiz,jobject inputBitmap,jobject outPutbitmap){
    Mat src;
    bitmapToMat(env,inputBitmap,src,false);
    docScanner(src,490,596,5);
    MatToBitmap2(env,src,outPutbitmap,false);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_kotlinopencv_SecondActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI()
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_kotlinopencv_SecondActivity_grayScaleImage(JNIEnv *env, jobject thiz, jobject inputBitmap,jobject outPutbitmap) {
    Mat src2;
    bitmapToMat(env,inputBitmap,src2,false);
    //grayScaleImage(src2,x,y);
    MatToBitmap2(env,src2,outPutbitmap,false);
}




extern "C"
JNIEXPORT void JNICALL
Java_com_example_kotlinopencv_MainActivity_drawCircle(JNIEnv *env, jobject thiz, jobject inputBitmap,
                                                      jobject out_put, jfloat x, jfloat y) {
    Mat src2;
    bitmapToMat(env,inputBitmap,src2,false);
    drawCircle(src2,x,y);
    MatToBitmap2(env,src2,out_put,false);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_example_kotlinopencv_MainActivity_drawSnake(JNIEnv *env, jobject thiz, jobject src,
                                                     jobject out_put, jfloat cx, jfloat cy
                                                     ){

    Mat src2;
    bitmapToMat(env,src,src2,false);
    drawPointer(src2,cx,cy);
    MatToBitmap2(env,src2,out_put,false);

}
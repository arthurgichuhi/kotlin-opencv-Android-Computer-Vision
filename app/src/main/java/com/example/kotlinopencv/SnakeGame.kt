package com.example.kotlinopencv

import android.graphics.Point
import android.util.Log
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.FILLED
import org.opencv.imgproc.Imgproc.circle
import org.opencv.imgproc.Imgproc.pointPolygonTest
import org.opencv.imgproc.Imgproc.polylines
import org.opencv.imgproc.Imgproc.putText
import java.util.ArrayList
import kotlin.random.Random
import kotlin.math.hypot
import kotlin.math.roundToInt

class SnakeGame() {
    var points= mutableListOf<org.opencv.core.Point>()
    var lengths= mutableListOf<Double>()
    var currentLength:Double=0.0
    var allowedLength:Int=0
    var previousHead:org.opencv.core.Point=org.opencv.core.Point()
    var foodPoint:org.opencv.core.Point=org.opencv.core.Point()
    var score:Int=0
    var gameOver:Boolean=false
    var wFood:Double=0.035
    var hFood:Double=0.035
    val polygonPoints= mutableListOf<org.opencv.core.Point>()
    //var polyLinePoints= mutableListOf<>()
    fun randomFoodLocation(){
        val x=Random.nextDouble(0.15,0.85)
        val y=Random.nextDouble(0.15,0.85)
        foodPoint=org.opencv.core.Point(x,y)

    }

    fun gameFunction(imgMain:Mat,currentHead:org.opencv.core.Point,screenHeight: Int,screenWidth: Int){
        if(gameOver){
            points= mutableListOf<org.opencv.core.Point>()
            lengths= mutableListOf<Double>()
            currentLength=0.0
            previousHead=org.opencv.core.Point()
            score=0
        }
        else{
            var px=previousHead.x
            var py=previousHead.y
            var cx:Double=currentHead.x
            var cy:Double=currentHead.y
            if(cx>0){
                points.add(org.opencv.core.Point(cx,cy))
                if(points.size==allowedLength){
                    points.removeAt(0)
                    lengths.removeAt(0)
                    if(polygonPoints.isNotEmpty()){
                        polygonPoints.removeAt(0)
                    }
                }
            }
            var distance= hypot((cx-px), (cy-py))

            currentLength+=(distance*10)

            lengths.add(distance*10)
            previousHead=org.opencv.core.Point(cx,cy)
            if (currentLength>allowedLength){
                lengths.forEachIndexed { index, length ->
                    currentLength-=length
                    if(currentLength<allowedLength){
                        return
                    }}
            }
            var rx=foodPoint.x
            var ry=foodPoint.y
            if (rx-wFood<cx && cx<rx+wFood && ry-hFood<cy && cy<ry+hFood){
                randomFoodLocation()
                if(allowedLength<=50){
                    allowedLength+=5
                }
                score+=1
            }

        }

        points.reversed().forEachIndexed { index, point ->
            if(index>1 && index<allowedLength){
                Imgproc.line(imgMain,org.opencv.core.Point(points[index-1].y*screenWidth,points[index-1].x*screenHeight),
                    org.opencv.core.Point(points[index-2].y*screenWidth,points[index-2].x*screenHeight),
                    Scalar(0.0),10
                )
                if((index%2)==1){
                    circle(imgMain,point,5, Scalar(255.0,0.0,0.0,255.0))
                }
            }
        }
        Imgproc.circle(imgMain,org.opencv.core.Point(currentHead.y*screenWidth,currentHead.x*screenHeight)
            ,10,Scalar(0.0,0.0,255.0),FILLED)

        points.reversed().forEachIndexed{index, point ->
            if(index>2){
                //polygonPoints.add(org.opencv.core.Point(point.x,point.y))
                if((point.x+0.3)<=currentHead.x){
                    gameOver=true
                }
            }
        }
        //check for collisions
//        if(polygonPoints.isNotEmpty()){
//            var polygon=MatOfPoint2f()
//            polygon.fromList(polygonPoints.toList())
//            //polylines(imgMain,polygon,false,Scalar(0.0,0.0,0.0),5,5)
//            var minDistance= pointPolygonTest(polygon,
//                org.opencv.core.Point(currentHead.x,currentHead.y),true)
//            if(minDistance in 0.0001..0.0009){
//            Log.d("Polygon-Distance","*$minDistance ")
//            }
//        }

        
    }

}

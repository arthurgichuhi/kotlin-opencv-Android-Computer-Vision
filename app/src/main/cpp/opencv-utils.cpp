//
// Created by gichuhi on 14/09/2023.
//
#include "opencv-utils.h"
#include <opencv2/imgproc.hpp>
#include <iostream>
#include <vector>
#include "opencv2/core.hpp"
///
//


// snake Game function
void drawPointer(Mat src,float cx,float cy){
    flip(src, src, 1);
    flip(src, src, 0);
    circle(src, Point(cy,cx), 10, (200, 0, 200), FILLED);
    flip(src,src,0);
}

void grayScaleImage(Mat src,float x,float y,int width,int height){
    flip(src,src,1);
    flip(src,src,0);
    circle(src,Point(y*width,x*height),10,Scalar(0,69,255),FILLED);

    flip(src,src,0);
}
//draw points
void drawPoints(vector<Point> points, Scalar color, Mat img)
{
    for (int i = 0; i < points.size(); i++)
    {
        circle(img, points[i], 10, color, FILLED);
        putText(img, to_string(i), points[i], FONT_HERSHEY_PLAIN, 5, color, 5);
    }
}


//image pre-processing for doc scanner
Mat preProcessingImage(Mat img){
    Mat imgBlur,imgGray,imgCanny,imgDil;
    cvtColor(img,imgGray,COLOR_RGBA2GRAY);
    GaussianBlur(imgGray,imgBlur,Size(3,3),3,0);
    Canny(imgGray,imgCanny,25,75);
    Mat kernel=getStructuringElement(MORPH_RECT,Size(3,3));
    dilate(imgCanny,imgDil,kernel);
    return imgDil;
}
//get contours of pre-processed image
vector<Point>getContours(Mat mask){
    vector<vector<Point>>contours;
    vector<Vec4i>hierachy;
    findContours(mask,contours,hierachy,RETR_EXTERNAL,CHAIN_APPROX_SIMPLE);
    vector<vector<Point>>conPoly(contours.size());
    vector<Rect>boundRect(contours.size());
    vector<Point>biggest={{5,5},{mask.cols-5,5},{5,mask.rows-5},{mask.cols-5,mask.rows-5}};
    int maxArea=0;
    for(int i=0;i<contours.size();i++){
        int area=contourArea(contours[i]);
        if( area > 1000){
            float peri=arcLength(contours[i],true);
            approxPolyDP(contours[i],conPoly[i],peri*0.002,true);
            if(area>maxArea && conPoly[i].size()==4){
                maxArea=area;
                biggest={conPoly[i][0],conPoly[i][1],conPoly[i][2],conPoly[i][3]};
            }
        }
    }
    return biggest;
}
//reorder points
vector<Point>reorderPoints(vector<Point>points){
    vector<Point>newPoints;
    vector<int>sumPoints,subPoints;
    for(int i=0;i<points.size();i++){
        sumPoints.push_back(points[i].x+points[i].y);
        subPoints.push_back(points[i].x-points[i].y);
    }
    newPoints.push_back(points[min_element(sumPoints.begin(),sumPoints.end())-sumPoints.begin()]);
    newPoints.push_back(points[max_element(subPoints.begin(),subPoints.end())-subPoints.begin()]);
    newPoints.push_back(points[min_element(subPoints.begin(),subPoints.end())-subPoints.begin()]);
    newPoints.push_back(points[max_element(sumPoints.begin(),sumPoints.end())-sumPoints.begin()]);
    return newPoints;
}
//warp image
Mat getWarp(Mat imgSrc,vector<Point>points,float w,float h){
    Mat imgWarped;
    Point2f src[4]={{points[0]},{points[1]},{points[2]},{points[3]}};
    Point2f dst[4]={{0.0f,0.0f},{w,0.0f},{0.0f,h},{w,h}};
    Mat matrix=getPerspectiveTransform(src,dst);
    warpPerspective(imgSrc,imgWarped,matrix,Point(w,h));
    return imgWarped;
}
//main doc Scanner function
void docScanner(Mat src,float w,float h,int cropValue){
    vector<Point>initialPoints,docPoints;
    Mat imgThresh= preProcessingImage(src);
    initialPoints= getContours(imgThresh);
    docPoints= reorderPoints(initialPoints);
    Mat src2= getWarp(src,docPoints,w,h);
    Rect roi(cropValue,cropValue,w-(2*cropValue),h-(2*cropValue));
    src=src2(roi);
    flip(src,src,1);
    drawPoints(docPoints,(0,255,0),src);
}


void drawCircle(Mat src,float x,float y){
    circle(src, Point(x,y), 10, Scalar(255,255,0), FILLED);
}
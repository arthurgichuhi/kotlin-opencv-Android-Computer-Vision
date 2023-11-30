//
// Created by gichuhi on 14/09/2023.
//
#pragma once
#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>
#ifndef SHIKA_OPENCV_UTILS_H
#define SHIKA_OPENCV_UTILS_H

#endif //SHIKA_OPENCV_UTILS_H
using namespace cv;
using namespace std;

void grayScaleImage(Mat src,float x,float y,int width,int height);

void docScanner(Mat src,float w,float h,int cropValue);

void drawCircle(Mat src,float x,float y);

void drawPointer(Mat src,float cx,float cy);
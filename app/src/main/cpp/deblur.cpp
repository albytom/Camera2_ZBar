#include <jni.h>
#include <stdio.h>
#include <opencv2/opencv.hpp>
#include <opencv2/core.hpp>
#include <iostream>
#include <android/log.h>

#include <zbar.h>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <algorithm>
#include <vector>



extern "C" {

using namespace cv;
using namespace std;
using namespace zbar;

struct AreaCmp {
    AreaCmp(const vector<float> &_areas) : areas(&_areas) {}

    bool operator()(int a, int b) const { return (*areas)[a] > (*areas)[b]; }

    const vector<float> *areas;
};

JNIEXPORT jintArray JNICALL Java_com_example_android_camera2basic_CvUtil_processMat
        (JNIEnv *env, jclass obj, jlong addrRgba) {
    int kernel_size = 3;
    Mat sharpen, dst, gray, grad_x, grad_y, gradient, blurred, thresh, closed, M;
    vector<vector<Point>> cnts, biggest;
    vector<vector<Point>> c;

    cv::Mat *pMatRgb = (cv::Mat *) addrRgba;

    cv::Mat image = *pMatRgb;

    Mat image_out = image.clone();

    // Check for failure
    if (image.empty()) {
        cout << "Could not open or find the image" << endl;
        cin.get(); //wait for any key press
        //return -1;
    }

    //Defining the filter

    Mat sharpen_kernel = Mat::ones(kernel_size, kernel_size, CV_32F) * -1;
    sharpen_kernel.at<float>(1, 1) = 9;
    filter2D(image, sharpen, -1, sharpen_kernel);

    //__android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", "deblur after filtered");


    cv::fastNlMeansDenoisingColored(sharpen, dst, 7, 21);

    cv::cvtColor(dst, gray, cv::COLOR_RGB2GRAY);


    Sobel(gray, grad_x, CV_32F, 1, 0, 3);
    Sobel(gray, grad_y, CV_32F, 0, 1, 3);

    subtract(grad_x, grad_y, gradient);
    convertScaleAbs(gradient, gradient);


    blur(gradient, blurred, Size(3, 3));

    //GaussianBlur( gradient, blurred, Size(3,3), 1, 0 );
    threshold(gradient, thresh, 40, 255, cv::THRESH_BINARY);
    //imshow("gradIent",blurred);

    M = getStructuringElement(MORPH_RECT, Size(21, 7));
    morphologyEx(thresh, closed, MORPH_CLOSE, M);

    Mat er, dl;

    erode(closed, er, getStructuringElement(MORPH_RECT, Size(5, 5)));
    dilate(er, dl, getStructuringElement(MORPH_RECT, Size(5, 5)));
    erode(dl, er, getStructuringElement(MORPH_RECT, Size(5, 5)));
    dilate(er, dl, getStructuringElement(MORPH_RECT, Size(5, 5)));

    erode(dl, er, getStructuringElement(MORPH_RECT, Size(5, 5)));
    dilate(er, dl, getStructuringElement(MORPH_RECT, Size(5, 5)));
    erode(dl, er, getStructuringElement(MORPH_RECT, Size(5, 5)));
    dilate(er, dl, getStructuringElement(MORPH_RECT, Size(5, 5)));


    findContours(dl, cnts, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE, Point(0, 0));
    vector<vector<Point> > contours_poly(cnts.size());

    vector<Rect> boundRect(cnts.size());
    vector<Point2f> centers(cnts.size());
    vector<float> radius(cnts.size());
    vector<int> sortIdx(cnts.size());
    vector<float> areas(cnts.size());
    vector<cv::Vec4i> hierarchy;


    for (size_t i = 0; i < cnts.size(); i++) {
        sortIdx[i] = i;
        areas[i] = contourArea(cnts[i], false);
        approxPolyDP(cnts[i], contours_poly[i], 3, true);
        boundRect[i] = boundingRect(contours_poly[i]);
        minEnclosingCircle(contours_poly[i], centers[i], radius[i]);
    }

    std::sort(sortIdx.begin(), sortIdx.end(), AreaCmp(areas));


    int idx = sortIdx[0];

    rectangle(image_out, boundRect[idx].tl(), boundRect[idx].br(), cv::Scalar(0, 0, 255), 2);

    cout << boundRect[idx].tl();
    cout << boundRect[idx].br();
    std::vector<cv::Point> contour(2);

    contour[0] = boundRect[idx].tl();
    contour[1] = boundRect[idx].br();


    //__android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", "deblur before myROI");
    cv::Rect myROI(boundRect[idx].tl(), boundRect[idx].br());
    //cv::Mat croppedImage = image_out(myROI);
    //__android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", "deblur after myROI");
    image = image_out(myROI);
    //image = image_out;

    jintArray result;
    result = env -> NewIntArray(4);
    // fill a temp structure to use to populate the java int array
    jint fill[4];

    fill[0] = myROI.x; // put whatever logic you want to populate the values here.
    fill[1] = myROI.y;
    fill[2] = myROI.width;
    fill[3] = myROI.height;

    // move from the temp structure to the java structure
    env->SetIntArrayRegion(result, 0, 4, fill);


    //__android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", "deblur completed");
    return result;
}
typedef struct
{
    string type;
    string data;
    vector <Point> location;
}decodedObject;

void decode(Mat &im, vector<decodedObject>&decodedObjects)
{

    // Create zbar scanner
    ImageScanner scanner;


    // Configure scanner
    scanner.set_config(ZBAR_QRCODE, ZBAR_CFG_ENABLE, 1);


    // Convert image to grayscale
    Mat imGray;
    cvtColor(im, imGray,CV_BGR2GRAY);

    // Wrap image data in a zbar image
    Image image(im.cols, im.rows, "Y800", (uchar *)imGray.data, im.cols * im.rows);

    // Scan the image for barcodes and QRCodes
    int n = scanner.scan(image);


    for(Image::SymbolIterator symbol = image.symbol_begin(); symbol != image.symbol_end(); ++symbol)
    {
        decodedObject obj;

        obj.type = symbol->get_type_name();
        obj.data = symbol->get_data();

        // Print type and data
        cout << "Type : " << obj.type << endl;
        cout << "Data : " << obj.data << endl << endl;
        //__android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "Type : %s ", obj.type );
        //__android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "Data : %s", obj.data );
        decodedObjects.push_back(obj);
    }


}

JNIEXPORT jobjectArray JNICALL Java_com_example_android_camera2basic_CvUtil_processZbar
        (JNIEnv *env, jclass obj, jlong addrRgba) {
    cv::Mat *pMatRgb = (cv::Mat *) addrRgba;

    cv::Mat image = *pMatRgb;
    // Variable for decoded objects
    vector<decodedObject> decodedObjects;

    // Find and decode barcodes and QR codes
    decode(image, decodedObjects);


    if(!decodedObjects.empty()){
        int counter = 0;
        jobjectArray ret = (jobjectArray)env->NewObjectArray(decodedObjects.size()*2,env->FindClass("java/lang/String"),env->NewStringUTF(""));
        for(int i = 0; i <decodedObjects.size(); i++){
            decodedObject obj = decodedObjects[i];
            const char* str = obj.type.c_str(); //
            __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "Type: %s ", str );
            printf("%s\n", str);
            const char* strd = obj.data.c_str();
            __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "Data: %s ", strd );
            printf("%s\n", strd);
            env->SetObjectArrayElement(ret,counter,env->NewStringUTF(str));
            counter++;
            env->SetObjectArrayElement(ret,counter,env->NewStringUTF(strd));
            counter++;
        }
        return ret;
    }
    return NULL;
}
}
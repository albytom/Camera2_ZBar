LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_INSTALL_MODULES:=on
OPENCV_CAMERA_MODULES:=on
OPENCV_LIB_TYPE := STATIC

include $(LOCAL_PATH)/OpenCV.mk

LOCAL_MODULE := deblur-lib, localize-lib, trilateration-lib, pathplan-lib
LOCAL_SRC_FILES := C:/Users/Albert/Documents/Android_Studio_Projects/Warehouse/app/src/main/cpp/deblur.cpp, C:/Users/Albert/Documents/Android_Studio_Projects/Warehouse/app/src/main/cpp/localise.cpp, C:/Users/Albert/Documents/Android_Studio_Projects/Warehouse/app/src/main/cpp/trilateration.cpp, C:/Users/Albert/Documents/Android_Studio_Projects/Warehouse/app/src/main/cpp/beacon.cpp, C:/Users/Albert/Documents/Android_Studio_Projects/Warehouse/app/src/main/cpp/test_trilateration.cpp, C:/Users/Albert/Documents/Android_Studio_Projects/Warehouse/app/src/main/cpp/pathPlanning.cpp
LOCAL_C_INCLUDES := C:/Users/Albert/Documents/Android_Studio_Projects/BarCodeHub/OpenCV-android-sdk/sdk/native/jni/include
#LOCAL_LDLIBS += -lm -llog
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog

include $(BUILD_SHARED_LIBRARY)

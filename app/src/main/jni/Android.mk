LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_INSTALL_MODULES:=on
OPENCV_CAMERA_MODULES:=on
OPENCV_LIB_TYPE := STATIC

include $(LOCAL_PATH)/OpenCV.mk

LOCAL_MODULE := deblur-lib
LOCAL_SRC_FILES := /home/albert/StudioProjects/Camera2_ZBar/app/src/main/cpp/deblur.cpp
LOCAL_C_INCLUDES := /home/albert/StudioProjects/OpenCV-android-sdk/sdk/native/jni/include
#LOCAL_LDLIBS += -lm -llog
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog

include $(BUILD_SHARED_LIBRARY)

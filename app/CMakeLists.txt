# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

# Typically you don't care so much for a third party library's tests to be
# run from your own project's code.
set(JSON_BuildTests OFF CACHE INTERNAL "")

#Added 2 path definitions to support 20160825 additions
#set(pathToProject /home/albert/StudioProjects/Camera2_ZBar)
#set(pathToOpenCv /home/albert/StudioProjects/OpenCV-android-sdk)
#set(pathToZbar /home/albert/StudioProjects/android-zbar-sdk/zbar/src/main/jniLibs)
#For Windows
set(pathToProject C:/Users/AlberDominic/Documents/Android_StPro/BarcodeHub/Camera2_ZBar)
set(pathToOpenCv C:/Users/AlberDominic/Documents/Android_StPro/BarcodeHub/OpenCV-android-sdk)
set(pathToZbar C:/Users/AlberDominic/Documents/Android_StPro/BarcodeHub/android-zbar-sdk/zbar/src/main/jniLibs)


#Added by the IDE on project create
cmake_minimum_required(VERSION 3.4.1)

#Two sets suggested by Bruno Alexandre Krinski 20160825
set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

#Addition suggested by Bruno Alexandre Krinski 20160825
include_directories(${pathToOpenCv}/sdk/native/jni/include)

#Additional ZBar support
#include_directories(/home/albert/StudioProjects/ZBar/include)
#For Windows
include_directories(C:/Users/AlberDominic/Documents/Android_StPro/BarcodeHub/ZBar/include)

#Added by IDE on project create
add_library( deblur-lib SHARED src/main/cpp/deblur.cpp )

#Addition suggested by Bruno Alexandre Krinski 20160825
add_library( lib_opencv SHARED IMPORTED )

add_library( lib_zbar SHARED IMPORTED )
add_library( lib_iconv SHARED IMPORTED )


#Addition suggested by Bruno Alexandre Krinski 20160825
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)
set_target_properties(lib_zbar PROPERTIES IMPORTED_LOCATION ${pathToZbar}/${ANDROID_ABI}/libzbar.so)
set_target_properties(lib_iconv PROPERTIES IMPORTED_LOCATION ${pathToZbar}/${ANDROID_ABI}/libiconv.so)

#Added by IDE on project create
find_library( log-lib log )

#Added by IDE on project create, Removed and replace with additional parameter suggested by Bruno Alexandre Krinski 20160825
#target_link_libraries( native-lib $\{log-lib} )
target_link_libraries( deblur-lib ${log-lib} lib_opencv lib_zbar lib_iconv )





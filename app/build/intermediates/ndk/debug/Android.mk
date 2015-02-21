LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := directIO
LOCAL_LDLIBS := \
	-llog \

LOCAL_SRC_FILES := \
	/Users/elenalast/AndroidstudioProjects/SDPlay/app/src/main/jni/Android.mk \
	/Users/elenalast/AndroidstudioProjects/SDPlay/app/src/main/jni/directIO.c \

LOCAL_C_INCLUDES += /Users/elenalast/AndroidstudioProjects/SDPlay/app/src/main/jni
LOCAL_C_INCLUDES += /Users/elenalast/AndroidstudioProjects/SDPlay/app/src/debug/jni

include $(BUILD_SHARED_LIBRARY)

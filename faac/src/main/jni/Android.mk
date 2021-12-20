LOCAL_PATH := $(call my-dir)
FAAC_TOP := $(LOCAL_PATH)
include $(CLEAR_VARS)
include $(FAAC_TOP)/libfaac/Android.mk

include $(CLEAR_VARS)
LOCAL_MODULE    := pcmtoaac
LOCAL_SRC_FILES := $(FAAC_TOP)/pcmtoaac/native-interfaces.cpp
LOCAL_C_INCLUDES += $(FAAC_TOP)/include/
LOCAL_LDLIBS +=  -llog -ldl -lz
LOCAL_SHARED_LIBRARIES := faac
include $(BUILD_SHARED_LIBRARY)
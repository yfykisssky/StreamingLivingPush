LOCAL_PATH := $(call my-dir)
X264_TOP := $(LOCAL_PATH)

include $(CLEAR_VARS)
LOCAL_MODULE := libx264
LOCAL_SRC_FILES := $(X264_TOP)/libs/libx264.a
LOCAL_EXPORT_C_INCLUDES := $(X264_TOP)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := encodeX264
LOCAL_SRC_FILES := $(X264_TOP)/encode/x264interfaces.cpp \
                   $(X264_TOP)/encode/x264encoder.cpp
LOCAL_C_INCLUDES +=$(X264_TOP)/include

LOCAL_C_INCLUDES +=$(X264_TOP)/leak/include/
LOCAL_SRC_FILES +=$(X264_TOP)/leak/src/AllocationHandlers.cpp \
                  $(X264_TOP)/leak//src/MemoryTrace.cpp

LOCAL_STATIC_LIBRARIES := libx264
LOCAL_LDLIBS +=  -llog -ldl -lz

include $(BUILD_SHARED_LIBRARY)

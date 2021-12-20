LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES:=       \
		bitstream.c     \
		blockswitch.c   \
		channels.c      \
		fft.c           \
		filtbank.c      \
		frame.c         \
		huff2.c         \
		huffdata.c      \
		quantize.c      \
		stereo.c        \
		tns.c           \
		util.c
LOCAL_MODULE:= libfaac
LOCAL_C_INCLUDES :=         \
    $(LOCAL_PATH)           \
    $(FAAC_TOP)             \
    $(FAAC_TOP)/include
LOCAL_CFLAGS:= -DHAVE_CONFIG_H
include $(BUILD_SHARED_LIBRARY)
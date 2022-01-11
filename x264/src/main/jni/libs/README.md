#Mac系统编译
#编译报错可以先看下生成的.a文件是否正确
#NDK:17.2.4988734

#!/bin/bash
#arm-v7a
# 将NDK的路径替换成你自己的NDK路径
NDK_ROOT=......
TOOLCHAIN=$NDK_ROOT/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64
PREFIX=./android/armeabi-v7a
FLAGS="-isysroot $NDK_ROOT/sysroot -isystem $NDK_ROOT/sysroot/usr/include/arm-linux-androideabi -D__ANDROID_API__=21 -g -DANDROID -ffunction-sections -funwind-tables -fstack-protector-strong -no-canonical-prefixes -march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16 -mthumb -Wa,--noexecstack -Wformat -Werror=format-security  -O0 -fPIC"

./configure \
--prefix=$PREFIX \
--enable-static \
--enable-pic \
--disable-asm \
--disable-cli \
--host=arm-linux \
--cross-prefix=$TOOLCHAIN/bin/arm-linux-androideabi- \
--sysroot=$NDK_ROOT/platforms/android-21/arch-arm \
--extra-cflags="$FLAGS"


#!/bin/bash
#arm64-v8a
# 将NDK的路径替换成你自己的NDK路径
NDK_ROOT=......
TOOLCHAIN=$NDK_ROOT/toolchains/aarch64-linux-android-4.9/prebuilt/darwin-x86_64
PREFIX=./android/arm64
FLAGS="-isysroot $NDK_ROOT/sysroot -isystem $NDK_ROOT/sysroot/usr/include/aarch64-linux-android -D__ANDROID_API__=21 -g -DANDROID -ffunction-sections -funwind-tables -fstack-protector-strong -no-canonical-prefixes -march=armv8-a -Wa,--noexecstack -Wformat -Werror=format-security  -O0 -fPIC"

./configure \
--prefix=$PREFIX \
--enable-static \
--enable-pic \
--disable-asm \
--disable-cli \
--host=arm-linux \
--cross-prefix=$TOOLCHAIN/bin/aarch64-linux-android- \
--sysroot=$NDK_ROOT/platforms/android-21/arch-arm64 \
--extra-cflags="$FLAGS"
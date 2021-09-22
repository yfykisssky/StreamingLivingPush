cmake_minimum_required(VERSION 3.4.1)
add_subdirectory(src/main/cpp/librtmp)
add_library(
             librtmplib
             SHARED
             src/main/cpp/native-interfaces.c
             )

find_library(   log-lib
                 log )


target_link_libraries( librtmplib
                       rtmp
                       ${log-lib} )
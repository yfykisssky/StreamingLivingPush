cmake_minimum_required(VERSION 3.4.1)
add_subdirectory(./librtmp)
add_library(
             librtmplib
             SHARED
             rtmp/rtmpinterfaces.c
             )

find_library(   log-lib
                 log )


target_link_libraries( librtmplib
                       rtmp
                       ${log-lib} )
cmake_minimum_required(VERSION 3.22.1)

project(trestle_android_native LANGUAGES CXX)

add_library(cutils SHARED libcutils.cpp)

target_compile_features(cutils PRIVATE cxx_std_20)
target_compile_options(cutils PRIVATE -Wall -Wextra -Werror)
target_link_options(
    cutils
    PRIVATE
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384"
)

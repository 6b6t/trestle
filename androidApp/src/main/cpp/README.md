# Android cutils runtime library

Trestle builds `libcutils.so` from the Android Open Source Project-compatible
implementation in `libcutils.cpp`. The native linker uses a 16 KB maximum page
size so Android can load it on devices with 4 KB or 16 KB memory pages.

The APK must contain this library in its native-library namespace. Mesa links
to it by name. Android does not resolve the same file when Trestle downloads it
into the application data directory.

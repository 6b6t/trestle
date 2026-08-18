package net.kdt.pojavlaunch.utils;

public final class JREUtils {
    private JREUtils() {}

    public static native int chdir(String path);
    public static native boolean dlopen(String path);
    public static native void setLdLibraryPath(String path);
    public static native void setupBridgeWindow(Object surface);
    public static native void releaseBridgeWindow();
}

package org.lwjgl.glfw;

import java.lang.ref.WeakReference;

/**
 * Android-side endpoint for the GLFW bridge loaded into the Minecraft JVM.
 *
 * The package and method signatures are part of the patched LWJGL ABI.
 */
public final class CallbackBridge {
    public interface Listener {
        String accessClipboard(int action, String value);
        void onGrabStateChanged(boolean grabbing);
        float getDensity();
    }

    public static final int CLIPBOARD_COPY = 2000;
    public static final int CLIPBOARD_PASTE = 2001;
    public static final int CLIPBOARD_OPEN = 2002;

    private static WeakReference<Listener> listener = new WeakReference<>(null);
    private static boolean grabbing;
    private static float mouseX;
    private static float mouseY;
    private static volatile boolean inputReady;

    private CallbackBridge() {}

    public static void initialize(Listener nextListener) {
        listener = new WeakReference<>(nextListener);
    }

    public static void clear(Listener currentListener) {
        if (listener.get() == currentListener) listener.clear();
        inputReady = false;
    }

    public static void setInputReady(boolean ready) {
        inputReady = ready;
    }

    public static boolean isGrabbing() {
        return grabbing;
    }

    public static void moveCursor(float deltaX, float deltaY) {
        if (!inputReady) return;
        mouseX += deltaX;
        mouseY += deltaY;
        nativeSendCursorPos(mouseX, mouseY);
    }

    public static void setCursor(float x, float y) {
        if (!inputReady) return;
        mouseX = x;
        mouseY = y;
        nativeSendCursorPos(x, y);
    }

    public static void sendKey(int key, boolean pressed) {
        sendKey(key, 0, pressed ? 1 : 0, 0);
    }

    public static void sendKey(int key, int scancode, int action, int mods) {
        if (!inputReady) return;
        nativeSendKey(key, scancode, action, mods);
    }

    public static void sendCharacter(char character) {
        sendCharacter(character, 0);
    }

    public static void sendCharacter(char character, int mods) {
        if (!inputReady) return;
        nativeSendCharMods(character, mods);
        nativeSendChar(character);
    }

    public static void sendMouseButton(int button, boolean pressed) {
        sendMouseButton(button, pressed ? 1 : 0, 0);
    }

    public static void sendMouseButton(int button, int action, int mods) {
        if (!inputReady) return;
        nativeSendMouseButton(button, action, mods);
    }

    public static void sendScroll(double horizontal, double vertical) {
        if (!inputReady) return;
        nativeSendScroll(horizontal, vertical);
    }

    public static void sendScreenSize(int width, int height) {
        if (!inputReady) return;
        nativeSendScreenSize(width, height);
    }

    @SuppressWarnings("unused")
    private static String accessAndroidClipboard(int action, String value) {
        Listener current = listener.get();
        return current == null ? "" : current.accessClipboard(action, value);
    }

    @SuppressWarnings("unused")
    private static void onGrabStateChanged(boolean nextGrabbing) {
        grabbing = nextGrabbing;
        Listener current = listener.get();
        if (current != null) current.onGrabStateChanged(nextGrabbing);
    }

    @SuppressWarnings("unused")
    private static void onDirectInputEnable() {
        // Android gamepad events are translated to GLFW keyboard, mouse, and cursor events.
    }

    @SuppressWarnings("unused")
    private static float getAndroidDPI() {
        Listener current = listener.get();
        return current == null ? 1.0f : current.getDensity();
    }

    @SuppressWarnings("unused")
    private static boolean notifyLauncher(int type, int[] action) {
        return false;
    }

    public static native void nativeSetUseInputStackQueue(boolean enabled);
    private static native boolean nativeSendChar(char codepoint);
    private static native boolean nativeSendCharMods(char codepoint, int mods);
    private static native void nativeSendKey(int key, int scancode, int action, int mods);
    private static native void nativeSendCursorPos(float x, float y);
    private static native void nativeSendMouseButton(int button, int action, int mods);
    private static native void nativeSendScroll(double horizontal, double vertical);
    private static native void nativeSendScreenSize(int width, int height);
}

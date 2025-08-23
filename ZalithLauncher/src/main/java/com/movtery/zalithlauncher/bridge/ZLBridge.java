package com.movtery.zalithlauncher.bridge;

import android.content.Context;

public final class ZLBridge {
    //AWT
    public static final int EVENT_TYPE_CHAR = 1000;
    public static final int EVENT_TYPE_CURSOR_POS = 1003;
    public static final int EVENT_TYPE_KEY = 1005;
    public static final int EVENT_TYPE_MOUSE_BUTTON = 1006;

    public static void sendKey(char keychar, int keycode) {
        // TODO: Android -> AWT keycode mapping
        sendInputData(EVENT_TYPE_KEY, (int) keychar, keycode, 1, 0);
        sendInputData(EVENT_TYPE_KEY, (int) keychar, keycode, 0, 0);
    }

    public static void sendKey(char keychar, int keycode, int state) {
        // TODO: Android -> AWT keycode mapping
        sendInputData(EVENT_TYPE_KEY, (int) keychar, keycode, state, 0);
    }

    public static void sendChar(char keychar){
        sendInputData(EVENT_TYPE_CHAR, (int) keychar, 0, 0, 0);
    }

    public static void sendMousePress(int awtButtons, boolean isDown) {
        sendInputData(EVENT_TYPE_MOUSE_BUTTON, awtButtons, isDown ? 1 : 0, 0, 0);
    }

    public static void sendMousePress(int awtButtons) {
        sendMousePress(awtButtons, true);
        sendMousePress(awtButtons, false);
    }

    public static void sendMousePos(int x, int y) {
        sendInputData(EVENT_TYPE_CURSOR_POS, x, y, 0, 0);
    }

    //Game
    public static native void initializeGameExitHook();
    public static native void setupExitMethod(Context context);

    //Launch
    public static native void setLdLibraryPath(String ldLibraryPath);
    public static native boolean dlopen(String libPath);

    //Render
    public static native void setupBridgeWindow(Object surface);
    public static native void releaseBridgeWindow();
    public static native void moveWindow(int xOffset, int yOffset);
    public static native int[] renderAWTScreenFrame();

    //Input
    public static native void sendInputData(int type, int i1, int i2, int i3, int i4);
    public static native void clipboardReceived(String data, String mimeTypeSub);

    //Utils
    public static native int chdir(String path);

    static {
        System.loadLibrary("exithook");
        System.loadLibrary("pojavexec");
        System.loadLibrary("pojavexec_awt");
    }
}

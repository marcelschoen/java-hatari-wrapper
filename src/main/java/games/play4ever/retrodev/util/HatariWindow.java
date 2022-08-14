package games.play4ever.retrodev.util;

import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import x11.X11Ext;

import java.util.*;

public class HatariWindow {

    private String windowId = null;
    private String title = "";

    private DesktopWindow jnaDesktopWindow;

    private X11.Window x11Window;

    private static X11.Display x11Display;
    private static X11.Window x11RootWindow;

    public String getId() {
        return this.windowId;
    }

    public static List<HatariWindow> getDesktopWindowsAsList() {
        List<HatariWindow> alreadyOpenWindows = new ArrayList<>();
        if(PlatformUtil.getOperatingSystemType() == PlatformUtil.OSType.Linux) {
            getX11DesktopWindows().stream()
                    .map(w -> new HatariWindow(w, ""))
                    .forEach(hw -> alreadyOpenWindows.add(hw));
        } else {
            WindowUtils.getAllWindows(true).stream()
                    .map(w -> new HatariWindow(w))
                    .forEach(hw -> alreadyOpenWindows.add(hw));
        }
        return alreadyOpenWindows;
    }

    public static Map<String, HatariWindow> getAllDesktopWindowsAsMap() {
        Map<String, HatariWindow> alreadyOpenWindows = new HashMap<>();
        if(PlatformUtil.getOperatingSystemType() == PlatformUtil.OSType.Linux) {
            getX11DesktopWindows().stream()
                    .map(w -> new HatariWindow(w, "?"))
                    .forEach(hw -> alreadyOpenWindows.put(hw.getId(), hw));
        } else {
            WindowUtils.getAllWindows(true).stream()
                    .map(w -> new HatariWindow(w))
                    .forEach(hw -> alreadyOpenWindows.put(hw.getId(), hw));
        }
        return alreadyOpenWindows;
    }

    public void toForeground() {
        if(jnaDesktopWindow != null) {
            User32.INSTANCE.SetForegroundWindow(jnaDesktopWindow.getHWND());
            User32.INSTANCE.SetFocus(jnaDesktopWindow.getHWND());
        }
        if(x11Window != null) {
            X11Ext.INSTANCE.XRaiseWindow(x11Display, x11Window);
        }
    }

    public String getTitle() {
        return this.title;
    }

    public HatariWindow(DesktopWindow desktopWindow) {
        this.windowId = desktopWindow.getHWND().toString();
        this.title = desktopWindow.getTitle();
        this.jnaDesktopWindow = desktopWindow;
    }

    public HatariWindow(X11.Window x11Window, String title) {
        this.windowId = x11Window.toString();
        this.title = title;
        this.x11Window = x11Window;
    }

    private static List<DesktopWindow> getJnaDesktopWindows() {
        return WindowUtils.getAllWindows(true);
    }

    private static List<X11.Window> getX11DesktopWindows() {
        if(x11Display == null) {
            x11Display = X11.INSTANCE.XOpenDisplay(":0");
            x11RootWindow = X11.INSTANCE.XRootWindow(x11Display, 0);
        }

        long[] childrenWindowIdArray = getChildrenListArch64(x11Display, x11RootWindow);
        List<X11.Window> x11Windows = new ArrayList<>();
        for (long windowId : childrenWindowIdArray) {
            X11.Window window = new X11.Window(windowId);
            // get window attributes
            X11.XWindowAttributes attributes = new X11.XWindowAttributes();
            X11.INSTANCE.XGetWindowAttributes(x11Display, window, attributes);
            // get window title
            //X11.XTextProperty windowTitle = new X11.XTextProperty();
            PointerByReference windowTitle = new PointerByReference();
            X11.INSTANCE.XFetchName(x11Display, window, windowTitle);
            System.out.println(">> Window title: " + windowTitle.getValue().toString());
            x11Windows.add(window);
        }
        return x11Windows;
    }

    private static long[] getChildrenListArch64(X11.Display display, X11.Window parentWindow) {
        long[] childrenWindowIdArray = new long[] {};

        // prepare reference values
        X11.WindowByReference rootWindowRef = new X11.WindowByReference();
        X11.WindowByReference parentWindowRef = new X11.WindowByReference();
        PointerByReference childrenPtr = new PointerByReference();
        IntByReference childrenCount = new IntByReference();

        // find all children to the rootWindow
        if (X11.INSTANCE.XQueryTree(display, parentWindow, rootWindowRef, parentWindowRef, childrenPtr, childrenCount) == 0) {
            X11.INSTANCE.XCloseDisplay(display);
            return childrenWindowIdArray;
        }

        // get all window id's from the pointer and the count
        if (childrenCount.getValue() > 0) {
            childrenWindowIdArray = childrenPtr.getValue().getLongArray(0, childrenCount.getValue());
        }

        return childrenWindowIdArray;
    }
}

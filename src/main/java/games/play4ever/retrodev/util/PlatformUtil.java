package games.play4ever.retrodev.util;

import java.util.Locale;

/**
 * helper class to check the operating system this Java VM runs in
 * <p>
 * please keep the notes below as a pseudo-license
 * <p>
 * http://stackoverflow.com/questions/228477/how-do-i-programmatically-determine-operating-system-in-java
 * compare to http://svn.terracotta.org/svn/tc/dso/tags/2.6.4/code/base/common/src/com/tc/util/runtime/Os.java
 * http://www.docjar.com/html/api/org/apache/commons/lang/SystemUtils.java.html
 */
public class PlatformUtil {

    // cached result of OS detection
    protected static OSType detectedOS;

    ;

    /**
     * detect the operating system from the os.name System property and cache
     * the result
     *
     * @returns - the operating system detected
     */
    public static OSType getOperatingSystemType() {
        if (detectedOS == null) {
            String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if ((OS.contains("mac")) || (OS.contains("darwin"))) {
                detectedOS = OSType.MacOS;
            } else if (OS.contains("win")) {
                detectedOS = OSType.Windows;
            } else if (OS.contains("nux")) {
                detectedOS = OSType.Linux;
            } else {
                detectedOS = OSType.Other;
            }
        }
        System.out.println(">> Detected platform: " + detectedOS.name());
        return detectedOS;
    }

    /**
     * types of Operating Systems
     */
    public enum OSType {
        Windows("/hatari-windows.zip", "hatari-win64-release.exe"),
        MacOS(null, null), // TBD
        Linux("/hatari-linux.zip", "hatari"),
        Other(null, null);
        public String emulatorArchive;

        public String emulatorExecutable;

        OSType(String emulatorArchive, String emulatorExecutable) {
            this.emulatorArchive = emulatorArchive;
            this.emulatorExecutable = emulatorExecutable;
        }
    }
}
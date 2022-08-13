package games.play4ever.retrodev.hatari;

import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.*;
import games.play4ever.retrodev.util.FileUtil;
import games.play4ever.retrodev.util.PlatformUtil;
import games.play4ever.retrodev.util.ZipHelper;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;

import static games.play4ever.retrodev.util.FileUtil.*;

/**
 * Handler for the native Atari ST emulator "Hatari".
 * Will unpack the emulator from the Java resources into
 * a temporary directory and then execute it from there.
 *
 * @author Marcel Schoen
 */
public class Hatari {

    private static Robot robot;

    /**
     * TOS versions included in this Java wrapper.
     */
    public enum TOS {
        etos512cz,
        etos512de,
        etos512es,
        etos512fi,
        etos512fr,
        etos512gr,
        etos512hu,
        etos512it,
        etos512nl,
        etos512no,
        etos512pl,
        etos512ru,
        etos512se,
        etos512sg,
        etos512tr,
        etos512uk,
        etos512us,
        tos100,
        tos102,
        tos104,
        tos106,
        tos205,
        tos206,
        tos306,
        tos402,
        tos404;

        /**
         * Tries to return the EmuTOS version which corresponds to the current locale language
         * or country code value of the running JVM.
         *
         * @return The EmuTOS matching the current language / country, or the US version (fallback).
         */
        public static TOS getEmuTOSByLocale() {
            // 1st try by language code, e.g. "de", "it"
            TOS value = getLocalizedEmuTOS(Locale.getDefault().getLanguage().toLowerCase());
            if(value == null) {
                // If that fails, try country code (e.g. "uk", "us")
                value = getLocalizedEmuTOS(Locale.getDefault().getCountry().toLowerCase());
            }
            return value == null ? etos512us : value;
        }

        /**
         * Tries to return the EmuTOS version which corresponds to the given language
         * or country code value.
         *
         * @param code The 2-character country or language code
         * @return The EmuTOS matching the current language, or the US version (fallback).
         */
        public static TOS getEmuTOSByCountryOrLanguage(String code) {
            return getLocalizedEmuTOS(code);
        }

        private static TOS getLocalizedEmuTOS(String code) {
            if(code.trim().length() != 2) {
                throw new IllegalArgumentException("Parameter code must be a 2-character value: " +  code);
            }
            String tosName = "etos512" + code;
            try {
                return valueOf(tosName);
            } catch(Exception e) {
                // ignore
            }
            return null;
        }
    }

    /**
     * Defines which type of Atari system to emulate.
     */
    public enum MACHINE {

        /**
         * Standard ST
         */
        st("st", 512, TOS.tos100, false),

        /**
         * Mega ST (blitter)
         */
        megast("megast", 1024, TOS.tos102, true),

        /**
         * STE (hardware scrolling, DMA audio, 4 ports)
         */
        ste("ste", 512, TOS.tos106, true);

        private TOS tosVersion = TOS.tos106;
        private String type;
        boolean hasBlitter = false;
        private int kbMemory = 1024; // defaults to 1 MB

        MACHINE(String type, int defaultMemory, TOS defaultTosVersion, boolean hasBlitter) {
            this.type = type;
            this.kbMemory = defaultMemory;
            this.tosVersion = defaultTosVersion;
            this.hasBlitter = hasBlitter;
        }
    }

    /**
     * Atari ST screen mode.
     */
    public enum MODE {
        /**
         * Low resolution (320x200 / 16 colors)
         */
        low("low"),
        /**
         * Medium resolution (640x200 / 4 colors)
         */
        medium("med"),
        /**
         * High resolution (640x400 / monochrome)
         */
        high("high");

        String value;

        MODE(String value) {
            this.value = value;
        }
    }

    /**
     * Possible values to use for the memory.
     */
    public enum MEMORY {
        kb256(256),
        kb512(512),
        mb1(1024),
        mb2(2 * 1024),
        mb4(4 * 1024),
        mb8(8 * 1024);

        private int kbMemory;

        MEMORY(int kbMemory) {
            this.kbMemory = kbMemory;
        }
    }

    public enum INSTANCES {
        // For running the compiled program (game)
        testing,

        // For running the GFA editor and compiler
        building
    }

    private static File workDirectory = new File(".");

    /** Store reference to emulator processes. */
    private static Map<INSTANCES, Process> emulatorProcesses = new HashMap<>();

    /** Store reference to emulator windows. */
    private static Map<INSTANCES, DesktopWindow> emulatorWindows = new HashMap<>();

    static {
        try {
            robot = new Robot();
        } catch(Exception e) {
            throw new RuntimeException("Failed to initialize robot API");
        }
    }

    /**
     * Main method used mostly for testing / demonstration purposes. Allows to run
     * the emulator manually from a shell using the executable Jar file.
     *
     * @param args The command line arguments.
     */
    public static void main(String ... args) {
        try {
            MACHINE machine = MACHINE.ste;
            MEMORY memory = MEMORY.mb1;
            MODE mode = MODE.low;
            File fileToCopy = null;

            prepare(new File("./hatari"), TOS.getEmuTOSByLocale());
            startEmulator(INSTANCES.testing, machine, memory, mode, machine.hasBlitter, null, fileToCopy);

        } catch(Exception e) {
            e.printStackTrace();
            printUsage();
            System.exit(-1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar java-hatari-wrapper-<version>.jar [...arguments]");
        System.out.println("Arguments:");
        System.out.println("");
        System.out.println("-machine st|megast|ste");
        System.out.println("");
        System.out.println("-memory 1|2|4");
        System.out.println("");
        System.out.println(" Chose either 1, 2 or 4 MB RAM.");
        System.out.println("");
        System.out.println("-mode low|mid|high");
        System.out.println("");
        System.out.println(" Screen resolution:");
        System.out.println("  low = 320x200  16 colors");
        System.out.println("  mid = 640x200   4 colors");
        System.out.println(" high = 640x400 monochrome");
    }

    /**
     * Launches the emulator of the given instance. A local file system directory
     * can be provided which will then be mounted as GEMDOS drive "C:" in the emulation.
     *
     * @param instance        The emulator instance to start. If such an instance is already
     *                        running, it will be killed first.
     * @param machine         The type of machine to emulate.
     * @param memory          Optional: The amount of memory for the emulation.
     * @param mode            Optional: The screen mode (defaults to mono)
     * @param blitter         True to use the blitter, false to disable it (depends on machine type).
     * @param memorySnapshotFile Optional: Memory snapshot file to start the emulator with.
     * @param programOrSource Optional: A program or GFA source file to copy into the GEMDOS drive.
     */
    private static DesktopWindow startEmulator(INSTANCES instance,
                                             MACHINE machine,
                                             MEMORY memory,
                                             MODE mode,
                                             boolean blitter,
                                             File memorySnapshotFile,
                                             File programOrSource) {

        if(emulatorWindows.containsKey(instance)) {
            // Emulator already running, return reference to open window
            return emulatorWindows.get(instance);
        }

        Map<WinDef.HWND, DesktopWindow> alreadyOpenWindows = new HashMap<>();
        WindowUtils.getAllWindows(true).stream().forEach(w -> alreadyOpenWindows.put(w.getHWND(), w));


        System.out.println(">> Start emulator in: " + Hatari.workDirectory.getAbsolutePath());

        ArrayList<String> args = new ArrayList<>();
        args.add(new File(Hatari.workDirectory,
                PlatformUtil.getOperatingSystemType().emulatorExecutable).getAbsolutePath()); // TODO - multiplatform support
        args.add("--fast-boot");
        args.add("true");

        args.add("--statusbar");
        args.add("false");

        if(memorySnapshotFile != null && memorySnapshotFile.isFile()) {
            args.add("--memstate");
            args.add(memorySnapshotFile.getAbsolutePath());
        }

        if(instance != INSTANCES.testing) {
            // Super-charge only build process
            args.add("--fast-forward");
            args.add("true");
        }

        if (emulatorProcesses.get(instance) != null) {
            // Instance of this type already running - try to kill it first
            emulatorProcesses.get(instance).destroyForcibly();
        }

        if (blitter) {
            args.add("--blitter");
            args.add("true");
        }

        MACHINE runtimeMachine = machine == null ? MACHINE.st : machine;
        args.add("--machine");
        args.add(runtimeMachine.type);

        int kbMemory = memory == null ? machine.kbMemory : memory.kbMemory;
        args.add("--memsize");
        args.add("" + kbMemory);

        // No annoying keyboard beeps allowed during source conversion and compilation
        if(instance != INSTANCES.testing) {
            args.add("--sound");
            args.add("off");
        }

        // Always start in windowed mode. User can still switch to fullscreen manually.
        args.add("-w");

        MODE screenMode = mode == null ? MODE.high : mode;
        args.add("--tos-res");
        args.add(screenMode.value);

        if (screenMode == MODE.high) {
            args.add("--monitor");
            args.add("mono");
        }

        File runtimeFolder = getOrCreateRuntimeBuildFolder();
        args.add("-d");
        args.add(runtimeFolder.getAbsolutePath());

        if (programOrSource != null && programOrSource.isFile()) {
            if (programOrSource.getName().toLowerCase().endsWith(".zip")) {
                try {
                    System.out.println("Unpack " + programOrSource.getAbsolutePath() + " to: " + runtimeFolder.getAbsolutePath());
                    unpackZip(runtimeFolder, new FileInputStream(programOrSource));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to unpack zip file " + programOrSource + ": " + e, e);
                }
            } else {
                File targetFile = new File(runtimeFolder, programOrSource.getName());
                System.out.println("> Copy file " + programOrSource.getAbsolutePath() + " to " + targetFile.getAbsolutePath());
                copyFileTo(programOrSource, targetFile);
            }
        }

        String[] finalArgs = args.toArray(new String[0]);

        System.out.println("------------- emulator arguments ---------");
        Arrays.asList(finalArgs).stream().forEach(arg -> System.out.println("> " + arg));
        System.out.println("------------------------------------------");

        DesktopWindow result = null;
        ProcessBuilder pb = new ProcessBuilder(finalArgs);
        pb.directory(Hatari.workDirectory.getAbsoluteFile());
        try {
            Process p = pb.start();
            emulatorProcesses.put(instance, p);

            // Try to get handle of emulator window for up to 1 second
            long now = System.currentTimeMillis();
            while(result == null && (System.currentTimeMillis() - now) < 1000) {
                List<DesktopWindow> windows = WindowUtils.getAllWindows(true);
                for(DesktopWindow desktopWindow : windows) {
                    // Make sure it's not a window that was open before (like one from an already running Hatari instance)
                    if(alreadyOpenWindows.get(desktopWindow.getHWND()) == null) {
                        // Check if it's a Hatari window
                        if(desktopWindow.getTitle().startsWith("Hatari v")) {
                            emulatorWindows.put(instance, desktopWindow);
                            result = desktopWindow;
                            break;
                        }
                    }
                }
                Thread.sleep(50);
            }

        } catch (Exception e) {
            // For some reason, the emulator window handle could not be obtained - kill the emulator and raise an exception
            stopEmulator(instance);
            throw new RuntimeException("Failed to start the emulator: " + e, e);
        }

        if(result == null) {
            // For some reason, the emulator window handle could not be obtained - kill the emulator and raise an exception
            stopEmulator(instance);
            throw new RuntimeException("Failed to obtain handle of emulator window!");
        }

        return result;
    }

    private static File getOrCreateRuntimeBuildFolder() {
        File runtimeFolder = new File(Hatari.workDirectory, "drivec");
        if(runtimeFolder.exists() && runtimeFolder.isDirectory()) {
            return runtimeFolder;
        }

        deleteDir(runtimeFolder);
        runtimeFolder.delete();
        runtimeFolder.mkdirs();

        File harddiscFolder = new File("src/resources/java/hatari/gfa_hdd");
        if (harddiscFolder != null && harddiscFolder.isDirectory()) {
            // Prepare runtime harddisc folder by copying the
            // given source folder into the build temp dir
            FileUtil.copyDirectory(harddiscFolder, runtimeFolder);
        }
        return runtimeFolder;
    }

    /**
     * Stops all open emulator instances.
     */
    public static void stopEmulators() {
        System.out.println(">> Shutting down all Hatari instances");
        emulatorProcesses.values().forEach(p -> p.destroyForcibly());
        emulatorProcesses.clear();
        emulatorWindows.clear();
    }

    /**
     * Stops the given emulator instance, if it is still running.
     *
     * @param instance The emulator instance to stop.
     */
    public static void stopEmulator(INSTANCES instance) {
        if (emulatorProcesses.get(instance) != null) {
            System.out.println(">> Shutting down Hatari instance " + instance.name());
            emulatorProcesses.get(instance).destroyForcibly();
            emulatorProcesses.remove(instance);
            emulatorWindows.remove(instance);
        }
    }

    /**
     * Performs some key presses in the given window, using the Java Robot API. This method
     * will first invoke "keyPress()" and then "keyRelease()" on each key, one by one. This
     * method should be used to simulate the user typing a command or a file name.
     *
     * @param robot The Robot instance.
     * @param window The window (will be brought to the foreground).
     * @param keys The list of key codes to send to the window.
     */
    private static void pressKeys(Robot robot, WinDef.HWND window, int ... keys) {
        try {
            User32.INSTANCE.SetFocus(window);
            User32.INSTANCE.SetForegroundWindow(window);
            for(int key : keys) {
                robot.keyPress(key);
                robot.keyRelease(key);
            }
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException("** Failed to enter keyboard presses **");
        } finally {
            Arrays.stream(keys).forEach(k -> robot.keyRelease(k));
        }
    }

    /**
     * Performs some key presses in the given window, using the Java Robot API. This method
     * will first invoke "keyPress()" on all given keys (effectively pressing the all at the same time),
     * and then "keyRelease()" on all of them. This should be used to simulate shortcut keypressed,
     * like pressing "ALT_GR" together with "l" to load a memory shortcut.
     *
     * @param robot The Robot instance.
     * @param window The window (will be brought to the foreground).
     * @param keys The list of key codes to send to the window.
     */
    private static void pressKeysTogether(Robot robot, WinDef.HWND window, int ... keys) {
        try {
            User32.INSTANCE.SetFocus(window);
            User32.INSTANCE.SetForegroundWindow(window);
            for(int key : keys) {
                robot.keyPress(key);
            }
        } catch(Exception e) {
            throw new RuntimeException("** Failed to enter keyboard presses **");
        } finally {
            Arrays.stream(keys).forEach(k -> robot.keyRelease(k));
        }
    }

    /**
     * Unpacks the Hatari emulator into a local temporary directory,
     * if it isn't already there.
     *
     * @param workDirectory The directory where to unpack the emulator files.
     */
    public static void prepare(File workDirectory, TOS tos) {
        Hatari.workDirectory = workDirectory;
        Hatari.workDirectory.mkdirs();
        File tempDir = workDirectory;
        PlatformUtil.OSType osType = PlatformUtil.getOperatingSystemType();
        File hatariBin = new File(tempDir, osType.emulatorExecutable);
        if (!hatariBin.exists() || !hatariBin.canExecute()) {
            try {
                String emulatorArchive = osType.emulatorArchive;
                if(emulatorArchive != null) {
                    System.out.println(">> Unpack emulator " + osType.emulatorArchive + " to: " + tempDir);
                    unpackEmulator(tempDir, osType.emulatorArchive);
                    System.out.println(">> Emulator unpacked to: " + tempDir);
                } else {
                    throw new RuntimeException(">> Platform '" + PlatformUtil.getOperatingSystemType().name() + " not yet supported.");
                }
            } catch (IOException ex) {
                deleteDir(tempDir);
                throw new RuntimeException("Failed to prepare the emulator: " + ex, ex);
            }
        }
        // Always extract TOS to make sure it doesn't use a wrong version from a previous run
        try {
            unpackTOS(tempDir, tos);
        } catch (IOException ex) {
            deleteDir(tempDir);
            throw new RuntimeException("Failed to prepare TOS: " + ex, ex);
        }
    }

    /**
     * Unpacks the requested TOS image into the given working directory.
     *
     * @param tempDir The target work/build directory.
     * @param tos     The TOS image to extract.
     * @throws IOException If the emulator could not be extracted.
     */
    private static void unpackTOS(File tempDir, TOS tos) throws IOException {
        InputStream tosImg = Hatari.class.getResourceAsStream("/tos/" + tos.name() + ".img");
        byte[] buffer = new byte[4096];
        File tosFile = new File(tempDir, "tos.img");
        FileOutputStream out = new FileOutputStream(tosFile);
        int len;
        while ((len = tosImg.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        out.close();
        System.out.println(">> Extracted TOS " + tos.name() + " to file: " + tosFile.getAbsolutePath());
    }


    /**
     * Unpacks the Hatari emulator into the given working directory.
     *
     * @param tempDir The target work/build directory.
     * @param fileZip The Hatari emulator zip archive.
     * @throws IOException If the emulator could not be extracted.
     */
    private static void unpackEmulator(File tempDir, String fileZip) throws IOException {
        InputStream resourceStream = Hatari.class.getResourceAsStream(fileZip);
        System.out.println("> Zip stream: " + resourceStream);
        unpackZip(tempDir, resourceStream);
        // Make Hatari executable... executable!
        File hatariExe = new File(tempDir, PlatformUtil.getOperatingSystemType().emulatorExecutable);
        hatariExe.setExecutable(true);
    }
}

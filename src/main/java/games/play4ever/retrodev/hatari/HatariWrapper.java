package games.play4ever.retrodev.hatari;

import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import games.play4ever.retrodev.util.FileUtil;
import games.play4ever.retrodev.util.PlatformUtil;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;

import static games.play4ever.retrodev.util.FileUtil.*;

/**
 * Handler for the native Atari ST emulator "Hatari".
 * Will unpack the emulator from the Java resources into
 * a temporary directory and then execute it from there.
 * <p>
 * When starting the emulator, an "INSTANCE" must be selected, either "testing"
 * or "building". The main differences between
 *
 * @author Marcel Schoen
 */
public class HatariWrapper {

    static File workDirectory = new File(".");
    private static Robot robot;
    /**
     * Store reference to emulator processes.
     */
    private static Map<HatariInstance, Process> emulatorProcesses = new HashMap<>();

    /**
     * Store reference to emulator windows.
     */
    private static Map<HatariInstance, DesktopWindow> emulatorWindows = new HashMap<>();

    static {
        try {
            robot = new Robot();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize robot API");
        }
    }

    /**
     * Main method used mostly for testing / demonstration purposes. Allows to run
     * the emulator manually from a shell using the executable Jar file.
     *
     * @param args The command line arguments.
     */
    public static void main(String... args) {
        try {
            HatariInstance instance = new HatariInstance("demo",
                    true,
                    false,
                    true,
                    true,
                    true,
                    true,
                    MachineType.ste,
                    TOS.tos206,
                    ScreenMode.low,
                    Memory.mb1);

            prepare(new File("./hatari"), TOS.getEmuTOSByLocale());
            startEmulator(instance, null, null);

        } catch (Exception e) {
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
     * @param instance           The emulator instance to start. If such an instance is already
     *                           running, it will be killed first.
     * @param memorySnapshotFile Optional: Memory snapshot file to start the emulator with.
     * @param programOrSource    Optional: A program or GFA source file to copy into the GEMDOS drive.
     */
    public static DesktopWindow startEmulator(HatariInstance instance,
                                               File memorySnapshotFile,
                                               File programOrSource) {

        if (emulatorWindows.containsKey(instance)) {
            // Emulator already running, return reference to open window
            return emulatorWindows.get(instance);
        }

        Map<WinDef.HWND, DesktopWindow> alreadyOpenWindows = new HashMap<>();
        if(PlatformUtil.getOperatingSystemType() == PlatformUtil.OSType.Windows) {
            WindowUtils.getAllWindows(true).stream().forEach(w -> alreadyOpenWindows.put(w.getHWND(), w));
        }

        if (emulatorProcesses.get(instance) != null) {
            // Instance of this type already running - try to kill it first
            emulatorProcesses.get(instance).destroyForcibly();
        }

        System.out.println(">> Start emulator in: " + HatariWrapper.workDirectory.getAbsolutePath());

        ArrayList<String> args = new ArrayList<>();
        args.add(new File(HatariWrapper.workDirectory,
                PlatformUtil.getOperatingSystemType().emulatorExecutable).getAbsolutePath()); // TODO - multiplatform support

        // Start with given runtime folder as GEMDOS drive C:
        File runtimeFolder = getOrCreateRuntimeBuildFolder();
        args.add("-d");
        args.add(runtimeFolder.getAbsolutePath());

        // Optional: Start with memory snapshot
        if (memorySnapshotFile != null && memorySnapshotFile.isFile()) {
            args.add("--memstate");
            args.add(memorySnapshotFile.getAbsolutePath());
        }

        // Add all additional arguments based on instance settings
        args.addAll(instance.getRuntimeArguments());


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

        System.out.println("------------- emulator arguments ---------\n\r");
        Arrays.asList(finalArgs).stream().forEach(arg -> System.out.print(arg + " "));
        System.out.println("\n\r------------------------------------------");

        DesktopWindow result = null;
        ProcessBuilder pb = new ProcessBuilder(finalArgs);
        pb.redirectError(new File(HatariWrapper.workDirectory, "error.log"));
        pb.redirectOutput(new File(HatariWrapper.workDirectory, "output.log"));
        pb.directory(HatariWrapper.workDirectory.getAbsoluteFile());
        try {
            Process p = pb.start();
            emulatorProcesses.put(instance, p);
            //System.out.println(">> Emulator process exit value: " + p.exitValue());

            if(!alreadyOpenWindows.isEmpty()) {
                // Try to get handle of emulator window for up to 1 second
                long now = System.currentTimeMillis();
                while (result == null && (System.currentTimeMillis() - now) < 5000) {
                    List<DesktopWindow> windows = WindowUtils.getAllWindows(true);
                    for (DesktopWindow desktopWindow : windows) {
                        // Make sure it's not a window that was open before (like one from an already running Hatari instance)
                        if (alreadyOpenWindows.get(desktopWindow.getHWND()) == null) {
                            // Check if it's a Hatari window
                            if (desktopWindow.getTitle().startsWith("Hatari v")) {
                                emulatorWindows.put(instance, desktopWindow);
                                result = desktopWindow;
                                break;
                            }
                        }
                    }
                    Thread.sleep(50);
                }
            } else {
                Thread.sleep(5000);
            }

        } catch (Exception e) {
            // For some reason, the emulator window handle could not be obtained - kill the emulator and raise an exception
            stopEmulator(instance);
            throw new RuntimeException("Failed to start the emulator: " + e, e);
        }

        if (result == null && !alreadyOpenWindows.isEmpty()) {
            // For some reason, the emulator window handle could not be obtained - kill the emulator and raise an exception
            stopEmulator(instance);
            throw new RuntimeException("Failed to obtain handle of emulator window!");
        }

        return result;
    }

    private static File getOrCreateRuntimeBuildFolder() {
        File runtimeFolder = new File(HatariWrapper.workDirectory, "drivec");
        if (runtimeFolder.exists() && runtimeFolder.isDirectory()) {
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
    public static void stopEmulator(HatariInstance instance) {
        if (emulatorProcesses.get(instance) != null) {
            System.out.println(">> Shutting down Hatari instance " + instance.getLabel());
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
     * @param robot  The Robot instance.
     * @param window The window (will be brought to the foreground).
     * @param keys   The list of key codes to send to the window.
     */
    private static void pressKeys(Robot robot, WinDef.HWND window, int... keys) {
        try {
            // NOTE: Currently only works on Windows
            if(PlatformUtil.getOperatingSystemType() == PlatformUtil.OSType.Windows) {
                User32.INSTANCE.SetFocus(window);
                User32.INSTANCE.SetForegroundWindow(window);
            }
            for (int key : keys) {
                robot.keyPress(key);
                robot.keyRelease(key);
            }
        } catch (Exception e) {
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
     * @param robot  The Robot instance.
     * @param window The window (will be brought to the foreground).
     * @param keys   The list of key codes to send to the window.
     */
    private static void pressKeysTogether(Robot robot, WinDef.HWND window, int... keys) {
        try {
            // NOTE: Currently only works on Windows
            if(PlatformUtil.getOperatingSystemType() == PlatformUtil.OSType.Windows) {
                User32.INSTANCE.SetFocus(window);
                User32.INSTANCE.SetForegroundWindow(window);
            }
            for (int key : keys) {
                robot.keyPress(key);
            }
        } catch (Exception e) {
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
        HatariWrapper.workDirectory = workDirectory;
        HatariWrapper.workDirectory.mkdirs();
        File tempDir = workDirectory;
        PlatformUtil.OSType osType = PlatformUtil.getOperatingSystemType();
        File hatariBin = new File(tempDir, osType.emulatorExecutable);
        if (!hatariBin.exists() || !hatariBin.canExecute()) {
            try {
                String emulatorArchive = osType.emulatorArchive;
                if (emulatorArchive != null) {
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
        InputStream tosImg = HatariWrapper.class.getResourceAsStream("/tos/" + tos.name() + ".img");
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
        InputStream resourceStream = HatariWrapper.class.getResourceAsStream(fileZip);
        System.out.println("> Zip stream: " + resourceStream);
        unpackZip(tempDir, resourceStream);
        // Make Hatari executable... executable!
        File hatariExe = new File(tempDir, PlatformUtil.getOperatingSystemType().emulatorExecutable);
        hatariExe.setExecutable(true);
    }
}

package games.play4ever.retrodev.hatari;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the configuration / runtime settings for a
 * Hatari instance. Default configuration:
 *
 * <ul>
 * <li></li>machine type: "ste"</li>
 * <li></li>memory: 1 MB</li>
 * <li></li>mode: ST low (320x200 16 colors)</li>
 * <li></li>TOS version: 2.06</li>
 * <li></li>sound enabled: yes</li>
 * <li></li>blitter enabled: yes</li>
 * <li></li>status bar enabled: yes</li>
 * <li></li>fast boot enabled: yes</li>
 * <li></li>full speed enabled: false</li>
 * </ul>
 * <p>
 * Basically the system most likely used for running / testing games.
 * For running development tools, the following configuration settings
 * would best be adapted as follows:
 *
 * <ul>
 * <li></li>memory: 4 MB</li>
 * <li></li>mode: ST high (640x400 monochrome)</li>
 * <li></li>sound enabled: no</li>
 * <li></li>full speed enabled: true</li>
 * </ul>
 *
 * @author Marcel Schoen
 */
public class HatariInstance {

    private String label = "hatari";

    private boolean windowed = true;
    private boolean fullSpeed = false;
    private boolean fastBoot = false;
    private boolean useBlitter = true;
    private boolean useSound = true;
    private boolean useStatusBar = true;

    private MachineType machine = MachineType.ste;
    private TOS tos = TOS.tos206;
    private ScreenMode mode = ScreenMode.low;
    private Memory memory = Memory.mb1;

    /**
     * Creates a new instance with the default configuration.
     */
    public HatariInstance(String label) {
        this.label = label;
    }

    /**
     * Creates a new instance configuration.
     *
     * @param windowed     True if the emulator is to run in a window, false for fullscreen.
     * @param fullSpeed    If true, the emulator runs at full host speed, false for accurate emulation speed.
     * @param fastBoot     If true, the boot process is a bit faster (memory check is cut short)
     * @param useBlitter   True to enable the blitter emulation
     * @param useSound     True to enable sound, false to disable (annoying keyboard sounds...)
     * @param useStatusBar True to enable the statusbar at the lower window border.
     * @param machine      The type of machine (ST, STE, MegaST)
     * @param tos          The TOS version to use.
     * @param mode         The screen mode to use.
     * @param memory       The memory configuration.
     */
    public HatariInstance(String label,
                          boolean windowed,
                          boolean fullSpeed,
                          boolean fastBoot,
                          boolean useBlitter,
                          boolean useSound,
                          boolean useStatusBar,
                          MachineType machine,
                          TOS tos,
                          ScreenMode mode,
                          Memory memory) {
        this.label = label;
        this.windowed = windowed;
        this.fastBoot = fastBoot;
        this.fullSpeed = fullSpeed;
        this.useBlitter = useBlitter;
        this.useSound = useSound;
        this.useStatusBar = useStatusBar;
        this.machine = machine;
        this.tos = tos;
        this.mode = mode;
        this.memory = memory;
    }

    private static void addArgument(List<String> args, String argument, String value) {
        args.add(argument);
        args.add(value);
    }

    private static void addArgument(List<String> args, String argument, boolean value) {
        args.add(argument);
        args.add(String.valueOf(value));
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isWindowed() {
        return windowed;
    }

    public void setWindowed(boolean windowed) {
        this.windowed = windowed;
    }

    public boolean isFullSpeed() {
        return fullSpeed;
    }

    public void setFullSpeed(boolean fullSpeed) {
        this.fullSpeed = fullSpeed;
    }

    public boolean isFastBoot() {
        return fastBoot;
    }

    public void setFastBoot(boolean fastBoot) {
        this.fastBoot = fastBoot;
    }

    public boolean isUseBlitter() {
        return useBlitter;
    }

    public void setUseBlitter(boolean useBlitter) {
        this.useBlitter = useBlitter;
    }

    public boolean isUseSound() {
        return useSound;
    }

    public void setUseSound(boolean useSound) {
        this.useSound = useSound;
    }

    public boolean isUseStatusBar() {
        return useStatusBar;
    }

    public void setUseStatusBar(boolean useStatusBar) {
        this.useStatusBar = useStatusBar;
    }

    public MachineType getMachine() {
        return machine;
    }

    public void setMachine(MachineType machine) {
        this.machine = machine;
    }

    public TOS getTos() {
        return tos;
    }

    public void setTos(TOS tos) {
        this.tos = tos;
    }

    public ScreenMode getMode() {
        return mode;
    }

    public void setMode(ScreenMode mode) {
        this.mode = mode;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    /**
     * Returns the Hatari emulator commandline arguments based on
     * the settings of this instance.
     *
     * @return The commandline arguments.
     */
    public List<String> getRuntimeArguments() {
        List<String> args = new ArrayList<>();

        addArgument(args, "--fast-boot", fastBoot);
        addArgument(args, "--fast-forward", fullSpeed);
        addArgument(args, "--statusbar", useStatusBar);
        addArgument(args, "--blitter", useBlitter);
        if (!useSound) {
            addArgument(args, "--sound", "off");
        }

        addArgument(args, "--machine", machine.type);
        addArgument(args, "--memsize", "" + memory.kbMemory);
        addArgument(args, "--tos-res", mode.value);
        args.add("--monitor");
        if (mode == ScreenMode.high) {
            args.add("mono");
        } else {
            args.add("tv");
        }
        if (windowed) {
            args.add("-w");
        } else {
            args.add("-f");
        }
        return args;
    }
}

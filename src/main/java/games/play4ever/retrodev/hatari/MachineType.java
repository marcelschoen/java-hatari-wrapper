package games.play4ever.retrodev.hatari;

/**
 * Defines which type of Atari system to emulate.
 *
 * @author Marcel Schoen
 */
public enum MachineType {

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

    /** The default TOS version for this machine type. */
    public TOS tosVersion = TOS.tos106;

    /** The type string ("st", "megast" or "ste") */
    public String type;

    /** True if the system has a blitter chip. */
    public boolean hasBlitter = false;

    /** Default amount of memory in kilobytes. */
    public int kbMemory = 1024; // defaults to 1 MB

    /**
     * Creates a machine type holder.
     *
     * @param type The type string.
     * @param defaultMemory The default memory for this type.
     * @param defaultTosVersion The default TOS version.
     * @param hasBlitter True if the machine has a blitter chip.
     */
    MachineType(String type, int defaultMemory, TOS defaultTosVersion, boolean hasBlitter) {
        this.type = type;
        this.kbMemory = defaultMemory;
        this.tosVersion = defaultTosVersion;
        this.hasBlitter = hasBlitter;
    }
}

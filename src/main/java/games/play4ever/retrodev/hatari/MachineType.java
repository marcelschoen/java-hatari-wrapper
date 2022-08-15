package games.play4ever.retrodev.hatari;

/**
 * Defines which type of Atari system to emulate.
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

    public TOS tosVersion = TOS.tos106;
    public String type;
    public boolean hasBlitter = false;
    public int kbMemory = 1024; // defaults to 1 MB

    MachineType(String type, int defaultMemory, TOS defaultTosVersion, boolean hasBlitter) {
        this.type = type;
        this.kbMemory = defaultMemory;
        this.tosVersion = defaultTosVersion;
        this.hasBlitter = hasBlitter;
    }
}

package games.play4ever.retrodev.hatari;

/**
 * Possible values to use for the memory.
 *
 * @author Marcel Schoen
 */
public enum Memory {

    kb512(512),
    mb1(1024),
    mb2(2 * 1024),
    mb4(4 * 1024),
    mb8(8 * 1024);

    /** The number of kilobytes */
    public int kbMemory;

    /**
     * Creates a memory amount holder.
     *
     * @param kbMemory The amount of kilobytes.
     */
    Memory(int kbMemory) {
        this.kbMemory = kbMemory;
    }
}

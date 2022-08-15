package games.play4ever.retrodev.hatari;

/**
 * Possible values to use for the memory.
 */
public enum Memory {
    kb256(256),
    kb512(512),
    mb1(1024),
    mb2(2 * 1024),
    mb4(4 * 1024),
    mb8(8 * 1024);

    public int kbMemory;

    Memory(int kbMemory) {
        this.kbMemory = kbMemory;
    }
}

package games.play4ever.retrodev.hatari;

/**
 * Atari ST screen mode.
 *
 * @author Marcel Schoen
 */
public enum ScreenMode {
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

    public String value;

    ScreenMode(String value) {
        this.value = value;
    }
}

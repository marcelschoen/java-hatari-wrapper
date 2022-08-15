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
    medium("mid"),
    /**
     * High resolution (640x400 / monochrome)
     */
    high("high");

    /** The current value ("low", "mid" or "high") */
    public String value;

    /**
     * Creates a screen mode.
     *
     * @param value The string value.
     */
    ScreenMode(String value) {
        this.value = value;
    }
}

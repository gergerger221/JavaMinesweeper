package util;

import java.awt.*;

public class UiTheme {
    public static final Color DARK_BG = new Color(248, 250, 252);
    public static final Color DARK_CELL_BORDER = new Color(203, 213, 225);
    public static final Color DARK_HEADER_BG = new Color(30, 41, 59);
    public static final Color DARK_HEADER_FG = new Color(241, 245, 249);
    public static final Color DARK_CELL_UNOPENED = new Color(226, 232, 240);
    public static final Color DARK_CELL_REVEALED = new Color(255, 255, 255);
    public static final Color DARK_PRIMARY = new Color(79, 70, 229);
    public static final Color DARK_TEXT = new Color(15, 23, 42);
    public static final Color LIGHT_BG = new Color(248, 250, 252);
    public static final Color HEADER_TEXT_BRIGHT = new Color(226, 232, 240);
    public static final Color CELL_TEXT_COLOR = new Color(100, 116, 139);

    // Additional cell colors used by game board
    public static final Color LIGHT_CELL_REVEALED_BG = new Color(255, 255, 255);
    public static final Color DARK_CELL_BG = new Color(30, 41, 59);
    public static final Color COLOR_FLAG_FG = new Color(239, 68, 68);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_CELL = new Font("Segoe UI", Font.BOLD, 14);

    // Scaling factor for responsive UI
    private static double scaleFactor = 1.0;
    private static boolean scaleInitialized = false;

    public static void initializeScaling() {
        if (scaleInitialized)
            return;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;

        // Base reference: 1920x1080 = 1.0 scale
        double baseArea = 1920.0 * 1080.0;
        double currentArea = screenWidth * screenHeight;
        double areaRatio = Math.sqrt(currentArea / baseArea);

        // Increased minimum scale for larger UI (1.2 instead of 1.0)
        scaleFactor = Math.max(1.2, Math.min(2.5, areaRatio));

        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        if (dpi > 96) {
            scaleFactor *= (dpi / 96.0) * 0.9;
            scaleFactor = Math.max(1.2, Math.min(2.5, scaleFactor));
        }

        scaleInitialized = true;
    }

    public static double getScale() {
        if (!scaleInitialized)
            initializeScaling();
        return scaleFactor;
    }

    public static int scale(int value) {
        if (!scaleInitialized)
            initializeScaling();
        return (int) Math.round(value * scaleFactor);
    }

    public static Font scaledFont(String name, int style, int baseSize) {
        if (!scaleInitialized)
            initializeScaling();
        int scaledSize = (int) Math.round(baseSize * scaleFactor);
        return new Font(name, style, scaledSize);
    }

    public static Font scaledFont(int style, int baseSize) {
        return scaledFont("Segoe UI", style, baseSize);
    }

    public static Dimension scaledDimension(int width, int height) {
        return new Dimension(scale(width), scale(height));
    }

    public static Insets scaledInsets(int top, int left, int bottom, int right) {
        return new Insets(scale(top), scale(left), scale(bottom), scale(right));
    }

    // Pre-scaled common font sizes - INCREASED for better visibility
    public static Font fontMenuTitle() {
        return scaledFont(Font.BOLD, 56); // was 40
    }

    public static Font fontSetupTitle() {
        return scaledFont(Font.BOLD, 36); // was 26
    }

    public static Font fontSectionLabel() {
        return scaledFont(Font.BOLD, 22); // was 16
    }

    public static Font fontButton() {
        return scaledFont(Font.BOLD, 20); // was 16
    }

    public static Font fontButtonSmall() {
        return scaledFont(Font.BOLD, 18); // was 14
    }

    public static Font fontButtonTiny() {
        return scaledFont(Font.BOLD, 16); // was 13
    }

    public static Font fontCreditsTitle() {
        return scaledFont(Font.BOLD, 44); // was 32
    }

    public static Font fontCreditsText() {
        return scaledFont(Font.PLAIN, 22); // was 16
    }

    public static Font fontCreditsLink() {
        return scaledFont(Font.PLAIN, 18); // was 14
    }

    public static Font fontHeader() {
        return scaledFont(Font.BOLD, 18); // was 14
    }
}

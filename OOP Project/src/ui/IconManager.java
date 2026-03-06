package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IconManager {

    private final BufferedImage flagImg;
    private final BufferedImage mineImg;
    private final BufferedImage starburstImg;

    private final Map<Integer, ImageIcon> flagIconCache = new HashMap<>();
    private final Map<Integer, ImageIcon> mineIconCache = new HashMap<>();
    private final Map<Integer, ImageIcon> starburstIconCache = new HashMap<>();

    public IconManager() {
        flagImg = loadImage("icons/flag.png");
        mineImg = loadImage("icons/mine.png");
        starburstImg = loadImage("icons/starburst.png");
    }

    public ImageIcon getFlagIcon(int size) {
        return getCachedIcon(flagIconCache, flagImg, size);
    }

    public ImageIcon getMineIcon(int size) {
        return getCachedIcon(mineIconCache, mineImg, size);
    }

    public BufferedImage getMineImage() {
        return mineImg;
    }

    public ImageIcon getStarburstIcon(int size) {
        return getCachedIcon(starburstIconCache, starburstImg, size);
    }

    private static BufferedImage loadImage(String relativePath) {
        BufferedImage fromResource = loadFromResource(relativePath);
        if (fromResource != null)
            return fromResource;

        BufferedImage fromFile = loadFromFile(relativePath);
        if (fromFile != null)
            return fromFile;

        System.err.println(
                "[IconManager] Could not load icon: " + relativePath + " (cwd=" + System.getProperty("user.dir") + ")");
        return null;
    }

    private static BufferedImage loadFromResource(String relativePath) {
        String resourcePath = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        try (InputStream is = IconManager.class.getResourceAsStream(resourcePath)) {
            if (is == null)
                return null;
            return ImageIO.read(is);
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage loadFromFile(String relativePath) {
        try {
            File direct = new File(relativePath);
            if (direct.exists())
                return ImageIO.read(direct);

            File cwd = new File(System.getProperty("user.dir"));
            File inCwd = new File(cwd, relativePath);
            if (inCwd.exists())
                return ImageIO.read(inCwd);

            File parent = cwd.getParentFile();
            if (parent != null) {
                File inParent = new File(parent, relativePath);
                if (inParent.exists())
                    return ImageIO.read(inParent);
            }

            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private static ImageIcon getCachedIcon(Map<Integer, ImageIcon> cache, BufferedImage img, int size) {
        ImageIcon icon = cache.get(size);
        if (icon != null)
            return icon;
        icon = scaleIcon(img, size);
        cache.put(size, icon);
        return icon;
    }

    private static ImageIcon scaleIcon(BufferedImage img, int size) {
        if (img == null)
            return null;
        Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}

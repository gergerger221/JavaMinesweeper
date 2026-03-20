package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Random;
import java.awt.image.BufferedImage;

public class MinePatternPanel extends JPanel {
    private final Random rnd = new Random();
    private final ArrayList<MineSprite> sprites = new ArrayList<>();
    private int lastW = -1;
    private int lastH = -1;
    private BufferedImage mineImg;

    private class MineSprite {
        final int x;
        final int y;
        final int size;
        final double angleRad;
        final float alpha;

        MineSprite(int x, int y, int size, double angleRad, float alpha) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.angleRad = angleRad;
            this.alpha = alpha;
        }
    }

    public MinePatternPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        loadMineImage();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                regenerateIfNeeded(true);
                repaint();
            }
        });
    }

    private void loadMineImage() {
        // Try to load from IconManager
        IconManager iconManager = new IconManager();
        mineImg = iconManager.getMineImage();
    }

    private void regenerateIfNeeded(boolean force) {
        int w = getWidth();
        int h = getHeight();
        if (!force && w == lastW && h == lastH)
            return;
        if (w <= 0 || h <= 0)
            return;

        lastW = w;
        lastH = h;
        sprites.clear();

        int area = w * h;
        int target = Math.max(30, Math.min(120, area / 12000));

        int padding = 6;
        int placed = 0;
        int attempts = 0;
        int maxAttempts = target * 80;
        while (placed < target && attempts < maxAttempts) {
            attempts++;

            int size = 16 + rnd.nextInt(70);
            if (rnd.nextInt(10) == 0) {
                size = 90 + rnd.nextInt(60);
            }

            int radius = Math.max(1, (size / 2) + padding);
            if (radius * 2 >= w || radius * 2 >= h)
                continue;

            int x = radius + rnd.nextInt(Math.max(1, w - (radius * 2)));
            int y = radius + rnd.nextInt(Math.max(1, h - (radius * 2)));

            boolean collides = false;
            for (MineSprite other : sprites) {
                int otherRadius = Math.max(1, (other.size / 2) + padding);
                int dx = x - other.x;
                int dy = y - other.y;
                int minDist = radius + otherRadius;
                if ((dx * dx) + (dy * dy) < (minDist * minDist)) {
                    collides = true;
                    break;
                }
            }
            if (collides)
                continue;

            double angle = (rnd.nextDouble() * Math.PI * 2.0);
            float alpha = 0.14f + (rnd.nextFloat() * 0.08f);
            sprites.add(new MineSprite(x, y, size, angle, alpha));
            placed++;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setColor(util.UiTheme.LIGHT_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            regenerateIfNeeded(false);

            if (mineImg == null) {
                loadMineImage();
            }
            if (mineImg == null)
                return;

            for (MineSprite s : sprites) {
                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, s.alpha));

                double scaleX = (double) s.size / (double) mineImg.getWidth();
                double scaleY = (double) s.size / (double) mineImg.getHeight();

                AffineTransform at = new AffineTransform();
                at.translate(s.x, s.y);
                at.rotate(s.angleRad);
                at.scale(scaleX, scaleY);
                at.translate(-mineImg.getWidth() / 2.0, -mineImg.getHeight() / 2.0);

                g2.drawImage(mineImg, at, null);
                g2.setComposite(old);
            }
        } finally {
            g2.dispose();
        }
    }
}

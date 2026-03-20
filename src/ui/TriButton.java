package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.RenderingHints;

public class TriButton extends JButton {
    private final int row;
    private final int col;
    private final boolean pointsUp;
    private Shape triShape;

    public TriButton(int row, int col) {
        this.row = row;
        this.col = col;
        this.pointsUp = ((row + col) % 2 == 0);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        triShape = createTriangle(w, h);

        // Fill background
        g2.setColor(getBackground());
        g2.fill(triShape);

        // Draw cell outline
        g2.setColor(util.UiTheme.DARK_CELL_BORDER);
        g2.draw(triShape);

        // Draw text
        String text = getText();
        if (text != null && !text.isEmpty()) {
            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();
            int yOffset = pointsUp ? 5 : -5;
            g2.drawString(text, (w - textWidth) / 2, (h + textHeight) / 2 - 2 + yOffset);
        }

        // Draw icon
        Icon icon = getIcon();
        if (icon != null) {
            int iconX = (w - icon.getIconWidth()) / 2;
            int iconY = (h - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, iconX, iconY);
        }

        g2.dispose();
    }

    private Shape createTriangle(int w, int h) {
        Path2D path = new Path2D.Double();
        int padding = Math.max(3, (int) Math.round(Math.min(w, h) * 0.10));
        if (pointsUp) {
            path.moveTo(w / 2.0, padding);
            path.lineTo(w - padding, h - padding);
            path.lineTo(padding, h - padding);
        } else {
            path.moveTo(padding, padding);
            path.lineTo(w - padding, padding);
            path.lineTo(w / 2.0, h - padding);
        }
        path.closePath();
        return path;
    }

    @Override
    public boolean contains(int x, int y) {
        if (triShape == null) {
            return super.contains(x, y);
        }
        return triShape.contains(x, y);
    }
}

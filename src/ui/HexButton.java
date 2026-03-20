package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.RenderingHints;

public class HexButton extends JButton {
    private final int row;
    private final int col;
    private Shape hexShape;

    public HexButton(int row, int col) {
        this.row = row;
        this.col = col;
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
        hexShape = createHexagon(w, h);

        // Fill background
        g2.setColor(getBackground());
        g2.fill(hexShape);

        // Draw cell outline
        g2.setColor(util.UiTheme.DARK_CELL_BORDER);
        g2.draw(hexShape);

        // Draw text
        String text = getText();
        if (text != null && !text.isEmpty()) {
            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();
            g2.drawString(text, (w - textWidth) / 2, (h + textHeight) / 2 - 2);
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

    private Shape createHexagon(int w, int h) {
        int pad = 1;
        double x0 = pad;
        double x1 = w * 0.25;
        double x2 = w * 0.75;
        double x3 = w - pad;
        double y0 = pad;
        double y1 = h * 0.5;
        double y2 = h - pad;

        Path2D path = new Path2D.Double();
        path.moveTo(x1, y0);
        path.lineTo(x2, y0);
        path.lineTo(x3, y1);
        path.lineTo(x2, y2);
        path.lineTo(x1, y2);
        path.lineTo(x0, y1);
        path.closePath();
        return path;
    }

    @Override
    public boolean contains(int x, int y) {
        if (hexShape == null) {
            return super.contains(x, y);
        }
        return hexShape.contains(x, y);
    }
}

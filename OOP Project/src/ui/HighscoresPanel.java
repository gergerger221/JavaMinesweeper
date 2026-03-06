package ui;

import database.DatabaseManager;
import database.Highscore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

public class HighscoresPanel extends JPanel {
    
    private final DatabaseManager dbManager;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private JTabbedPane tabbedPane;
    private final IconManager iconManager = new IconManager();
    
    // Mine pattern background variables
    private final Random rnd = new Random();
    private final List<MineSprite> sprites = new ArrayList<>();
    private int lastW = -1;
    private int lastH = -1;
    
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
    
    public HighscoresPanel(CardLayout cardLayout, JPanel cardPanel) {
        super(new BorderLayout());
        this.cardLayout = cardLayout;
        this.cardPanel = cardPanel;
        this.dbManager = DatabaseManager.getInstance();
        
        setOpaque(false);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                regenerateIfNeeded(true);
                repaint();
            }
        });
        
        buildHighscoresUI();
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
            float alpha = 0.08f + (rnd.nextFloat() * 0.06f);
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
            g2.setColor(new Color(240, 249, 255));
            g2.fillRect(0, 0, getWidth(), getHeight());
            
            regenerateIfNeeded(false);
            
            // Draw mine sprites using actual mine.png image
            BufferedImage mineImg = iconManager.getMineImage();
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
    
    private void buildHighscoresUI() {
        // Title panel with modern styling
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(25, 0, 25, 0));
        
        JLabel titleLabel = new JLabel("🏆 Highscores");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new Color(30, 41, 59));
        titlePanel.add(titleLabel);
        
        add(titlePanel, BorderLayout.NORTH);
        
        // Highscores content with modern card design and gradient background
        JPanel contentPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                // Create subtle gradient background
                GradientPaint gradient = new GradientPaint(
                    0f, 0f, new Color(248, 250, 252),
                    0f, getHeight(), new Color(240, 249, 255)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        contentPanel.setOpaque(true);
        contentPanel.setBorder(new EmptyBorder(0, 30, 20, 30));
        
        // Modern tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabbedPane.setBackground(new Color(248, 250, 252));
        tabbedPane.setForeground(new Color(30, 41, 59));
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create tabs for each difficulty
        tabbedPane.addTab("Easy", createHighscoreTable("Easy"));
        tabbedPane.addTab("Medium", createHighscoreTable("Medium"));
        tabbedPane.addTab("Hard", createHighscoreTable("Hard"));
        
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);
        
        // Bottom panel with back button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JButton backButton = new JButton("Back to Menu");
        applyButtonStyle(backButton, 14);
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "MENU"));
        bottomPanel.add(backButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    public void refreshHighscores() {
        // Remove all tabs and recreate them with fresh data
        tabbedPane.removeAll();
        
        // Recreate tabs with updated data
        tabbedPane.addTab("Easy", createHighscoreTable("Easy"));
        tabbedPane.addTab("Medium", createHighscoreTable("Medium"));
        tabbedPane.addTab("Hard", createHighscoreTable("Hard"));
        
        // Refresh the UI
        tabbedPane.revalidate();
        tabbedPane.repaint();
    }
    
    private JScrollPane createHighscoreTable(String difficulty) {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        
        // Get highscores for this difficulty
        List<Highscore> highscores = dbManager.getTopHighscores(difficulty, 10);
        
        // Create table model
        String[] columnNames = {"Rank", "Player", "Time", "Date"};
        Object[][] data = new Object[highscores.size()][4];
        
        for (int i = 0; i < highscores.size(); i++) {
            Highscore hs = highscores.get(i);
            data[i][0] = i + 1;
            data[i][1] = hs.getPlayerName();
            data[i][2] = hs.getFormattedTime();
            data[i][3] = hs.getFormattedDate();
        }
        
        // Create table
        JTable table = new JTable(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
            
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                // Transparent alternating row colors to show mine background
                if (!component.getBackground().equals(getSelectionBackground())) {
                    component.setBackground(row % 2 == 0 ? 
                        new Color(255, 255, 255, 160) : new Color(248, 252, 255, 160)); // Semi-transparent
                }
                return component;
            }
        };
        
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(35);
        table.setOpaque(false); // Make table transparent
        table.setBackground(new Color(255, 255, 255, 180)); // Semi-transparent white
        table.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 16));
        table.getTableHeader().setBackground(new Color(30, 41, 59, 220)); // Semi-transparent dark header
        table.getTableHeader().setForeground(new Color(99, 102, 241));
        table.getTableHeader().setPreferredSize(new Dimension(0, 50));
        table.getTableHeader().setBorder(new EmptyBorder(15, 10, 15, 10));
        table.getTableHeader().setOpaque(false); // Make header transparent
        
        table.setSelectionBackground(new Color(99, 102, 241));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(71, 85, 105)); // Darker gridlines for visibility
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Center align columns
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(new CenterRenderer());
        }
        
        // Adjust column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);  // Rank
        table.getColumnModel().getColumn(1).setPreferredWidth(150); // Player
        table.getColumnModel().getColumn(2).setPreferredWidth(80);  // Time
        table.getColumnModel().getColumn(3).setPreferredWidth(120); // Date
        
        // Create scroll pane with transparent design
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(new Color(0, 0, 0, 0));
        
        // Add empty message if no highscores
        if (highscores.isEmpty()) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            emptyPanel.setOpaque(false);
            JLabel emptyLabel = new JLabel("No highscores yet. Be the first to complete a game!");
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            emptyLabel.setForeground(new Color(107, 114, 128));
            emptyPanel.add(emptyLabel);
            return new JScrollPane(emptyPanel);
        }
        
        return scrollPane;
    }
    
    private void applyButtonStyle(JButton button, int fontSize) {
        button.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 22, 10, 22));
        button.setBackground(new Color(129, 140, 248));
        button.setForeground(Color.WHITE);
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(99, 102, 241));
                }
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(129, 140, 248));
            }
            
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(71, 85, 105));
                }
            }
            
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (button.isEnabled() && button.getBounds().contains(evt.getPoint())) {
                    button.setBackground(new Color(99, 102, 241));
                }
            }
        });
    }
    
    // Custom renderer for centering table cells
    private static class CenterRenderer extends DefaultTableCellRenderer {
        public CenterRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setBorder(new EmptyBorder(5, 5, 5, 5));
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                // Modern color scheme for better visibility
                if (column == 0) { // Rank column
                    setForeground(new Color(99, 102, 241));
                } else if (column == 1) { // Player name column
                    setForeground(new Color(30, 41, 59));
                } else if (column == 2) { // Time column
                    setForeground(new Color(16, 185, 129));
                } else { // Date column
                    setForeground(new Color(107, 114, 128));
                }
            } else {
                setForeground(Color.WHITE);
            }
            
            return component;
        }
    }
}

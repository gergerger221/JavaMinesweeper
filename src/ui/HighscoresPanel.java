package ui;

import database.DatabaseManager;
import database.Highscore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
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
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel emptyLabel;
    private JPanel tableArea;
    private CardLayout tableAreaLayout;
    private JComboBox<String> difficultyFilter;
    private JComboBox<String> modeFilter;
    private JComboBox<String> livesFilter;
    private JComboBox<String> sortFilter;
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

    private void installScrollBarStyle(JScrollPane scrollPane) {
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        bar.setPreferredSize(new Dimension(14, Integer.MAX_VALUE));
        bar.setUnitIncrement(16);
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(99, 102, 241, 200);
                this.trackColor = new Color(226, 232, 240, 140);
                this.trackHighlightColor = new Color(226, 232, 240, 160);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                int inset = 2;
                g2.fillRoundRect(thumbBounds.x + inset, thumbBounds.y + inset, thumbBounds.width - (inset * 2),
                        thumbBounds.height - (inset * 2), 10, 10);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(trackColor);
                int inset = 2;
                g2.fillRoundRect(trackBounds.x + inset, trackBounds.y + inset, trackBounds.width - (inset * 2),
                        trackBounds.height - (inset * 2), 10, 10);
                g2.dispose();
            }
        });
    }

    private JComboBox<String> createDropdownSelector(String[] items, String prototypeValue) {
        final Color violet = new Color(99, 102, 241);
        final Color violetDark = new Color(79, 70, 229);

        JComboBox<String> comboBox = new JComboBox<>(items) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int arc = 14;
                    g2.setColor(violet);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                    g2.setColor(violetDark);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };

        comboBox.setOpaque(false);
        comboBox.setForeground(Color.WHITE);
        comboBox.setBackground(violet);
        comboBox.setFont(new Font("Segoe UI", Font.BOLD, 13));
        comboBox.setPrototypeDisplayValue(prototypeValue);
        comboBox.setBorder(new EmptyBorder(7, 12, 7, 10));

        final int arrowW = 26;
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                // Do not paint the default LAF background (removes inner grey rectangle/lines).
            }

            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        try {
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(Color.WHITE);
                            int w = getWidth();
                            int h = getHeight();
                            int cx = w / 2;
                            int cy = h / 2;
                            int size = 8;
                            Polygon p = new Polygon(
                                    new int[] { cx - size / 2, cx + size / 2, cx },
                                    new int[] { cy - 2, cy - 2, cy + size / 2 },
                                    3);
                            g2.fillPolygon(p);
                        } finally {
                            g2.dispose();
                        }
                    }
                };
                b.setOpaque(false);
                b.setContentAreaFilled(false);
                b.setBorderPainted(false);
                b.setFocusPainted(false);
                b.setPreferredSize(new Dimension(arrowW, 28));
                b.setMinimumSize(new Dimension(arrowW, 28));
                return b;
            }

            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = new BasicComboPopup(comboBox) {
                    @Override
                    protected JScrollPane createScroller() {
                        JScrollPane sp = super.createScroller();
                        sp.setBorder(BorderFactory.createEmptyBorder());
                        sp.getViewport().setOpaque(true);
                        sp.getViewport().setBackground(Color.WHITE);
                        installScrollBarStyle(sp);
                        return sp;
                    }
                };
                popup.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                        new EmptyBorder(6, 6, 6, 6)
                ));
                popup.setOpaque(true);
                popup.setBackground(Color.WHITE);
                return popup;
            }
        });

        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                if (index == -1) {
                    lbl.setOpaque(false);
                    lbl.setBorder(new EmptyBorder(0, 2, 0, 32));
                    lbl.setForeground(Color.WHITE);
                    lbl.setBackground(new Color(0, 0, 0, 0));
                } else {
                    lbl.setOpaque(true);
                    lbl.setBorder(new EmptyBorder(9, 12, 9, 12));
                    if (isSelected) {
                        lbl.setBackground(new Color(237, 233, 254));
                        lbl.setForeground(new Color(67, 56, 202));
                    } else {
                        lbl.setBackground(Color.WHITE);
                        lbl.setForeground(new Color(30, 41, 59));
                    }
                }
                return lbl;
            }
        });

        FontMetrics fm = comboBox.getFontMetrics(comboBox.getFont());
        int textW = fm.stringWidth(prototypeValue);
        int padW = 12 + 12;
        int prefW = textW + padW + arrowW + 18;
        Dimension pref = comboBox.getPreferredSize();
        comboBox.setPreferredSize(new Dimension(Math.max(prefW, 138), Math.max(pref.height, 36)));

        return comboBox;
    }

    private JPanel createFilterBlock(String labelText, JComboBox<String> comboBox) {
        JPanel block = new JPanel();
        block.setOpaque(true);
        block.setBackground(new Color(255, 255, 255, 180));
        block.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(148, 163, 184), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(51, 65, 85));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.add(lbl);
        block.add(Box.createVerticalStrut(6));
        block.add(comboBox);
        return block;
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

        JLabel titleLabel = new JLabel("Highscores");
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
                        0f, getHeight(), new Color(240, 249, 255));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        contentPanel.setOpaque(true);
        contentPanel.setBorder(new EmptyBorder(0, 30, 20, 30));

        JPanel filtersPanel = new JPanel(new GridLayout(1, 4, 24, 0));
        filtersPanel.setOpaque(false);
        filtersPanel.setBorder(new EmptyBorder(16, 40, 14, 40));

        difficultyFilter = createDropdownSelector(new String[] { "All", "Easy", "Medium", "Hard" }, "Medium");

        modeFilter = createDropdownSelector(new String[] { "All", "SQUARE", "HEXAGONAL" }, "HEXAGONAL");

        livesFilter = createDropdownSelector(new String[] { "All", "1", "3" }, "All");

        sortFilter = createDropdownSelector(new String[] { "Best Time", "Worst Time", "Newest" }, "Worst Time");

        filtersPanel.add(createFilterBlock("Difficulty", difficultyFilter));
        filtersPanel.add(createFilterBlock("Mode", modeFilter));
        filtersPanel.add(createFilterBlock("Lives", livesFilter));
        filtersPanel.add(createFilterBlock("Sort", sortFilter));

        contentPanel.add(filtersPanel, BorderLayout.NORTH);

        String[] columnNames = { "Rank", "Player", "Difficulty", "Mode", "Lives", "Time", "Date" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                if (!component.getBackground().equals(getSelectionBackground())) {
                    component.setBackground(row % 2 == 0 ? new Color(255, 255, 255, 185)
                            : new Color(248, 250, 252, 185));
                }
                return component;
            }
        };

        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(34);
        table.setOpaque(false);
        table.setBackground(new Color(255, 255, 255, 180));
        table.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(30, 41, 59, 220));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 44));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());

        table.setSelectionBackground(new Color(79, 70, 229));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(148, 163, 184));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setRowMargin(1);
        table.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        table.setFillsViewportHeight(true);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(new CenterRenderer());
        }

        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(new Color(0, 0, 0, 0));

        installScrollBarStyle(scrollPane);

        emptyLabel = new JLabel("No highscores yet.");
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        emptyLabel.setForeground(new Color(71, 85, 105));

        tableAreaLayout = new CardLayout();
        tableArea = new JPanel(tableAreaLayout);
        tableArea.setOpaque(false);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setOpaque(false);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(99, 102, 241), 2, true),
                new EmptyBorder(10, 10, 10, 10)
        ));
        tableCard.add(scrollPane, BorderLayout.CENTER);

        JPanel emptyCard = new JPanel(new BorderLayout());
        emptyCard.setOpaque(false);
        emptyCard.setBorder(new EmptyBorder(40, 10, 40, 10));
        emptyCard.add(emptyLabel, BorderLayout.CENTER);

        tableArea.add(tableCard, "TABLE");
        tableArea.add(emptyCard, "EMPTY");

        contentPanel.add(tableArea, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        difficultyFilter.addActionListener(e -> refreshHighscores());
        modeFilter.addActionListener(e -> refreshHighscores());
        livesFilter.addActionListener(e -> refreshHighscores());
        sortFilter.addActionListener(e -> refreshHighscores());

        refreshHighscores();

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
        if (tableModel == null) {
            return;
        }

        String difficulty = (difficultyFilter == null) ? "All" : String.valueOf(difficultyFilter.getSelectedItem());
        String mode = (modeFilter == null) ? "All" : String.valueOf(modeFilter.getSelectedItem());
        String livesStr = (livesFilter == null) ? "All" : String.valueOf(livesFilter.getSelectedItem());
        Integer lives = null;
        if (livesStr != null && !livesStr.equalsIgnoreCase("All")) {
            try {
                lives = Integer.valueOf(Integer.parseInt(livesStr));
            } catch (Exception ex) {
                lives = null;
            }
        }

        DatabaseManager.SortOrder sort = DatabaseManager.SortOrder.TIME_ASC;
        String sortSel = (sortFilter == null) ? "Best Time" : String.valueOf(sortFilter.getSelectedItem());
        if ("Worst Time".equalsIgnoreCase(sortSel)) {
            sort = DatabaseManager.SortOrder.TIME_DESC;
        } else if ("Newest".equalsIgnoreCase(sortSel)) {
            sort = DatabaseManager.SortOrder.DATE_DESC;
        }

        List<Highscore> highscores = dbManager.getHighscores(difficulty, mode, lives, 100, sort);

        tableModel.setRowCount(0);
        if (highscores == null || highscores.isEmpty()) {
            if (tableAreaLayout != null) {
                tableAreaLayout.show(tableArea, "EMPTY");
            }
            revalidate();
            repaint();
            return;
        }

        if (tableAreaLayout != null) {
            tableAreaLayout.show(tableArea, "TABLE");
        }

        for (int i = 0; i < highscores.size(); i++) {
            Highscore hs = highscores.get(i);
            tableModel.addRow(new Object[] { i + 1, hs.getPlayerName(), hs.getDifficulty(), hs.getBoardMode(),
                    hs.getLives(), hs.getFormattedTime(), hs.getFormattedDate() });
        }

        revalidate();
        repaint();
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
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                // Modern color scheme for better visibility
                if (column == 0) { // Rank column
                    setForeground(new Color(99, 102, 241));
                } else if (column == 1) { // Player name column
                    setForeground(new Color(30, 41, 59));
                } else if (column == 2) { // Difficulty
                    setForeground(new Color(37, 99, 235));
                } else if (column == 3) { // Mode
                    setForeground(new Color(124, 58, 237));
                } else if (column == 4) { // Lives
                    setForeground(new Color(234, 88, 12));
                } else if (column == 5) { // Time column
                    setForeground(new Color(16, 185, 129));
                } else if (column == 6) { // Date column
                    setForeground(new Color(107, 114, 128));
                } else {
                    setForeground(new Color(51, 65, 85));
                }
            } else {
                setForeground(Color.WHITE);
            }

            return component;
        }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setOpaque(true);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(148, 163, 184)));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(new Color(30, 41, 59, 235));
            if (column == 0) {
                setForeground(new Color(129, 140, 248));
            } else if (column == 1) {
                setForeground(new Color(226, 232, 240));
            } else if (column == 2) {
                setForeground(new Color(147, 197, 253));
            } else if (column == 3) {
                setForeground(new Color(196, 181, 253));
            } else if (column == 4) {
                setForeground(new Color(253, 186, 116));
            } else if (column == 5) {
                setForeground(new Color(134, 239, 172));
            } else {
                setForeground(new Color(203, 213, 225));
            }
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(148, 163, 184)));
            return this;
        }
    }
}

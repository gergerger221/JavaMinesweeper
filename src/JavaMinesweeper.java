import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import database.DatabaseManager;
import database.Highscore;
import database.PlayerSlot;
import ui.HighscoresPanel;
import ui.IconManager;
import util.UiTheme;
import util.AnimationManager;

enum BoardShape {
    SQUARE, HEXAGONAL
}

class JavaMinesweeper extends JFrame {

    @SuppressWarnings("unused")
    private static class HexAxial {
        @SuppressWarnings("unused")
        final int q;
        @SuppressWarnings("unused")
        final int r;

        HexAxial(int q, int r) {
            this.q = q;
            this.r = r;
        }
    }

    // even-q vertical layout (flat-top): odd columns are shifted down
    @SuppressWarnings("unused")
    private HexAxial hexOffsetToAxial(int row, int col) {
        int q = col;
        int r = row - ((col + (col & 1)) / 2);
        return new HexAxial(q, r);
    }

    @SuppressWarnings("unused")
    private Point hexAxialToOffset(int q, int r) {
        int col = q;
        int row = r + ((q + (q & 1)) / 2);
        return new Point(row, col);
    }

    private class MinePatternPanel extends JPanel {

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

        MinePatternPanel(LayoutManager layout) {
            super(layout);
            setOpaque(true);

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    regenerateIfNeeded(true);
                    repaint();
                }
            });
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
                g2.setColor(UiTheme.LIGHT_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());

                regenerateIfNeeded(false);

                java.awt.image.BufferedImage mineImg = iconManager.getMineImage();
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

    private class HexButton extends JButton {
        private final int row;
        private final int col;
        private Shape hexShape;

        HexButton(int row, int col) {
            this.row = row;
            this.col = col;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
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

            // Draw border
            g2.setColor(UiTheme.DARK_CELL_BORDER);
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

    private class HexagonalLayoutManager implements LayoutManager2 {
        private int cellWidth = 40;
        private int cellHeight = 40;
        private int horizontalSpacing = 0;
        private int verticalSpacing = 0;

        void setCellSize(int cell) {
            cellWidth = cell;
            cellHeight = Math.max(18, (int) Math.round(cell * 0.866));
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0.5f;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0.5f;
        }

        @Override
        public void invalidateLayout(Container target) {
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return calculateSize(parent);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return calculateSize(parent);
        }

        private Dimension calculateSize(Container parent) {
            int componentCount = parent.getComponentCount();
            if (componentCount == 0)
                return new Dimension(0, 0);

            double stepX = (cellWidth * 0.75) + horizontalSpacing;
            int stepY = cellHeight + verticalSpacing;
            int w = (int) Math.ceil((size - 1) * stepX + cellWidth);
            int h = (int) Math.ceil((size - 1) * stepY + cellHeight + (cellHeight / 2.0));
            return new Dimension(w, h);
        }

        @Override
        public void layoutContainer(Container parent) {
            int componentCount = parent.getComponentCount();
            if (componentCount == 0)
                return;

            int parentWidth = parent.getWidth();
            int parentHeight = parent.getHeight();

            Dimension prefSize = calculateSize(parent);
            int startX = (parentWidth - prefSize.width) / 2;
            int startY = (parentHeight - prefSize.height) / 2;

            double stepX = (cellWidth * 0.75) + horizontalSpacing;
            int stepY = cellHeight + verticalSpacing;

            for (int i = 0; i < componentCount; i++) {
                Component comp = parent.getComponent(i);
                if (!(comp instanceof HexButton))
                    continue;
                HexButton hb = (HexButton) comp;

                int r = hb.row;
                int c = hb.col;

                int x = startX + (int) Math.round(c * stepX);
                int y = startY + r * stepY + ((c % 2 == 0) ? 0 : (cellHeight / 2));

                comp.setBounds(x, y, cellWidth, cellHeight);
            }
        }
    }

    private static Color darken(Color c, float amount) {
        if (c == null)
            return null;
        amount = Math.max(0f, Math.min(1f, amount));
        int r = Math.max(0, Math.round(c.getRed() * (1f - amount)));
        int g = Math.max(0, Math.round(c.getGreen() * (1f - amount)));
        int b = Math.max(0, Math.round(c.getBlue() * (1f - amount)));
        return new Color(r, g, b, c.getAlpha());
    }

    private int size;
    private int minesCount;
    private BoardShape boardShape = BoardShape.SQUARE;
    private int lives = 1;
    private int maxLives = 1;

    private JButton[][] buttons;
    private boolean[][] mines;
    private boolean[][] revealed;
    private boolean[][] flagged;

    private JPanel topPanel;
    private JLabel title;
    private JLabel statusLabel;
    private JLabel timerLabel;
    private JLabel livesLabel;
    private JButton settingsButton;
    private JPanel settingsGlassPane;
    private JPanel playerSlotsGlassPane;
    private JPanel endGameGlassPane;
    private JPanel confirmGlassPane;
    private boolean endGameOverlayHidden;
    private boolean lastEndGameWin;
    private String lastEndGameMessage;
    private AWTEventListener endGameRestoreListener;
    private boolean gamePaused;
    private long pauseStartMs = 0L;
    private JPanel boardContainer;
    private JPanel boardPanel;

    private HexagonalLayoutManager hexLayout;

    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JPanel menuPanel;
    private JLabel menuUserIndicator;
    private JPanel creditsPanel;
    private JPanel gamePanel;
    private JPanel setupPanel;
    private JPanel playerSlotsPanel;
    private HighscoresPanel highscoresPanel;

    private Integer currentUserId;
    private String currentUsername;
    private boolean guestMode;

    private boolean showCurrentBadgeOnSlots;
    private Runnable rebuildPlayerSlotsList;

    private boolean firstMove;
    private boolean gameActive;
    private int lastExplodedRow = -1;
    private int lastExplodedCol = -1;

    private String currentDifficulty = "";
    private long runStartMs = 0L;
    private Timer runTimer;

    private final IconManager iconManager = new IconManager();
    private ImageIcon flagIcon;
    private ImageIcon mineIcon;
    private ImageIcon starburstIcon;

    public JavaMinesweeper() {
        // Initialize UI scaling based on screen size
        UiTheme.initializeScaling();

        setTitle("JavaMinesweeper");
        setSize(UiTheme.scale(960), UiTheme.scale(720));
        setMinimumSize(new Dimension(UiTheme.scale(720), UiTheme.scale(600)));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(true);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        add(cardPanel, BorderLayout.CENTER);

        buildMenuUI();
        buildCreditsUI();
        buildSetupUI();
        buildGameUI();
        buildPlayerSlotsUI();
        buildHighscoresUI();

        cardPanel.add(menuPanel, "MENU");
        cardPanel.add(creditsPanel, "CREDITS");
        cardPanel.add(setupPanel, "SETUP");
        cardPanel.add(gamePanel, "GAME");
        cardPanel.add(highscoresPanel, "HIGHSCORES");
        cardPanel.add(playerSlotsPanel, "PLAYER_SLOTS");

        setLocationRelativeTo(null);
        showCurrentBadgeOnSlots = false;
        showPlayerSlots();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }

    private void buildPlayerSlotsUI() {
        playerSlotsPanel = new MinePatternPanel(new BorderLayout());

        JLabel title = new JLabel("Select Player");
        title.setFont(UiTheme.fontMenuTitle());
        title.setForeground(new Color(15, 23, 42));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(UiTheme.scale(18), UiTheme.scale(18), UiTheme.scale(10), UiTheme.scale(18)));
        header.add(title, BorderLayout.CENTER);
        playerSlotsPanel.add(header, BorderLayout.NORTH);

        DatabaseManager db = DatabaseManager.getInstance();

        JPanel listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(new EmptyBorder(UiTheme.scale(8), UiTheme.scale(60), UiTheme.scale(8), UiTheme.scale(60)));
        installScrollBarStyle(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(UiTheme.scale(28));
        scroll.getVerticalScrollBar().setBlockIncrement(UiTheme.scale(160));
        playerSlotsPanel.add(scroll, BorderLayout.CENTER);

        final JPanel slotsGlass = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(new Color(15, 23, 42, 130));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        slotsGlass.setOpaque(false);
        slotsGlass.setVisible(false);
        slotsGlass.setFocusable(true);
        slotsGlass.setFocusTraversalKeysEnabled(false);

        slotsGlass.addMouseListener(new MouseAdapter() {
        });
        slotsGlass.addMouseMotionListener(new MouseMotionAdapter() {
        });
        slotsGlass.addMouseWheelListener(e -> {
            e.consume();
        });
        slotsGlass.addKeyListener(new KeyAdapter() {
        });

        playerSlotsGlassPane = slotsGlass;

        final java.util.function.BiConsumer<JPanel, Boolean> showSlotsOverlay = (dialog, visible) -> {
            if (visible.booleanValue()) {
                setGlassPane(playerSlotsGlassPane);
                slotsGlass.removeAll();
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 1;
                gbc.weighty = 1;
                gbc.anchor = GridBagConstraints.CENTER;
                slotsGlass.add(dialog, gbc);
                slotsGlass.revalidate();
                slotsGlass.repaint();
                slotsGlass.setVisible(true);
                slotsGlass.requestFocusInWindow();
            } else {
                slotsGlass.setVisible(false);
                slotsGlass.removeAll();
                if (settingsGlassPane != null) {
                    setGlassPane(settingsGlassPane);
                }
            }
        };

        final Runnable[] rebuildListRef = new Runnable[1];
        rebuildListRef[0] = () -> {
            listPanel.removeAll();
            List<PlayerSlot> slots = db.getPlayerSlots();

            if (slots == null || slots.isEmpty()) {
                JLabel empty = new JLabel("No players yet. Click New to create one.");
                empty.setFont(UiTheme.scaledFont(Font.PLAIN, 14));
                empty.setForeground(new Color(71, 85, 105));
                empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                listPanel.add(Box.createVerticalStrut(UiTheme.scale(20)));
                listPanel.add(empty);
                listPanel.add(Box.createVerticalGlue());
            } else {
                for (PlayerSlot slot : slots) {
                    boolean isCurrent = (showCurrentBadgeOnSlots && currentUserId != null
                            && currentUserId.intValue() == slot.getId() && !guestMode);

                    JPanel rowOuter = new JPanel(new BorderLayout());
                    rowOuter.setOpaque(false);
                    rowOuter.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTheme.scale(78)));

                    JPanel row = new JPanel(new BorderLayout(UiTheme.scale(12), 0));
                    row.setOpaque(true);
                    row.setBackground(new Color(255, 255, 255, 215));
                    row.setBorder(new CompoundBorder(
                            new LineBorder(isCurrent ? new Color(99, 102, 241) : new Color(120, 145, 190),
                                    isCurrent ? 3 : 2, true),
                            new EmptyBorder(UiTheme.scale(14), UiTheme.scale(16), UiTheme.scale(14), UiTheme.scale(16))
                    ));
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTheme.scale(72)));

                    JLabel name = new JLabel(slot.getUsername());
                    name.setFont(UiTheme.scaledFont(Font.BOLD, 16));
                    name.setForeground(new Color(15, 23, 42));
                    row.add(name, BorderLayout.WEST);

                    if (isCurrent) {
                        JLabel currentBadge = new JLabel("Current");
                        currentBadge.setFont(UiTheme.scaledFont(Font.BOLD, 12));
                        currentBadge.setForeground(new Color(99, 102, 241));
                        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.scale(10), 0));
                        badgeWrap.setOpaque(false);
                        badgeWrap.add(currentBadge);
                        row.add(badgeWrap, BorderLayout.CENTER);
                    }

                    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.scale(8), 0));
                    actions.setOpaque(false);

                    JButton use = new JButton("Use");
                    applyPrimaryButtonStyle(use, UiTheme.scale(14));
                    use.setRolloverEnabled(true);
                    makeHoverMoreNoticeable(use);
                    use.addActionListener(e -> {
                        currentUserId = Integer.valueOf(slot.getId());
                        currentUsername = slot.getUsername();
                        guestMode = false;
                        showMenu();
                    });

                    JButton rename = new JButton("Rename");
                    applyPrimaryButtonStyle(rename, UiTheme.scale(14));
                    rename.setRolloverEnabled(true);
                    makeHoverMoreNoticeable(rename);
                    rename.addActionListener(e -> {
                        JPanel dialog = new JPanel(new BorderLayout(UiTheme.scale(12), UiTheme.scale(12)));
                        dialog.setBackground(new Color(255, 255, 255, 245));
                        dialog.setBorder(new CompoundBorder(
                                new LineBorder(new Color(99, 102, 241), 2, true),
                                new EmptyBorder(UiTheme.scale(16), UiTheme.scale(18), UiTheme.scale(14),
                                        UiTheme.scale(18))));
                        dialog.setPreferredSize(UiTheme.scaledDimension(420, 210));

                        JLabel dlgTitle = new JLabel("Rename Player");
                        dlgTitle.setFont(UiTheme.scaledFont(Font.BOLD, 18));
                        dlgTitle.setForeground(new Color(15, 23, 42));

                        JLabel hint = new JLabel("New name:");
                        hint.setFont(UiTheme.scaledFont(Font.BOLD, 13));
                        hint.setForeground(new Color(51, 65, 85));

                        JTextField input = new JTextField(slot.getUsername());
                        input.setFont(UiTheme.scaledFont(Font.PLAIN, 14));
                        input.setBorder(new CompoundBorder(
                                new LineBorder(new Color(148, 163, 184), 1, true),
                                new EmptyBorder(UiTheme.scale(8), UiTheme.scale(10), UiTheme.scale(8),
                                        UiTheme.scale(10))));

                        JLabel error = new JLabel(" ");
                        error.setFont(UiTheme.scaledFont(Font.BOLD, 12));
                        error.setForeground(new Color(220, 38, 38));

                        JPanel body = new JPanel();
                        body.setOpaque(false);
                        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
                        body.add(hint);
                        body.add(Box.createVerticalStrut(UiTheme.scale(8)));
                        body.add(input);
                        body.add(Box.createVerticalStrut(UiTheme.scale(8)));
                        body.add(error);

                        JButton cancel = new JButton("Cancel");
                        applyPrimaryButtonStyle(cancel, UiTheme.scale(14));
                        cancel.setRolloverEnabled(true);
                        makeHoverMoreNoticeable(cancel);

                        JButton confirm = new JButton("Rename");
                        applyPrimaryButtonStyle(confirm, UiTheme.scale(14));
                        confirm.setRolloverEnabled(true);
                        makeHoverMoreNoticeable(confirm);

                        cancel.addActionListener(ev -> showSlotsOverlay.accept(dialog, Boolean.FALSE));
                        confirm.addActionListener(ev -> {
                            String newName = input.getText();
                            newName = (newName == null) ? "" : newName.trim();
                            if (newName.isBlank()) {
                                error.setText("Player name cannot be empty.");
                                return;
                            }
                            boolean ok = db.renamePlayerSlot(slot.getId(), newName);
                            if (!ok) {
                                String err = db.getLastErrorMessage();
                                String msg;
                                if (err != null && (err.toLowerCase().contains("unique")
                                        || err.toLowerCase().contains("constraint")
                                        || err.toLowerCase().contains("already"))) {
                                    msg = "That username is already taken.";
                                } else {
                                    msg = (err == null || err.isBlank())
                                            ? "Could not rename player."
                                            : ("Could not rename player: " + err);
                                }
                                error.setText(msg);
                                return;
                            }
                            if (currentUserId != null && currentUserId.intValue() == slot.getId()) {
                                currentUsername = newName;
                                guestMode = false;
                            }
                            if (highscoresPanel != null) {
                                highscoresPanel.refreshHighscores();
                            }
                            showSlotsOverlay.accept(dialog, Boolean.FALSE);
                            rebuildListRef[0].run();
                        });

                        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.scale(10), 0));
                        buttons.setOpaque(false);
                        buttons.add(cancel);
                        buttons.add(confirm);

                        dialog.add(dlgTitle, BorderLayout.NORTH);
                        dialog.add(body, BorderLayout.CENTER);
                        dialog.add(buttons, BorderLayout.SOUTH);

                        showSlotsOverlay.accept(dialog, Boolean.TRUE);
                        SwingUtilities.invokeLater(() -> input.requestFocusInWindow());
                    });

                    JButton delete = new JButton("Delete");
                    applyPrimaryButtonStyle(delete, UiTheme.scale(14));
                    delete.putClientProperty("customHover", Boolean.TRUE);
                    Color dangerBg = new Color(220, 38, 38);
                    delete.setBackground(dangerBg);
                    delete.putClientProperty("baseBg", dangerBg);
                    delete.setRolloverEnabled(true);
                    makeHoverMoreNoticeable(delete);
                    delete.addActionListener(e -> {
                        JPanel dialog = new JPanel(new BorderLayout(UiTheme.scale(12), UiTheme.scale(12)));
                        dialog.setBackground(new Color(255, 255, 255, 245));
                        dialog.setBorder(new CompoundBorder(
                                new LineBorder(new Color(99, 102, 241), 2, true),
                                new EmptyBorder(UiTheme.scale(16), UiTheme.scale(18), UiTheme.scale(14),
                                        UiTheme.scale(18))));
                        dialog.setPreferredSize(UiTheme.scaledDimension(460, 220));

                        JLabel dlgTitle = new JLabel("Delete Player");
                        dlgTitle.setFont(UiTheme.scaledFont(Font.BOLD, 18));
                        dlgTitle.setForeground(new Color(15, 23, 42));

                        JLabel msg1 = new JLabel(
                                "Delete player '" + slot.getUsername() + "' and ALL their highscores?");
                        msg1.setFont(UiTheme.scaledFont(Font.BOLD, 13));
                        msg1.setForeground(new Color(51, 65, 85));

                        JLabel msg2 = new JLabel("This cannot be undone.");
                        msg2.setFont(UiTheme.scaledFont(Font.PLAIN, 13));
                        msg2.setForeground(new Color(71, 85, 105));

                        JLabel error = new JLabel(" ");
                        error.setFont(UiTheme.scaledFont(Font.BOLD, 12));
                        error.setForeground(new Color(220, 38, 38));

                        JPanel body = new JPanel();
                        body.setOpaque(false);
                        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
                        body.add(msg1);
                        body.add(Box.createVerticalStrut(UiTheme.scale(8)));
                        body.add(msg2);
                        body.add(Box.createVerticalStrut(UiTheme.scale(10)));
                        body.add(error);

                        JButton cancel = new JButton("Cancel");
                        applyPrimaryButtonStyle(cancel, UiTheme.scale(14));
                        cancel.setRolloverEnabled(true);
                        makeHoverMoreNoticeable(cancel);

                        JButton confirm = new JButton("Delete");
                        applyPrimaryButtonStyle(confirm, UiTheme.scale(14));
                        confirm.putClientProperty("customHover", Boolean.TRUE);
                        Color dangerBg2 = new Color(220, 38, 38);
                        confirm.setBackground(dangerBg2);
                        confirm.putClientProperty("baseBg", dangerBg2);
                        confirm.setRolloverEnabled(true);
                        makeHoverMoreNoticeable(confirm);

                        cancel.addActionListener(ev -> showSlotsOverlay.accept(dialog, Boolean.FALSE));
                        confirm.addActionListener(ev -> {
                            boolean ok = db.deletePlayerSlot(slot.getId());
                            if (!ok) {
                                String err = db.getLastErrorMessage();
                                String msg = (err == null || err.isBlank()) ? "Could not delete player."
                                        : ("Could not delete player: " + err);
                                error.setText(msg);
                                return;
                            }
                            if (currentUserId != null && currentUserId.intValue() == slot.getId()) {
                                currentUserId = null;
                                currentUsername = null;
                                guestMode = true;
                            }
                            if (highscoresPanel != null) {
                                highscoresPanel.refreshHighscores();
                            }
                            showSlotsOverlay.accept(dialog, Boolean.FALSE);
                            rebuildListRef[0].run();
                        });

                        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.scale(10), 0));
                        buttons.setOpaque(false);
                        buttons.add(cancel);
                        buttons.add(confirm);

                        dialog.add(dlgTitle, BorderLayout.NORTH);
                        dialog.add(body, BorderLayout.CENTER);
                        dialog.add(buttons, BorderLayout.SOUTH);

                        showSlotsOverlay.accept(dialog, Boolean.TRUE);
                    });

                    actions.add(use);
                    actions.add(rename);
                    actions.add(delete);
                    row.add(actions, BorderLayout.EAST);

                    rowOuter.add(row, BorderLayout.CENTER);
                    listPanel.add(rowOuter);
                    listPanel.add(Box.createVerticalStrut(UiTheme.scale(2)));
                }
            }

            listPanel.revalidate();
            listPanel.repaint();
        };

        rebuildListRef[0].run();
        rebuildPlayerSlotsList = rebuildListRef[0];

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(UiTheme.scale(10), UiTheme.scale(60), UiTheme.scale(18), UiTheme.scale(60)));

        JButton back = new JButton("Back");
        applyPrimaryButtonStyle(back, UiTheme.scale(16));
        back.setRolloverEnabled(true);
        makeHoverMoreNoticeable(back);
        back.addActionListener(e -> showMenu());

        JButton newBtn = new JButton("New");
        applyPrimaryButtonStyle(newBtn, UiTheme.scale(16));
        newBtn.setRolloverEnabled(true);
        makeHoverMoreNoticeable(newBtn);
        newBtn.addActionListener(e -> {
            JPanel dialog = new JPanel(new BorderLayout(UiTheme.scale(12), UiTheme.scale(12)));
            dialog.setBackground(new Color(255, 255, 255, 245));
            dialog.setBorder(new CompoundBorder(
                    new LineBorder(new Color(99, 102, 241), 2, true),
                    new EmptyBorder(UiTheme.scale(16), UiTheme.scale(18), UiTheme.scale(14), UiTheme.scale(18))));
            dialog.setPreferredSize(UiTheme.scaledDimension(420, 210));

            JLabel dlgTitle = new JLabel("New Player");
            dlgTitle.setFont(UiTheme.scaledFont(Font.BOLD, 18));
            dlgTitle.setForeground(new Color(15, 23, 42));

            JLabel hint = new JLabel("Enter player name:");
            hint.setFont(UiTheme.scaledFont(Font.BOLD, 13));
            hint.setForeground(new Color(51, 65, 85));

            JTextField input = new JTextField();
            input.setFont(UiTheme.scaledFont(Font.PLAIN, 14));
            input.setBorder(new CompoundBorder(
                    new LineBorder(new Color(148, 163, 184), 1, true),
                    new EmptyBorder(UiTheme.scale(8), UiTheme.scale(10), UiTheme.scale(8), UiTheme.scale(10))));

            JLabel error = new JLabel(" ");
            error.setFont(UiTheme.scaledFont(Font.BOLD, 12));
            error.setForeground(new Color(220, 38, 38));

            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.add(hint);
            body.add(Box.createVerticalStrut(UiTheme.scale(8)));
            body.add(input);
            body.add(Box.createVerticalStrut(UiTheme.scale(8)));
            body.add(error);

            JButton cancel = new JButton("Cancel");
            applyPrimaryButtonStyle(cancel, UiTheme.scale(14));
            cancel.setRolloverEnabled(true);
            makeHoverMoreNoticeable(cancel);

            JButton confirm = new JButton("Create");
            applyPrimaryButtonStyle(confirm, UiTheme.scale(14));
            confirm.setRolloverEnabled(true);
            makeHoverMoreNoticeable(confirm);

            cancel.addActionListener(ev -> showSlotsOverlay.accept(dialog, Boolean.FALSE));
            confirm.addActionListener(ev -> {
                String name = input.getText();
                name = (name == null) ? "" : name.trim();
                if (name.isBlank()) {
                    error.setText("Player name cannot be empty.");
                    return;
                }
                Integer id = db.createPlayerSlot(name);
                if (id == null) {
                    String err = db.getLastErrorMessage();
                    String msg;
                    if (err != null && (err.toLowerCase().contains("unique")
                            || err.toLowerCase().contains("constraint")
                            || err.toLowerCase().contains("already"))) {
                        msg = "That username is already taken.";
                    } else {
                        msg = (err == null || err.isBlank()) ? "Could not create player."
                                : ("Could not create player: " + err);
                    }
                    error.setText(msg);
                    return;
                }
                showSlotsOverlay.accept(dialog, Boolean.FALSE);
                rebuildListRef[0].run();
            });

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.scale(10), 0));
            buttons.setOpaque(false);
            buttons.add(cancel);
            buttons.add(confirm);

            dialog.add(dlgTitle, BorderLayout.NORTH);
            dialog.add(body, BorderLayout.CENTER);
            dialog.add(buttons, BorderLayout.SOUTH);

            showSlotsOverlay.accept(dialog, Boolean.TRUE);
            SwingUtilities.invokeLater(() -> input.requestFocusInWindow());
        });

        footer.add(back, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.scale(10), 0));
        right.setOpaque(false);
        right.add(newBtn);
        footer.add(right, BorderLayout.EAST);

        playerSlotsPanel.add(footer, BorderLayout.SOUTH);
    }

    private void showPlayerSlots() {
        if (showCurrentBadgeOnSlots && rebuildPlayerSlotsList != null) {
            rebuildPlayerSlotsList.run();
        }
        cardLayout.show(cardPanel, "PLAYER_SLOTS");
    }

    private String getCurrentUserDisplayName() {
        if (guestMode || currentUserId == null) {
            return "None";
        }
        if (currentUsername == null || currentUsername.isBlank()) {
            return "Player";
        }
        return currentUsername;
    }

    private void updateMenuUserIndicator() {
        if (menuUserIndicator == null) {
            return;
        }
        menuUserIndicator.setText("Current User: " + getCurrentUserDisplayName());
    }

    private void buildMenuUI() {
        menuPanel = new MinePatternPanel(new GridBagLayout());

        JLabel menuTitle = new JLabel("JavaMinesweeper");
        menuTitle.setFont(UiTheme.fontMenuTitle());
        menuTitle.setForeground(new Color(15, 23, 42));

        menuUserIndicator = new JLabel();
        menuUserIndicator.setFont(UiTheme.scaledFont(Font.BOLD, 14));
        menuUserIndicator.setForeground(new Color(30, 41, 59));
        menuUserIndicator.setOpaque(true);
        menuUserIndicator.setBackground(new Color(255, 255, 255, 220));
        menuUserIndicator.setBorder(new CompoundBorder(
                new LineBorder(new Color(99, 102, 241), 2, true),
                new EmptyBorder(UiTheme.scale(8), UiTheme.scale(14), UiTheme.scale(8), UiTheme.scale(14))
        ));
        updateMenuUserIndicator();

        JButton playerSlotsButton = new JButton("Change User");
        applyPrimaryButtonStyle(playerSlotsButton, UiTheme.scale(16));
        playerSlotsButton.addActionListener(e -> {
            showCurrentBadgeOnSlots = true;
            showPlayerSlots();
        });

        JButton playButton = new JButton("Play");
        applyPrimaryButtonStyle(playButton, UiTheme.scale(16));
        playButton.addActionListener(e -> showSetup());

        JButton highscoresButton = new JButton("Highscores");
        applyPrimaryButtonStyle(highscoresButton, UiTheme.scale(16));
        highscoresButton.addActionListener(e -> showHighscores());

        JLabel creditsLink = new JLabel("Credits");
        creditsLink.setFont(UiTheme.fontCreditsLink());
        Color creditsDefault = new Color(71, 85, 105);
        Color creditsHover = new Color(30, 41, 59);
        creditsLink.setForeground(creditsDefault);
        creditsLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        creditsLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCredits();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                creditsLink.setText("<html><u>Credits</u></html>");
                creditsLink.setForeground(creditsHover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                creditsLink.setText("Credits");
                creditsLink.setForeground(creditsDefault);
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = UiTheme.scaledInsets(0, 0, 22, 0);
        menuPanel.add(menuTitle, gbc);

        gbc.gridy = 1;
        gbc.insets = UiTheme.scaledInsets(0, 0, 14, 0);
        menuPanel.add(menuUserIndicator, gbc);

        gbc.gridy = 2;
        gbc.insets = UiTheme.scaledInsets(0, 0, 10, 0);
        menuPanel.add(playButton, gbc);

        gbc.gridy = 3;
        gbc.insets = UiTheme.scaledInsets(0, 0, 10, 0);
        menuPanel.add(playerSlotsButton, gbc);

        gbc.gridy = 4;
        gbc.insets = UiTheme.scaledInsets(0, 0, 10, 0);
        menuPanel.add(highscoresButton, gbc);

        gbc.gridy = 5;
        gbc.insets = UiTheme.scaledInsets(0, 0, 0, 0);
        menuPanel.add(creditsLink, gbc);
    }

    private void buildSetupUI() {
        setupPanel = new MinePatternPanel(new GridBagLayout());

        ImageIcon mine = iconManager.getMineIcon(UiTheme.scale(22));
        ImageIcon flag = iconManager.getFlagIcon(UiTheme.scale(18));

        JLabel title = new JLabel("Game Setup");
        title.setFont(UiTheme.fontSetupTitle());
        title.setForeground(new Color(15, 23, 42));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, UiTheme.scale(10), 0));
        titleRow.setOpaque(false);
        titleRow.add(new JLabel(mine));
        titleRow.add(title);
        titleRow.add(new JLabel(mine));

        Dimension optionSize = UiTheme.scaledDimension(160, 44);
        Color hoverBg = new Color(129, 140, 248);
        Color normalBg = new Color(165, 180, 252);
        Color selectedBg = UiTheme.DARK_PRIMARY;

        java.util.function.BiConsumer<JButton[], JButton> selectInGroup = (group, selected) -> {
            for (JButton b : group) {
                if (b == null)
                    continue;
                boolean isSel = (b == selected);
                b.putClientProperty("selectedOption", Boolean.valueOf(isSel));
                b.setBackground(isSel ? selectedBg : normalBg);
                b.setForeground(isSel ? Color.WHITE : new Color(80, 80, 80));
                // Update flag visibility
                JLabel leftFlag = (JLabel) b.getClientProperty("leftFlag");
                JLabel rightFlag = (JLabel) b.getClientProperty("rightFlag");
                if (leftFlag != null && rightFlag != null) {
                    leftFlag.setIcon(isSel ? flag : null);
                    rightFlag.setIcon(isSel ? flag : null);
                }
            }
        };

        java.util.function.Function<String, JPanel> createOptionRow = (text) -> {
            JButton button = new JButton(text);
            applyPrimaryButtonStyle(button, UiTheme.scale(16));
            button.setBackground(normalBg);

            JLabel leftFlag = new JLabel();
            leftFlag.setPreferredSize(UiTheme.scaledDimension(24, 24));
            JLabel rightFlag = new JLabel();
            rightFlag.setPreferredSize(UiTheme.scaledDimension(24, 24));

            JPanel row = new JPanel(new BorderLayout(UiTheme.scale(10), 0));
            row.setOpaque(false);
            row.add(leftFlag, BorderLayout.WEST);
            row.add(button, BorderLayout.CENTER);
            row.add(rightFlag, BorderLayout.EAST);

            // Store flag references in button for access by selectInGroup
            button.putClientProperty("leftFlag", leftFlag);
            button.putClientProperty("rightFlag", rightFlag);

            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    boolean isSel = Boolean.TRUE.equals(button.getClientProperty("selectedOption"));
                    if (!isSel) {
                        button.setBackground(hoverBg);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    boolean isSel = Boolean.TRUE.equals(button.getClientProperty("selectedOption"));
                    button.setBackground(isSel ? selectedBg : normalBg);
                }
            });

            button.setPreferredSize(optionSize);
            button.setMinimumSize(optionSize);
            button.setMaximumSize(optionSize);

            return row;
        };

        JLabel shapeLabel = new JLabel("Board Shape");
        shapeLabel.setFont(UiTheme.fontSectionLabel());
        shapeLabel.setForeground(new Color(15, 23, 42));

        JPanel squareRow = createOptionRow.apply("Square");
        JButton squareBtn = (JButton) squareRow.getComponent(1);
        JPanel hexRow = createOptionRow.apply("Hexagonal");
        JButton hexBtn = (JButton) hexRow.getComponent(1);
        JButton[] shapeGroup = new JButton[] { squareBtn, hexBtn };

        squareBtn.addActionListener(e -> {
            boardShape = BoardShape.SQUARE;
            selectInGroup.accept(shapeGroup, squareBtn);
        });
        hexBtn.addActionListener(e -> {
            boardShape = BoardShape.HEXAGONAL;
            selectInGroup.accept(shapeGroup, hexBtn);
        });

        boardShape = BoardShape.SQUARE;
        selectInGroup.accept(shapeGroup, squareBtn);

        JLabel difficultyLabel = new JLabel("Difficulty");
        difficultyLabel.setFont(UiTheme.fontSectionLabel());
        difficultyLabel.setForeground(new Color(15, 23, 42));

        JPanel easyRow = createOptionRow.apply("Easy");
        JButton easyBtn = (JButton) easyRow.getComponent(1);
        JPanel mediumRow = createOptionRow.apply("Medium");
        JButton mediumBtn = (JButton) mediumRow.getComponent(1);
        JPanel hardRow = createOptionRow.apply("Hard");
        JButton hardBtn = (JButton) hardRow.getComponent(1);
        JButton[] diffGroup = new JButton[] { easyBtn, mediumBtn, hardBtn };

        easyBtn.addActionListener(e -> {
            currentDifficulty = "Easy";
            selectInGroup.accept(diffGroup, easyBtn);
        });
        mediumBtn.addActionListener(e -> {
            currentDifficulty = "Medium";
            selectInGroup.accept(diffGroup, mediumBtn);
        });
        hardBtn.addActionListener(e -> {
            currentDifficulty = "Hard";
            selectInGroup.accept(diffGroup, hardBtn);
        });

        currentDifficulty = "Easy";
        selectInGroup.accept(diffGroup, easyBtn);

        JLabel livesLabelSetup = new JLabel("Lives");
        livesLabelSetup.setFont(UiTheme.fontSectionLabel());
        livesLabelSetup.setForeground(new Color(15, 23, 42));

        JPanel oneLifeRow = createOptionRow.apply("1");
        JButton oneLifeBtn = (JButton) oneLifeRow.getComponent(1);
        JPanel threeLivesRow = createOptionRow.apply("3");
        JButton threeLivesBtn = (JButton) threeLivesRow.getComponent(1);
        JButton[] livesGroup = new JButton[] { oneLifeBtn, threeLivesBtn };

        oneLifeBtn.addActionListener(e -> {
            maxLives = 1;
            selectInGroup.accept(livesGroup, oneLifeBtn);
        });
        threeLivesBtn.addActionListener(e -> {
            maxLives = 3;
            selectInGroup.accept(livesGroup, threeLivesBtn);
        });

        maxLives = 1;
        selectInGroup.accept(livesGroup, oneLifeBtn);

        JButton startButton = new JButton("Start Game");
        applyPrimaryButtonStyle(startButton, UiTheme.scale(16));
        startButton.setPreferredSize(UiTheme.scaledDimension(200, 48));
        startButton.addActionListener(e -> {
            lives = maxLives;
            startGameWithSettings();
        });

        JButton backButton = new JButton("Back");
        applyPrimaryButtonStyle(backButton, UiTheme.scale(14));
        backButton.setPreferredSize(optionSize);
        backButton.addActionListener(e -> showMenu());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = UiTheme.scaledInsets(0, 0, 18, 0);
        setupPanel.add(titleRow, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 8, 0);
        setupPanel.add(shapeLabel, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 10, 0);
        setupPanel.add(squareRow, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 18, 0);
        setupPanel.add(hexRow, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 8, 0);
        setupPanel.add(difficultyLabel, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 10, 0);
        setupPanel.add(easyRow, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 10, 0);
        setupPanel.add(mediumRow, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 18, 0);
        setupPanel.add(hardRow, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 8, 0);
        setupPanel.add(livesLabelSetup, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 10, 0);
        setupPanel.add(oneLifeRow, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 18, 0);
        setupPanel.add(threeLivesRow, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 10, 0);
        setupPanel.add(startButton, gbc);

        gbc.gridy++;
        gbc.insets = UiTheme.scaledInsets(0, 0, 0, 0);
        setupPanel.add(backButton, gbc);
    }

    private void buildCreditsUI() {
        creditsPanel = new MinePatternPanel(new GridBagLayout());

        JLabel title = new JLabel("Credits");
        title.setFont(UiTheme.fontCreditsTitle());
        title.setForeground(new Color(15, 23, 42));

        JLabel group = new JLabel("Group: JerJerKings");
        group.setFont(UiTheme.fontCreditsText());
        group.setForeground(new Color(15, 23, 42));

        JLabel members = new JLabel(
                "<html>Members:<br>1) Joven Sanchez<br>2) Andrew Llaneta<br>3) Terd Sionzon<br>4) Chriz Almonte </html>");
        members.setFont(UiTheme.fontCreditsText());
        members.setForeground(new Color(15, 23, 42));

        JButton backButton = new JButton("Back");
        applyPrimaryButtonStyle(backButton, UiTheme.scale(14));
        backButton.addActionListener(e -> showMenu());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = UiTheme.scaledInsets(0, 0, 18, 0);
        creditsPanel.add(title, gbc);

        gbc.gridy = 1;
        gbc.insets = UiTheme.scaledInsets(0, 0, 10, 0);
        creditsPanel.add(group, gbc);

        gbc.gridy = 2;
        gbc.insets = UiTheme.scaledInsets(0, 0, 18, 0);
        creditsPanel.add(members, gbc);

        gbc.gridy = 3;
        gbc.insets = UiTheme.scaledInsets(0, 0, 0, 0);
        creditsPanel.add(backButton, gbc);
    }

    @SuppressWarnings("unused")
    private JPanel createDifficultyRow(String text, Dimension size, ImageIcon flagIcon,
            Color hoverBg, Color normalBg, Runnable action) {
        JButton button = new JButton();
        button.putClientProperty("customHover", Boolean.TRUE);
        applyPrimaryButtonStyle(button, UiTheme.scale(16));
        button.setBackground(normalBg);
        button.setText(text);

        JLabel leftFlag = new JLabel();
        leftFlag.setPreferredSize(UiTheme.scaledDimension(24, 24));
        JLabel rightFlag = new JLabel();
        rightFlag.setPreferredSize(UiTheme.scaledDimension(24, 24));

        JPanel row = new JPanel(new BorderLayout(UiTheme.scale(10), 0));
        row.setOpaque(false);
        row.add(leftFlag, BorderLayout.WEST);
        row.add(button, BorderLayout.CENTER);
        row.add(rightFlag, BorderLayout.EAST);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                leftFlag.setIcon(flagIcon);
                rightFlag.setIcon(flagIcon);
                button.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                leftFlag.setIcon(null);
                rightFlag.setIcon(null);
                button.setBackground(normalBg);
            }
        });

        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.addActionListener(e -> action.run());

        return row;
    }

    private void startGameWithSettings() {
        if (currentDifficulty.equals("Easy")) {
            startNewGame(8, 10);
        } else if (currentDifficulty.equals("Medium")) {
            startNewGame(12, 25);
        } else if (currentDifficulty.equals("Hard")) {
            startNewGame(16, 45);
        }
        cardLayout.show(cardPanel, "GAME");
    }

    private void buildHighscoresUI() {
        highscoresPanel = new HighscoresPanel(cardLayout, cardPanel);
    }

    private void buildGameUI() {
        gamePanel = new MinePatternPanel(new BorderLayout());

        topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(UiTheme.scale(10), UiTheme.scale(12), UiTheme.scale(10), UiTheme.scale(12)));
        topPanel.setBackground(UiTheme.DARK_HEADER_BG);

        title = new JLabel("JavaMinesweeper");
        title.setFont(UiTheme.scaledFont(Font.BOLD, 18));
        title.setForeground(UiTheme.DARK_HEADER_FG);
        topPanel.add(title, BorderLayout.WEST);

        statusLabel = new JLabel("");
        statusLabel.setFont(UiTheme.fontHeader());
        statusLabel.setForeground(UiTheme.HEADER_TEXT_BRIGHT);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        timerLabel = new JLabel("Time: 00:00");
        timerLabel.setFont(UiTheme.fontHeader());
        timerLabel.setForeground(UiTheme.HEADER_TEXT_BRIGHT);
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        livesLabel = new JLabel("Lives: 1");
        livesLabel.setFont(UiTheme.fontHeader());
        livesLabel.setForeground(new Color(244, 63, 94));
        livesLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel centerHeader = new JPanel();
        centerHeader.setOpaque(false);
        centerHeader.setLayout(new BoxLayout(centerHeader, BoxLayout.Y_AXIS));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        livesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerHeader.add(statusLabel);
        centerHeader.add(Box.createVerticalStrut(2));
        centerHeader.add(timerLabel);
        centerHeader.add(Box.createVerticalStrut(2));
        centerHeader.add(livesLabel);
        topPanel.add(centerHeader, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.scale(10), 0));
        actionsPanel.setOpaque(false);

        // Load and scale icons for buttons - larger size for better visibility
        int iconSize = UiTheme.scale(28);
        ImageIcon settingIcon = null;
        Image settingImg = iconManager.getSettingImage();
        if (settingImg != null) {
            settingIcon = new ImageIcon(settingImg.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));
        }

        settingsButton = (settingIcon != null) ? new JButton(settingIcon) : new JButton("Settings");
        settingsButton.setToolTipText("Settings");
        applyIconButtonStyle(settingsButton);
        settingsButton.addActionListener(e -> {
            if (!gameActive) {
                if (endGameOverlayHidden) {
                    showEndGameOverlay(lastEndGameWin, lastEndGameMessage);
                }
                return;
            }
            toggleSettingsOverlay();
        });
        actionsPanel.add(settingsButton);

        topPanel.add(actionsPanel, BorderLayout.EAST);
        gamePanel.add(topPanel, BorderLayout.NORTH);

        boardPanel = new JPanel();
        boardPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        boardPanel.setOpaque(false);
        boardPanel.setBackground(new Color(0, 0, 0, 0));

        boardContainer = new SquareBoardContainer(boardPanel);
        boardContainer.setLayout(null);
        boardContainer
                .setBorder(new EmptyBorder(UiTheme.scale(12), UiTheme.scale(12), UiTheme.scale(12), UiTheme.scale(12)));
        boardContainer.setOpaque(false);
        boardContainer.setBackground(new Color(0, 0, 0, 0));

        boardContainer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                boardContainer.doLayout();
                updateCellSizesAndIcons();
            }
        });

        boardContainer.add(boardPanel);
        gamePanel.add(boardContainer, BorderLayout.CENTER);

        buildSettingsOverlay();
    }

    private void installEndGameRestoreListener() {
        if (endGameRestoreListener != null)
            return;
        endGameRestoreListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (!endGameOverlayHidden)
                    return;
                if (!(event instanceof MouseEvent))
                    return;
                MouseEvent me = (MouseEvent) event;
                if (me.getID() != MouseEvent.MOUSE_PRESSED)
                    return;

                // Restore the exact same overlay contents.
                showEndGameOverlay(lastEndGameWin, lastEndGameMessage);
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(endGameRestoreListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void uninstallEndGameRestoreListener() {
        if (endGameRestoreListener == null)
            return;
        Toolkit.getDefaultToolkit().removeAWTEventListener(endGameRestoreListener);
        endGameRestoreListener = null;
    }

    private void buildSettingsOverlay() {
        settingsGlassPane = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 140));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        settingsGlassPane.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UiTheme.DARK_HEADER_BG);
        card.setBorder(new EmptyBorder(UiTheme.scale(18), UiTheme.scale(22), UiTheme.scale(18), UiTheme.scale(22)));

        JLabel titleLabel = new JLabel("Settings");
        titleLabel.setFont(UiTheme.scaledFont(Font.BOLD, 22));
        titleLabel.setForeground(UiTheme.DARK_HEADER_FG);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton resume = new JButton("Resume");
        applyPrimaryButtonStyle(resume, UiTheme.scale(16));
        resume.setAlignmentX(Component.CENTER_ALIGNMENT);
        resume.addActionListener(e -> hideSettingsOverlay());

        JButton retry = new JButton("Retry");
        applyPrimaryButtonStyle(retry, UiTheme.scale(16));
        retry.setAlignmentX(Component.CENTER_ALIGNMENT);
        retry.addActionListener(e -> {
            showConfirmationOverlay(
                    "Confirm Retry",
                    "Are you sure you want to restart the game?\nYour current progress will be lost.",
                    () -> {
                        hideSettingsOverlay();
                        resetGame();
                    },
                    () -> {
                    } // Cancel - do nothing, just hide confirmation
            );
        });

        JButton quit = new JButton("Quit");
        applyPrimaryButtonStyle(quit, UiTheme.scale(16));
        quit.setAlignmentX(Component.CENTER_ALIGNMENT);
        quit.addActionListener(e -> {
            showConfirmationOverlay(
                    "Confirm Quit",
                    "Are you sure you want to quit to the main menu?\nYour current progress will be lost.",
                    () -> {
                        hideSettingsOverlay();
                        showMenu();
                    },
                    () -> {
                    } // Cancel - do nothing, just hide confirmation
            );
        });

        Dimension btnSize = UiTheme.scaledDimension(220, 48);
        resume.setPreferredSize(btnSize);
        resume.setMaximumSize(btnSize);
        retry.setPreferredSize(btnSize);
        retry.setMaximumSize(btnSize);
        quit.setPreferredSize(btnSize);
        quit.setMaximumSize(btnSize);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(UiTheme.scale(14)));
        card.add(resume);
        card.add(Box.createVerticalStrut(UiTheme.scale(10)));
        card.add(retry);
        card.add(Box.createVerticalStrut(UiTheme.scale(10)));
        card.add(quit);

        settingsGlassPane.add(card, new GridBagConstraints());
        settingsGlassPane.setVisible(false);
        setGlassPane(settingsGlassPane);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (settingsGlassPane != null) {
                    settingsGlassPane.revalidate();
                    settingsGlassPane.repaint();
                }
            }
        });
    }

    private void buildEndGameOverlay() {
        endGameGlassPane = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        endGameGlassPane.setOpaque(false);
        endGameGlassPane.setVisible(false);
    }

    private void showEndGameOverlay(boolean win, String message) {
        if (endGameGlassPane == null) {
            buildEndGameOverlay();
        }

        lastEndGameWin = win;
        lastEndGameMessage = message;
        endGameOverlayHidden = false;
        uninstallEndGameRestoreListener();

        endGameGlassPane.removeAll();

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UiTheme.DARK_HEADER_BG);
        card.setBorder(new EmptyBorder(UiTheme.scale(18), UiTheme.scale(22), UiTheme.scale(18), UiTheme.scale(22)));

        JLabel titleLabel = new JLabel(win ? "You Win!" : "Game Over");
        titleLabel.setFont(UiTheme.scaledFont(Font.BOLD, 26));
        titleLabel.setForeground(win ? new Color(34, 197, 94) : new Color(244, 63, 94));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, titleLabel.getPreferredSize().height));

        JLabel msgLabel = new JLabel("<html><div style='text-align:center;'>" + message.replace("\n", "<br>")
                + "</div></html>");
        msgLabel.setFont(UiTheme.scaledFont(Font.PLAIN, 16));
        msgLabel.setForeground(UiTheme.DARK_HEADER_FG);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        msgLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, msgLabel.getPreferredSize().height));

        JButton viewBoard = new JButton("View Board");
        applyPrimaryButtonStyle(viewBoard, UiTheme.scale(16));
        viewBoard.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewBoard.addActionListener(e -> hideEndGameOverlayForViewing());

        JButton retry = new JButton("Retry");
        applyPrimaryButtonStyle(retry, UiTheme.scale(16));
        retry.setAlignmentX(Component.CENTER_ALIGNMENT);
        retry.addActionListener(e -> {
            hideEndGameOverlay(true);
            resetGame();
        });

        JButton quit = new JButton("Quit");
        applyPrimaryButtonStyle(quit, UiTheme.scale(16));
        quit.setAlignmentX(Component.CENTER_ALIGNMENT);
        quit.addActionListener(e -> {
            hideEndGameOverlay(true);
            showMenu();
        });

        Dimension btnSize = UiTheme.scaledDimension(220, 48);
        viewBoard.setPreferredSize(btnSize);
        viewBoard.setMaximumSize(btnSize);
        retry.setPreferredSize(btnSize);
        retry.setMaximumSize(btnSize);
        quit.setPreferredSize(btnSize);
        quit.setMaximumSize(btnSize);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(UiTheme.scale(10)));
        card.add(msgLabel);
        card.add(Box.createVerticalStrut(UiTheme.scale(16)));
        card.add(viewBoard);
        card.add(Box.createVerticalStrut(UiTheme.scale(10)));
        card.add(retry);
        card.add(Box.createVerticalStrut(UiTheme.scale(10)));
        card.add(quit);

        endGameGlassPane.add(card, new GridBagConstraints());

        setGamePaused(true);
        setGlassPane(endGameGlassPane);
        endGameGlassPane.setVisible(true);
        endGameGlassPane.revalidate();
        endGameGlassPane.repaint();
    }

    private void hideEndGameOverlayForViewing() {
        endGameOverlayHidden = true;
        if (endGameGlassPane != null) {
            endGameGlassPane.setVisible(false);
        }
        if (settingsGlassPane != null) {
            setGlassPane(settingsGlassPane);
        }
        // Keep paused/locked since the game is over.
        setGamePaused(true);
        installEndGameRestoreListener();
    }

    private void hideEndGameOverlay(boolean leavingGameEndState) {
        endGameOverlayHidden = false;
        if (endGameGlassPane != null) {
            endGameGlassPane.setVisible(false);
        }
        if (settingsGlassPane != null) {
            setGlassPane(settingsGlassPane);
        }

        uninstallEndGameRestoreListener();

        // Only unpause if we are actually leaving the end-game state (Retry/Quit)
        if (leavingGameEndState) {
            setGamePaused(false);
        } else {
            setGamePaused(true);
        }
    }

    private void showConfirmationOverlay(String title, String message, Runnable onConfirm, Runnable onCancel) {
        if (confirmGlassPane == null) {
            confirmGlassPane = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(0, 0, 0, 160));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            confirmGlassPane.setOpaque(false);
            confirmGlassPane.setVisible(false);
        }

        confirmGlassPane.removeAll();

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UiTheme.DARK_HEADER_BG);
        card.setBorder(new EmptyBorder(UiTheme.scale(18), UiTheme.scale(22), UiTheme.scale(18), UiTheme.scale(22)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiTheme.scaledFont(Font.BOLD, 22));
        titleLabel.setForeground(new Color(244, 63, 94));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, titleLabel.getPreferredSize().height));

        JLabel msgLabel = new JLabel("<html><div style='text-align:center;'>" + message.replace("\n", "<br>")
                + "</div></html>");
        msgLabel.setFont(UiTheme.scaledFont(Font.PLAIN, 14));
        msgLabel.setForeground(UiTheme.DARK_HEADER_FG);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        msgLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, msgLabel.getPreferredSize().height));

        JButton yesButton = new JButton("Yes");
        applyPrimaryButtonStyle(yesButton, UiTheme.scale(16));
        yesButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        yesButton.addActionListener(e -> {
            confirmGlassPane.setVisible(false);
            setGlassPane(settingsGlassPane);
            onConfirm.run();
        });

        JButton noButton = new JButton("No");
        applyPrimaryButtonStyle(noButton, UiTheme.scale(16));
        noButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        noButton.addActionListener(e -> {
            confirmGlassPane.setVisible(false);
            setGlassPane(settingsGlassPane);
            settingsGlassPane.setVisible(true);
            onCancel.run();
        });

        Dimension btnSize = UiTheme.scaledDimension(220, 48);
        yesButton.setPreferredSize(btnSize);
        yesButton.setMaximumSize(btnSize);
        noButton.setPreferredSize(btnSize);
        noButton.setMaximumSize(btnSize);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(UiTheme.scale(10)));
        card.add(msgLabel);
        card.add(Box.createVerticalStrut(UiTheme.scale(16)));
        card.add(yesButton);
        card.add(Box.createVerticalStrut(UiTheme.scale(10)));
        card.add(noButton);

        confirmGlassPane.add(card, new GridBagConstraints());

        setGlassPane(confirmGlassPane);
        confirmGlassPane.setVisible(true);
        confirmGlassPane.revalidate();
        confirmGlassPane.repaint();
    }

    private void toggleSettingsOverlay() {
        if (!gameActive)
            return;
        if (settingsGlassPane == null)
            return;
        if (settingsGlassPane.isVisible()) {
            hideSettingsOverlay();
        } else {
            showSettingsOverlay();
        }
    }

    private void showSettingsOverlay() {
        setGamePaused(true);
        if (settingsGlassPane != null) {
            setGlassPane(settingsGlassPane);
        }
        settingsGlassPane.setVisible(true);
        settingsGlassPane.requestFocusInWindow();
    }

    private void hideSettingsOverlay() {
        if (settingsGlassPane != null) {
            settingsGlassPane.setVisible(false);
        }
        setGamePaused(false);
    }

    private void setGamePaused(boolean paused) {
        if (paused == gamePaused)
            return;
        gamePaused = paused;

        if (paused) {
            pauseStartMs = System.currentTimeMillis();
            if (runTimer != null) {
                runTimer.stop();
            }
        } else {
            if (pauseStartMs != 0L && runStartMs != 0L) {
                long pausedFor = System.currentTimeMillis() - pauseStartMs;
                runStartMs += pausedFor;
            }
            pauseStartMs = 0L;
            if (runTimer != null && gameActive) {
                runTimer.start();
            }
            updateTimerLabel();
        }
    }

    private void makeHoverMoreNoticeable(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!button.isEnabled()) return;
                button.setBackground(darken(button.getBackground(), 0.20f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!button.isEnabled()) return;
                Color baseBg = (Color) button.getClientProperty("baseBg");
                if (baseBg != null) {
                    button.setBackground(baseBg);
                }
            }
        });
    }

    private void applyPrimaryButtonStyle(JButton button, int fontSize) {
        button.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setRequestFocusEnabled(false);
        button.setRolloverEnabled(false);
        button.setBorderPainted(false);
        button.setBorder(new EmptyBorder(10, 22, 10, 22));
        button.setBackground(UiTheme.DARK_PRIMARY);
        button.setForeground(Color.WHITE);

        Object hoverInstalled = button.getClientProperty("hoverInstalled");
        if (Boolean.TRUE.equals(hoverInstalled))
            return;
        button.putClientProperty("hoverInstalled", Boolean.TRUE);

        if (Boolean.TRUE.equals(button.getClientProperty("customHover")))
            return;

        Color baseBg = button.getBackground();
        button.putClientProperty("baseBg", baseBg);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!button.isEnabled())
                    return;
                Color bg = (Color) button.getClientProperty("baseBg");
                if (bg != null)
                    button.setBackground(darken(bg, 0.12f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Color bg = (Color) button.getClientProperty("baseBg");
                if (bg != null)
                    button.setBackground(bg);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!button.isEnabled())
                    return;
                Color bg = (Color) button.getClientProperty("baseBg");
                if (bg != null)
                    button.setBackground(darken(bg, 0.22f));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!button.isEnabled())
                    return;
                if (!button.getBounds().contains(e.getPoint()))
                    return;
                Color bg = (Color) button.getClientProperty("baseBg");
                if (bg != null)
                    button.setBackground(darken(bg, 0.12f));
            }
        });
    }

    private void applyIconButtonStyle(JButton button) {
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setRequestFocusEnabled(false);
        button.setRolloverEnabled(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(UiTheme.DARK_PRIMARY);
        // Larger padding for bigger button with icon centered
        int padding = UiTheme.scale(12);
        button.setBorder(new EmptyBorder(padding, padding, padding, padding));
        button.setPreferredSize(UiTheme.scaledDimension(52, 52));

        // Add hover effect matching primary button style
        Color baseBg = UiTheme.DARK_PRIMARY;
        button.putClientProperty("baseBg", baseBg);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(darken(baseBg, 0.12f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseBg);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(darken(baseBg, 0.22f));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.getBounds().contains(e.getPoint())) {
                    button.setBackground(darken(baseBg, 0.12f));
                } else {
                    button.setBackground(baseBg);
                }
            }
        });
    }

    private void showMenu() {
        showCurrentBadgeOnSlots = false;
        updateMenuUserIndicator();
        cardLayout.show(cardPanel, "MENU");
    }

    private void showCredits() {
        cardLayout.show(cardPanel, "CREDITS");
    }

    private void showSetup() {
        cardLayout.show(cardPanel, "SETUP");
    }

    private void showHighscores() {
        if (highscoresPanel != null) {
            highscoresPanel.refreshHighscores();
        }
        cardLayout.show(cardPanel, "HIGHSCORES");
    }

    private void installScrollBarStyle(JScrollPane scrollPane) {
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        bar.setPreferredSize(new Dimension(UiTheme.scale(12), Integer.MAX_VALUE));
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
                int arc = UiTheme.scale(10);
                int inset = UiTheme.scale(2);
                g2.fillRoundRect(thumbBounds.x + inset, thumbBounds.y + inset,
                        thumbBounds.width - (inset * 2), thumbBounds.height - (inset * 2), arc, arc);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(trackColor);
                int arc = UiTheme.scale(10);
                int inset = UiTheme.scale(2);
                g2.fillRoundRect(trackBounds.x + inset, trackBounds.y + inset,
                        trackBounds.width - (inset * 2), trackBounds.height - (inset * 2), arc, arc);
                g2.dispose();
            }
        });
        bar.setOpaque(false);
        scrollPane.setOpaque(false);
    }

    @SuppressWarnings("unused")
    private String getPlayerName() {
        String playerName = JOptionPane.showInputDialog(
                this,
                "Congratulations! You've achieved a highscore!\n\nEnter your name:",
                "Highscore Achievement",
                JOptionPane.PLAIN_MESSAGE);

        // Validate input
        if (playerName == null) {
            return null; // User cancelled
        }

        playerName = playerName.trim();
        if (playerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid name.", "Invalid Name",
                    JOptionPane.WARNING_MESSAGE);
            return getPlayerName(); // Ask again
        }

        if (playerName.length() > 20) {
            JOptionPane.showMessageDialog(this, "Name must be 20 characters or less.", "Name Too Long",
                    JOptionPane.WARNING_MESSAGE);
            return getPlayerName(); // Ask again
        }

        // Confirmation dialog
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Save highscore with name: '" + playerName + "'?",
                "Confirm Name",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            return playerName; // Confirmed, save with this name
        } else {
            return getPlayerName(); // No, ask for name again
        }
    }

    private void startNewGame(int newSize, int newMinesCount) {
        size = newSize;
        minesCount = newMinesCount;

        firstMove = true;
        gameActive = true;
        lastExplodedRow = -1;
        lastExplodedCol = -1;

        buttons = new JButton[size][size];
        mines = new boolean[size][size];
        revealed = new boolean[size][size];

        flagged = new boolean[size][size];

        boardPanel.removeAll();

        if (boardShape == BoardShape.HEXAGONAL) {
            hexLayout = new HexagonalLayoutManager();
            boardPanel.setLayout(hexLayout);
        } else {
            hexLayout = null;
            boardPanel.setLayout(new GridLayout(size, size, 0, 0));
        }

        initializeBoard(boardPanel);
        placeMines();
        updateCellSizesAndIcons();

        updateStatusHeader();
        startRunTimer();

        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private void updateStatusHeader() {
        if (statusLabel == null)
            return;
        String diff = (currentDifficulty == null || currentDifficulty.isBlank()) ? "Custom" : currentDifficulty;
        statusLabel.setText("Difficulty: " + diff + "    Mines: " + minesCount);
        if (livesLabel != null) {
            livesLabel.setText("Lives: " + lives);
        }
    }

    private void startRunTimer() {
        runStartMs = System.currentTimeMillis();
        if (runTimer != null) {
            runTimer.stop();
        }
        runTimer = new Timer(250, e -> updateTimerLabel());
        runTimer.setRepeats(true);
        if (!gamePaused) {
            runTimer.start();
        }
        updateTimerLabel();
    }

    private void stopRunTimer() {
        if (runTimer != null) {
            runTimer.stop();
        }
        updateTimerLabel();
    }

    private void updateTimerLabel() {
        if (timerLabel == null)
            return;
        long elapsed = (runStartMs == 0L) ? 0L : (System.currentTimeMillis() - runStartMs);
        timerLabel.setText("Time: " + formatDuration(elapsed));
    }

    private static String formatDuration(long ms) {
        long totalSeconds = Math.max(0L, ms / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void initializeBoard(JPanel boardPanel) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                JButton button;
                if (boardShape == BoardShape.HEXAGONAL) {
                    button = new HexButton(row, col);
                } else {
                    button = new JButton();
                    button.setUI(new BasicButtonUI());
                    button.setOpaque(true);
                    button.setContentAreaFilled(true);
                    button.setBorderPainted(true);
                    button.setBorder(new LineBorder(UiTheme.DARK_CELL_BORDER, 1, true));
                    button.setFocusPainted(false);
                    button.setFocusable(false);
                    button.setRequestFocusEnabled(false);
                    button.setRolloverEnabled(false);
                }
                button.setFont(UiTheme.FONT_CELL);
                button.setFocusPainted(false);
                button.setPreferredSize(new Dimension(40, 40));
                button.setMargin(new Insets(0, 0, 0, 0));
                buttons[row][col] = button;

                int r = row;
                int c = col;

                button.addActionListener(e -> revealCell(r, c));
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (gamePaused)
                            return;
                        if (SwingUtilities.isRightMouseButton(e)) {
                            if (!gameActive || !button.isEnabled())
                                return;
                            toggleFlag(r, c);
                            return;
                        }
                        if (!button.isEnabled() || revealed[r][c] || flagged[r][c])
                            return;
                        button.setBackground(new Color(37, 99, 235));
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (gamePaused)
                            return;
                        if (!button.isEnabled() || revealed[r][c])
                            return;
                        applyCellTheme(r, c);
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (gamePaused)
                            return;
                        if (!button.isEnabled() || revealed[r][c] || flagged[r][c])
                            return;
                        button.setBackground(new Color(59, 130, 246));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (gamePaused)
                            return;
                        if (!button.isEnabled() || revealed[r][c])
                            return;
                        applyCellTheme(r, c);
                    }
                });

                applyCellTheme(row, col);
                boardPanel.add(button);
            }
        }
    }

    private void placeMines() {
        Random rand = new Random();
        int placed = 0;
        while (placed < minesCount) {
            int r = rand.nextInt(size);
            int c = rand.nextInt(size);
            if (!mines[r][c]) {
                mines[r][c] = true;
                placed++;
            }
        }
    }

    private int countMines(int row, int col) {
        if (boardShape == BoardShape.HEXAGONAL) {
            return countMinesHex(row, col);
        }
        return countMinesSquare(row, col);
    }

    private int countMinesSquare(int row, int col) {
        int count = 0;
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (r >= 0 && r < size && c >= 0 && c < size && mines[r][c]) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countMinesHex(int row, int col) {
        int count = 0;
        for (Point p : getNeighborsHex(row, col)) {
            if (mines[p.x][p.y]) {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unused")
    private int countMinesTri(int row, int col) {
        int count = 0;
        boolean pointsUp = ((row + col) % 2 == 0);
        int[][] triOffsets = pointsUp
                ? new int[][] { { -1, 0 }, { 0, -1 }, { 0, 1 } }
                : new int[][] { { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] offset : triOffsets) {
            int r = row + offset[0];
            int c = col + offset[1];
            if (r >= 0 && r < size && c >= 0 && c < size && mines[r][c]) {
                count++;
            }
        }
        return count;
    }

    private boolean checkWin() {
        int revealedCount = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (revealed[r][c])
                    revealedCount++;
            }
        }
        return revealedCount == (size * size - minesCount);
    }

    private void revealCell(int row, int col) {
        if (!gameActive)
            return;
        if (gamePaused)
            return;
        if (revealed[row][col] || flagged[row][col])
            return;

        if (firstMove) {
            firstMove = false;
            ensureSafeZone(row, col);
            explodeAndRevealNearby(row, col);
            return;
        }

        if (mines[row][col]) {
            lastExplodedRow = row;
            lastExplodedCol = col;
            revealed[row][col] = true;

            JButton b = buttons[row][col];
            b.setText("");
            b.setIcon(starburstIcon);
            b.setDisabledIcon(starburstIcon);
            b.setBackground(new Color(244, 63, 94));
            b.setEnabled(false);

            // Add explosion animation
            AnimationManager.shakeButton(b, 8, 300);
            AnimationManager.pulseButton(b, new Color(244, 63, 94), new Color(251, 146, 60), 400);

            lives--;
            updateStatusHeader();

            if (lives <= 0) {
                gameOver(false);
            }
            return;
        }

        floodReveal(row, col);
        if (checkWin()) {
            gameOver(true);
        }
    }

    private void floodReveal(int startRow, int startCol) {
        ArrayDeque<Point> queue = new ArrayDeque<>();
        boolean[][] queued = new boolean[size][size];
        List<Point> revealOrder = new ArrayList<>();

        queue.add(new Point(startRow, startCol));
        queued[startRow][startCol] = true;

        while (!queue.isEmpty()) {
            Point p = queue.removeFirst();
            int row = p.x;
            int col = p.y;

            if (row < 0 || row >= size || col < 0 || col >= size)
                continue;
            if (revealed[row][col] || flagged[row][col] || mines[row][col])
                continue;

            revealed[row][col] = true;
            revealOrder.add(p);
            int count = countMines(row, col);

            JButton button = buttons[row][col];
            if (button == null)
                continue;

            button.setText(count == 0 ? "" : String.valueOf(count));
            button.setIcon(null);
            button.setDisabledIcon(null);
            button.setEnabled(true);

            if (count > 0) {
                Color n = getNumberColor(count);
                button.setForeground(n);
                continue;
            }

            Color emptyFg = new Color(31, 41, 55);
            button.setForeground(emptyFg);

            List<Point> neighbors = getNeighbors(row, col);
            for (Point n : neighbors) {
                int r = n.x;
                int c = n.y;
                if (r < 0 || r >= size || c < 0 || c >= size)
                    continue;
                if (queued[r][c])
                    continue;
                queued[r][c] = true;
                queue.addLast(new Point(r, c));
            }
        }

        animateReveals(revealOrder);
    }

    private void animateReveals(List<Point> points) {
        if (points.isEmpty())
            return;

        // Shape-based timing: slower for hexagonal
        int totalDuration = boardShape == BoardShape.HEXAGONAL ? 600 : 450;
        int steps = totalDuration / 16;
        int totalPoints = points.size();

        final int[] currentStep = { 0 };
        final int[] lastRevealIndex = { -1 };

        Timer revealTimer = new Timer(16, e -> {
            if (currentStep[0] >= steps) {
                ((Timer) e.getSource()).stop();
                // Ensure all cells are at final state
                for (Point p : points) {
                    JButton button = buttons[p.x][p.y];
                    if (button != null) {
                        button.setBackground(UiTheme.LIGHT_CELL_REVEALED_BG);
                    }
                }
                return;
            }

            // Smooth progress with easing
            double progress = easeOutQuart((double) currentStep[0] / steps);
            int revealIndex = (int) (progress * totalPoints);

            // Animate newly revealed cells individually
            if (revealIndex > lastRevealIndex[0]) {
                for (int i = lastRevealIndex[0] + 1; i <= revealIndex && i < totalPoints; i++) {
                    Point p = points.get(i);
                    animateCellReveal(p.x, p.y, 200);
                }
                lastRevealIndex[0] = revealIndex;
            }

            currentStep[0]++;
        });
        revealTimer.start();
    }

    private void animateCellReveal(int row, int col, int duration) {
        JButton button = buttons[row][col];
        if (button == null)
            return;

        int steps = duration / 16;
        final int[] step = { 0 };

        Timer cellTimer = new Timer(16, e -> {
            if (step[0] >= steps) {
                ((Timer) e.getSource()).stop();
                button.setBackground(UiTheme.LIGHT_CELL_REVEALED_BG);
                return;
            }

            double p = easeOutCubic((double) step[0] / steps);
            Color c = interpolateColor(UiTheme.DARK_CELL_BG, UiTheme.LIGHT_CELL_REVEALED_BG, p);
            button.setBackground(c);
            step[0]++;
        });
        cellTimer.start();
    }

    private double easeOutQuart(double t) {
        return 1 - Math.pow(1 - t, 4);
    }

    private double easeOutCubic(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    private Color interpolateColor(Color c1, Color c2, double ratio) {
        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);
        int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * ratio);
        return new Color(r, g, b, a);
    }

    private List<Point> getNeighbors(int row, int col) {
        if (boardShape == BoardShape.HEXAGONAL) {
            return getNeighborsHex(row, col);
        }
        return getNeighborsSquare(row, col);
    }

    private List<Point> getNeighborsSquare(int row, int col) {
        List<Point> neighbors = new ArrayList<>();
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (r == row && c == col)
                    continue;
                neighbors.add(new Point(r, c));
            }
        }
        return neighbors;
    }

    private List<Point> getNeighborsHex(int row, int col) {
        // Convert offset to axial, get neighbors, convert back
        // even-q vertical layout: odd columns are shifted down
        int q = col;
        int r = row - (col >> 1); // col/2 using integer division

        // 6 axial directions for flat-top hexes
        int[][] axialDirs = { { 1, 0 }, { 1, -1 }, { 0, -1 }, { -1, 0 }, { -1, 1 }, { 0, 1 } };

        List<Point> neighbors = new ArrayList<>();
        for (int[] dir : axialDirs) {
            int nq = q + dir[0];
            int nr = r + dir[1];
            // Convert axial back to offset (even-q)
            int nrow = nr + (nq >> 1);
            int ncol = nq;
            if (nrow >= 0 && nrow < size && ncol >= 0 && ncol < size) {
                neighbors.add(new Point(nrow, ncol));
            }
        }
        return neighbors;
    }

    private void gameOver(boolean win) {
        gameActive = false;
        stopRunTimer();

        String message;
        if (win) {
            long elapsed = (runStartMs == 0L) ? 0L : (System.currentTimeMillis() - runStartMs);
            long timeSeconds = elapsed / 1000;

            System.out.println("DEBUG: Game won - Time: " + timeSeconds + "s, Difficulty: '" + currentDifficulty + "'");

            // Check if this qualifies as a highscore
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String boardMode = (boardShape == null) ? "SQUARE" : boardShape.name();
            boolean qualifies = dbManager.isHighscore(currentDifficulty, boardMode, maxLives, timeSeconds, 10);
            System.out.println("DEBUG: Highscore qualification: " + qualifies);

            if (qualifies) {
                if (guestMode || currentUserId == null) {
                    message = "You Win!\nTime: " + formatDuration(elapsed);
                } else {
                    String playerName = (currentUsername == null || currentUsername.isBlank()) ? "Player"
                            : currentUsername;
                    Highscore highscore = new Highscore(playerName, currentDifficulty, boardMode, maxLives,
                            timeSeconds);
                    boolean saved = dbManager.saveHighscore(currentUserId, highscore);
                    System.out.println("DEBUG: Highscore saved: " + saved);

                    // Refresh the highscores panel to show the new highscore immediately
                    if (highscoresPanel != null) {
                        highscoresPanel.refreshHighscores();
                    }

                    message = "Congratulations! New Highscore!\nPlayer: " + playerName + "\nTime: "
                            + formatDuration(elapsed);
                }
            } else {
                message = "You Win!\nTime: " + formatDuration(elapsed);
            }
        } else {
            message = "Game Over!";
        }

        // Show confetti animation on win before showing dialog
        if (win) {
            AnimationManager.showConfetti(this, 120, 5000);
        }

        showEndGameOverlay(win, message);

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                JButton b = buttons[r][c];
                if (b == null)
                    continue;

                // Remove correctly placed flags (flags on mines) to prevent display issues on
                // resize
                if (flagged[r][c] && mines[r][c]) {
                    flagged[r][c] = false;
                    b.setIcon(null);
                    b.setDisabledIcon(null);
                }

                if (flagged[r][c] && b.getIcon() != null) {
                    b.setDisabledIcon(b.getIcon());
                }

                if (mines[r][c]) {
                    if (!(r == lastExplodedRow && c == lastExplodedCol)) {
                        b.setText("");
                        b.setIcon(mineIcon);
                        b.setDisabledIcon(mineIcon);
                    } else {
                        b.setDisabledIcon(starburstIcon);
                    }
                }

                b.setEnabled(false);

                if (mines[r][c]) {
                    // Only set opaque/contentAreaFilled for square buttons, not hex buttons
                    if (!(b instanceof HexButton)) {
                        b.setOpaque(true);
                        b.setContentAreaFilled(true);
                    }
                    if (r == lastExplodedRow && c == lastExplodedCol) {
                        b.setBackground(new Color(251, 146, 60));
                    } else {
                        b.setBackground(new Color(244, 63, 94));
                    }
                    b.setForeground(Color.WHITE);
                }
            }
        }
    }

    private void resetGame() {
        if (buttons == null)
            return;

        firstMove = true;
        gameActive = true;
        lastExplodedRow = -1;
        lastExplodedCol = -1;
        lives = maxLives;

        // For hexagonal mode, fully rebuild the board to avoid visual layout bugs on
        // retry
        if (boardShape == BoardShape.HEXAGONAL) {
            boardPanel.removeAll();
            hexLayout = new HexagonalLayoutManager();
            boardPanel.setLayout(hexLayout);

            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    mines[r][c] = false;
                    revealed[r][c] = false;
                    flagged[r][c] = false;
                }
            }

            initializeBoard(boardPanel);
            placeMines();
            updateCellSizesAndIcons();
            updateStatusHeader();
            startRunTimer();
            boardPanel.revalidate();
            boardPanel.repaint();
            return;
        }

        // Square mode: just reset state (GridLayout handles it fine)
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                JButton b = buttons[r][c];
                b.setText("");
                b.setIcon(null);
                b.setDisabledIcon(null);
                b.setEnabled(true);
                mines[r][c] = false;
                revealed[r][c] = false;
                flagged[r][c] = false;
                applyCellTheme(r, c);
            }
        }

        placeMines();
        updateCellSizesAndIcons();
        updateStatusHeader();
        startRunTimer();
    }

    private void ensureSafeZone(int row, int col) {
        int removed = 0;
        List<Point> toClear = new ArrayList<>();
        toClear.add(new Point(row, col));
        toClear.addAll(getNeighbors(row, col));

        for (Point p : toClear) {
            int r = p.x;
            int c = p.y;
            if (r < 0 || r >= size || c < 0 || c >= size)
                continue;
            if (mines[r][c]) {
                mines[r][c] = false;
                removed++;
            }
        }

        if (removed == 0)
            return;

        Random rand = new Random();
        while (removed > 0) {
            int r = rand.nextInt(size);
            int c = rand.nextInt(size);
            if (mines[r][c])
                continue;
            if (r >= row - 1 && r <= row + 1 && c >= col - 1 && c <= col + 1)
                continue;
            mines[r][c] = true;
            removed--;
        }
    }

    private void explodeAndRevealNearby(int row, int col) {
        Color explosion = new Color(251, 146, 60);

        JButton center = buttons[row][col];
        center.setText("*");
        center.setForeground(Color.WHITE);
        center.setBackground(explosion);

        for (Point p : getNeighbors(row, col)) {
            int r = p.x;
            int c = p.y;
            if (r < 0 || r >= size || c < 0 || c >= size)
                continue;
            if (flagged[r][c] || revealed[r][c])
                continue;
            JButton b = buttons[r][c];
            if (b == null)
                continue;
            b.setBackground(explosion);
            b.setForeground(Color.WHITE);
        }

        Timer timer = new Timer(220, e -> {
            ((Timer) e.getSource()).stop();
            center.setText("");
            applyCellTheme(row, col);
            revealSafeNeighborhood(row, col);
            if (checkWin()) {
                gameOver(true);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void revealSafeNeighborhood(int row, int col) {
        List<Point> around = new ArrayList<>();
        around.add(new Point(row, col));
        around.addAll(getNeighbors(row, col));

        for (Point p : around) {
            int r = p.x;
            int c = p.y;
            if (r < 0 || r >= size || c < 0 || c >= size)
                continue;
            if (mines[r][c] || flagged[r][c] || revealed[r][c])
                continue;
            int count = countMines(r, c);
            if (count == 0) {
                floodReveal(r, c);
            } else {
                revealNumberCell(r, c, count);
            }
        }
    }

    private void revealNumberCell(int row, int col, int count) {
        if (revealed[row][col] || flagged[row][col] || mines[row][col])
            return;
        revealed[row][col] = true;
        JButton button = buttons[row][col];
        if (button == null)
            return;
        button.setText(String.valueOf(count));
        button.setIcon(null);
        button.setDisabledIcon(null);
        button.setEnabled(true);
        button.setBackground(UiTheme.LIGHT_CELL_REVEALED_BG);
        Color n = getNumberColor(count);
        button.setForeground(n);
    }

    private Color getNumberColor(int count) {
        int c = Math.max(1, Math.min(count, 8));
        Color[] colors = {
                new Color(37, 99, 235),
                new Color(22, 163, 74),
                new Color(220, 38, 38),
                new Color(147, 51, 234),
                new Color(234, 88, 12),
                new Color(8, 145, 178),
                new Color(17, 24, 39),
                new Color(107, 114, 128)
        };
        return colors[c - 1];
    }

    private void toggleFlag(int row, int col) {
        if (!gameActive)
            return;
        if (gamePaused)
            return;
        JButton b = buttons[row][col];
        if (!b.isEnabled())
            return;
        if (revealed[row][col])
            return;

        boolean isPlacing = !flagged[row][col];
        flagged[row][col] = isPlacing;
        b.setText("");
        b.setIcon(isPlacing ? flagIcon : null);
        b.setDisabledIcon(isPlacing ? flagIcon : null);
        b.setForeground(isPlacing ? UiTheme.COLOR_FLAG_FG : new Color(229, 231, 235));
        applyCellTheme(row, col);

        // Add subtle bounce animation
        if (isPlacing) {
            AnimationManager.pulseButton(b, UiTheme.DARK_CELL_BG, new Color(239, 68, 68), 200);
        }
    }

    private void updateCellSizesAndIcons() {
        if (buttons == null || boardPanel == null)
            return;

        Insets insets = boardPanel.getInsets();
        int w = Math.max(1, boardPanel.getWidth() - insets.left - insets.right);
        int h = Math.max(1, boardPanel.getHeight() - insets.top - insets.bottom);
        int cell;
        if (boardShape == BoardShape.HEXAGONAL) {
            double effCols = 1.0 + (size - 1) * 0.75;
            double effRows = size + 0.5;
            cell = (int) Math.floor(Math.min(w / effCols, h / effRows));
            cell = Math.max(28, cell);
        } else {
            cell = Math.max(18, Math.min(w, h) / Math.max(1, size));
        }
        int icon = Math.max(14, (int) Math.round(cell * 0.55));

        flagIcon = iconManager.getFlagIcon(icon);
        mineIcon = iconManager.getMineIcon(icon);
        starburstIcon = iconManager.getStarburstIcon(icon);

        Font cellFont = new Font("Segoe UI", Font.BOLD, Math.max(12, (int) Math.round(cell * 0.35)));

        if (hexLayout != null) {
            hexLayout.setCellSize(cell);
        }

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                JButton b = buttons[r][c];
                if (b == null)
                    continue;

                if (b instanceof HexButton && hexLayout != null) {
                    b.setPreferredSize(new Dimension(cell, Math.max(18, (int) Math.round(cell * 0.866))));
                } else {
                    b.setPreferredSize(new Dimension(cell, cell));
                }
                b.setFont(cellFont);
                b.setIconTextGap(0);
                b.setHorizontalTextPosition(SwingConstants.CENTER);
                b.setVerticalTextPosition(SwingConstants.CENTER);

                if (flagged[r][c]) {
                    b.setIcon(flagIcon);
                    b.setDisabledIcon(flagIcon);
                } else if (!gameActive && mines[r][c]) {
                    if (r == lastExplodedRow && c == lastExplodedCol) {
                        b.setIcon(starburstIcon);
                        b.setDisabledIcon(starburstIcon);
                    } else {
                        b.setIcon(mineIcon);
                        b.setDisabledIcon(mineIcon);
                    }
                }
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private void applyCellTheme(int row, int col) {
        JButton button = buttons[row][col];
        if (button == null)
            return;
        if (revealed[row][col]) {
            button.setBackground(UiTheme.LIGHT_CELL_REVEALED_BG);
            if (button.getText() == null || button.getText().isEmpty()) {
                Color emptyFg = new Color(31, 41, 55);
                button.setForeground(emptyFg);
            } else {
                try {
                    int n = Integer.parseInt(button.getText());
                    Color fg = getNumberColor(n);
                    button.setForeground(fg);
                } catch (NumberFormatException ignored) {
                }
            }
            return;
        }

        if (flagged[row][col]) {
            button.setBackground(UiTheme.DARK_CELL_BG);
            button.setForeground(UiTheme.COLOR_FLAG_FG);
            return;
        }

        button.setBackground(UiTheme.DARK_CELL_BG);
        button.setForeground(new Color(229, 231, 235));
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(JavaMinesweeper::new);
    }
}

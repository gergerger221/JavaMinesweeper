import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JavaMinesweeper extends JFrame {

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

    private JButton[][] buttons;
    private boolean[][] mines;
    private boolean[][] revealed;
    private boolean[][] flagged;

    private JPanel topPanel;
    private JLabel title;
    private JLabel statusLabel;
    private JLabel timerLabel;
    private JButton menuButton;
    private JButton retryButton;
    private JPanel boardContainer;
    private JPanel boardPanel;

    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JPanel menuPanel;
    private JPanel creditsPanel;
    private JPanel difficultyPanel;
    private JPanel gamePanel;

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
        setTitle("JavaMinesweeper");
        setSize(960, 720);
        setMinimumSize(new Dimension(720, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(true);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        add(cardPanel, BorderLayout.CENTER);

        buildMenuUI();
        buildCreditsUI();
        buildDifficultyUI();
        buildGameUI();

        cardPanel.add(menuPanel, "MENU");
        cardPanel.add(creditsPanel, "CREDITS");
        cardPanel.add(difficultyPanel, "DIFFICULTY");
        cardPanel.add(gamePanel, "GAME");

        setLocationRelativeTo(null);
        showMenu();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }

    private void buildMenuUI() {
        menuPanel = new MinePatternPanel(new GridBagLayout());

        JLabel menuTitle = new JLabel("JavaMinesweeper");
        menuTitle.setFont(new Font("Segoe UI", Font.BOLD, 40));
        menuTitle.setForeground(new Color(15, 23, 42));

        JButton playButton = new JButton("Play");
        applyPrimaryButtonStyle(playButton, 16);
        playButton.addActionListener(e -> showDifficulty());

        JLabel creditsLink = new JLabel("Credits");
        creditsLink.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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
        gbc.insets = new Insets(0, 0, 22, 0);
        menuPanel.add(menuTitle, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        menuPanel.add(playButton, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        menuPanel.add(creditsLink, gbc);
    }

    private void buildCreditsUI() {
        creditsPanel = new MinePatternPanel(new GridBagLayout());

        JLabel title = new JLabel("Credits");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(new Color(15, 23, 42));

        JLabel group = new JLabel("Group: Placeholder Group Name");
        group.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        group.setForeground(new Color(15, 23, 42));

        JLabel members = new JLabel(
                "<html>Members:<br>1) Joven Sanchez<br>2) Andrew Llaneta<br>3) Terd Sionzon<br>4) Christiano Ronaldo</html>");
        members.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        members.setForeground(new Color(15, 23, 42));

        JButton backButton = new JButton("Back");
        applyPrimaryButtonStyle(backButton, 14);
        backButton.addActionListener(e -> showMenu());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 18, 0);
        creditsPanel.add(title, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        creditsPanel.add(group, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 18, 0);
        creditsPanel.add(members, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        creditsPanel.add(backButton, gbc);
    }

    private void buildDifficultyUI() {
        difficultyPanel = new MinePatternPanel(new GridBagLayout());

        ImageIcon difficultyMineIcon = iconManager.getMineIcon(22);

        JLabel difficultyTitle = new JLabel("Choose Difficulty");
        difficultyTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        difficultyTitle.setForeground(new Color(15, 23, 42));

        JPanel difficultyTitleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        difficultyTitleRow.setOpaque(false);
        JLabel leftMine = new JLabel(difficultyMineIcon);
        JLabel rightMine = new JLabel(difficultyMineIcon);
        difficultyTitleRow.add(leftMine);
        difficultyTitleRow.add(difficultyTitle);
        difficultyTitleRow.add(rightMine);

        Dimension difficultyButtonSize = new Dimension(160, 44);

        ImageIcon difficultyFlagIcon = iconManager.getFlagIcon(18);
        Color difficultyHoverBg = new Color(99, 102, 241);
        Color difficultyNormalBg = UiTheme.DARK_PRIMARY;

        JButton easyButton = new JButton();
        easyButton.putClientProperty("customHover", Boolean.TRUE);
        applyPrimaryButtonStyle(easyButton, 16);
        easyButton.setBackground(difficultyNormalBg);
        easyButton.setText("Easy");

        JLabel easyLeftFlag = new JLabel();
        easyLeftFlag.setPreferredSize(new Dimension(24, 24));
        JLabel easyRightFlag = new JLabel();
        easyRightFlag.setPreferredSize(new Dimension(24, 24));

        JPanel easyRow = new JPanel(new BorderLayout(10, 0));
        easyRow.setOpaque(false);
        easyRow.add(easyLeftFlag, BorderLayout.WEST);
        easyRow.add(easyButton, BorderLayout.CENTER);
        easyRow.add(easyRightFlag, BorderLayout.EAST);
        easyButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                easyLeftFlag.setIcon(difficultyFlagIcon);
                easyRightFlag.setIcon(difficultyFlagIcon);
                easyButton.setBackground(difficultyHoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                easyLeftFlag.setIcon(null);
                easyRightFlag.setIcon(null);
                easyButton.setBackground(difficultyNormalBg);
            }
        });
        easyButton.setPreferredSize(difficultyButtonSize);
        easyButton.setMinimumSize(difficultyButtonSize);
        easyButton.setMaximumSize(difficultyButtonSize);
        easyButton.addActionListener(e -> {
            currentDifficulty = "Easy";
            startNewGame(8, 10);
            cardLayout.show(cardPanel, "GAME");
        });

        JButton mediumButton = new JButton();
        mediumButton.putClientProperty("customHover", Boolean.TRUE);
        applyPrimaryButtonStyle(mediumButton, 16);
        mediumButton.setBackground(difficultyNormalBg);
        mediumButton.setText("Medium");

        JLabel mediumLeftFlag = new JLabel();
        mediumLeftFlag.setPreferredSize(new Dimension(24, 24));
        JLabel mediumRightFlag = new JLabel();
        mediumRightFlag.setPreferredSize(new Dimension(24, 24));

        JPanel mediumRow = new JPanel(new BorderLayout(10, 0));
        mediumRow.setOpaque(false);
        mediumRow.add(mediumLeftFlag, BorderLayout.WEST);
        mediumRow.add(mediumButton, BorderLayout.CENTER);
        mediumRow.add(mediumRightFlag, BorderLayout.EAST);
        mediumButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mediumLeftFlag.setIcon(difficultyFlagIcon);
                mediumRightFlag.setIcon(difficultyFlagIcon);
                mediumButton.setBackground(difficultyHoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mediumLeftFlag.setIcon(null);
                mediumRightFlag.setIcon(null);
                mediumButton.setBackground(difficultyNormalBg);
            }
        });
        mediumButton.setPreferredSize(difficultyButtonSize);
        mediumButton.setMinimumSize(difficultyButtonSize);
        mediumButton.setMaximumSize(difficultyButtonSize);
        mediumButton.addActionListener(e -> {
            currentDifficulty = "Medium";
            startNewGame(12, 25);
            cardLayout.show(cardPanel, "GAME");
        });

        JButton hardButton = new JButton();
        hardButton.putClientProperty("customHover", Boolean.TRUE);
        applyPrimaryButtonStyle(hardButton, 16);
        hardButton.setBackground(difficultyNormalBg);
        hardButton.setText("Hard");

        JLabel hardLeftFlag = new JLabel();
        hardLeftFlag.setPreferredSize(new Dimension(24, 24));
        JLabel hardRightFlag = new JLabel();
        hardRightFlag.setPreferredSize(new Dimension(24, 24));

        JPanel hardRow = new JPanel(new BorderLayout(10, 0));
        hardRow.setOpaque(false);
        hardRow.add(hardLeftFlag, BorderLayout.WEST);
        hardRow.add(hardButton, BorderLayout.CENTER);
        hardRow.add(hardRightFlag, BorderLayout.EAST);
        hardButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hardLeftFlag.setIcon(difficultyFlagIcon);
                hardRightFlag.setIcon(difficultyFlagIcon);
                hardButton.setBackground(difficultyHoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hardLeftFlag.setIcon(null);
                hardRightFlag.setIcon(null);
                hardButton.setBackground(difficultyNormalBg);
            }
        });
        hardButton.setPreferredSize(difficultyButtonSize);
        hardButton.setMinimumSize(difficultyButtonSize);
        hardButton.setMaximumSize(difficultyButtonSize);
        hardButton.addActionListener(e -> {
            currentDifficulty = "Hard";
            startNewGame(16, 45);
            cardLayout.show(cardPanel, "GAME");
        });

        JButton backButton = new JButton("Back");
        applyPrimaryButtonStyle(backButton, 14);
        backButton.setPreferredSize(difficultyButtonSize);
        backButton.setMinimumSize(difficultyButtonSize);
        backButton.setMaximumSize(difficultyButtonSize);
        backButton.addActionListener(e -> showMenu());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 14, 0);
        difficultyPanel.add(difficultyTitleRow, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        difficultyPanel.add(easyRow, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 10, 0);
        difficultyPanel.add(mediumRow, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 10, 0);
        difficultyPanel.add(hardRow, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(12, 0, 0, 0);
        difficultyPanel.add(backButton, gbc);
    }

    private void buildGameUI() {
        gamePanel = new MinePatternPanel(new BorderLayout());

        topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(10, 12, 10, 12));
        topPanel.setBackground(UiTheme.DARK_HEADER_BG);

        title = new JLabel("JavaMinesweeper");
        title.setFont(UiTheme.FONT_TITLE);
        title.setForeground(UiTheme.DARK_HEADER_FG);
        topPanel.add(title, BorderLayout.WEST);

        statusLabel = new JLabel("");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(UiTheme.HEADER_TEXT_BRIGHT);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        timerLabel = new JLabel("Time: 00:00");
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        timerLabel.setForeground(UiTheme.HEADER_TEXT_BRIGHT);
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel centerHeader = new JPanel();
        centerHeader.setOpaque(false);
        centerHeader.setLayout(new BoxLayout(centerHeader, BoxLayout.Y_AXIS));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerHeader.add(statusLabel);
        centerHeader.add(Box.createVerticalStrut(2));
        centerHeader.add(timerLabel);
        topPanel.add(centerHeader, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);

        menuButton = new JButton("Menu");
        applyPrimaryButtonStyle(menuButton, 13);
        menuButton.addActionListener(e -> showMenu());
        actionsPanel.add(menuButton);

        retryButton = new JButton("Retry");
        applyPrimaryButtonStyle(retryButton, 13);
        retryButton.addActionListener(e -> resetGame());
        actionsPanel.add(retryButton);

        topPanel.add(actionsPanel, BorderLayout.EAST);
        gamePanel.add(topPanel, BorderLayout.NORTH);

        boardPanel = new JPanel();
        boardPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        boardPanel.setOpaque(false);
        boardPanel.setBackground(new Color(0, 0, 0, 0));

        boardContainer = new SquareBoardContainer(boardPanel);
        boardContainer.setLayout(null);
        boardContainer.setBorder(new EmptyBorder(12, 12, 12, 12));
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
    }

    private void applyPrimaryButtonStyle(JButton button, int fontSize) {
        button.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        button.setFocusPainted(false);
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

    private void showMenu() {
        cardLayout.show(cardPanel, "MENU");
    }

    private void showCredits() {
        cardLayout.show(cardPanel, "CREDITS");
    }

    private void showDifficulty() {
        cardLayout.show(cardPanel, "DIFFICULTY");
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
        boardPanel.setLayout(new GridLayout(size, size, 0, 0));

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
    }

    private void startRunTimer() {
        runStartMs = System.currentTimeMillis();
        if (runTimer != null) {
            runTimer.stop();
        }
        runTimer = new Timer(250, e -> updateTimerLabel());
        runTimer.setRepeats(true);
        runTimer.start();
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
                JButton button = new JButton();
                button.setFont(UiTheme.FONT_CELL);
                button.setFocusPainted(false);
                button.setOpaque(true);
                button.setPreferredSize(new Dimension(40, 40));
                button.setMargin(new Insets(0, 0, 0, 0));
                button.setContentAreaFilled(true);
                button.setBorderPainted(true);
                button.setBorder(new LineBorder(UiTheme.DARK_CELL_BORDER, 1, true));
                buttons[row][col] = button;

                int r = row;
                int c = col;

                button.addActionListener(e -> revealCell(r, c));
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
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
                        if (!button.isEnabled() || revealed[r][c])
                            return;
                        applyCellTheme(r, c);
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (!button.isEnabled() || revealed[r][c] || flagged[r][c])
                            return;
                        button.setBackground(new Color(59, 130, 246));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
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

            gameOver(false);
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
            int count = countMines(row, col);

            JButton button = buttons[row][col];
            button.setText(count == 0 ? "" : String.valueOf(count));
            button.setIcon(null);
            button.setDisabledIcon(null);
            button.setEnabled(false);
            button.setBackground(UiTheme.LIGHT_CELL_REVEALED_BG);

            if (count > 0) {
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
                button.setForeground(colors[Math.max(0, Math.min(count, 8) - 1)]);
                continue;
            }

            button.setForeground(new Color(229, 231, 235));
            for (int r = row - 1; r <= row + 1; r++) {
                for (int c = col - 1; c <= col + 1; c++) {
                    if (r == row && c == col)
                        continue;
                    if (r < 0 || r >= size || c < 0 || c >= size)
                        continue;
                    if (queued[r][c])
                        continue;
                    queued[r][c] = true;
                    queue.addLast(new Point(r, c));
                }
            }
        }
    }

    private void gameOver(boolean win) {
        gameActive = false;
        stopRunTimer();

        String message;
        if (win) {
            long elapsed = (runStartMs == 0L) ? 0L : (System.currentTimeMillis() - runStartMs);
            message = "You Win!\nTime: " + formatDuration(elapsed);
        } else {
            message = "Game Over!";
        }
        JOptionPane.showMessageDialog(this, message);

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                JButton b = buttons[r][c];

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
                    b.setBackground(new Color(244, 63, 94));
                }

                b.setEnabled(false);
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
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (r < 0 || r >= size || c < 0 || c >= size)
                    continue;
                if (mines[r][c]) {
                    mines[r][c] = false;
                    removed++;
                }
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

        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (r < 0 || r >= size || c < 0 || c >= size)
                    continue;
                if (r == row && c == col)
                    continue;
                if (flagged[r][c] || revealed[r][c])
                    continue;
                buttons[r][c].setBackground(explosion);
                buttons[r][c].setForeground(Color.WHITE);
            }
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
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
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
    }

    private void revealNumberCell(int row, int col, int count) {
        if (revealed[row][col] || flagged[row][col] || mines[row][col])
            return;
        revealed[row][col] = true;
        JButton button = buttons[row][col];
        button.setText(String.valueOf(count));
        button.setIcon(null);
        button.setDisabledIcon(null);
        button.setEnabled(false);
        button.setBackground(UiTheme.LIGHT_CELL_REVEALED_BG);

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
        button.setForeground(colors[Math.max(0, Math.min(count, 8) - 1)]);
    }

    private void toggleFlag(int row, int col) {
        if (!gameActive)
            return;
        JButton b = buttons[row][col];
        if (!b.isEnabled())
            return;
        if (revealed[row][col])
            return;

        flagged[row][col] = !flagged[row][col];
        b.setText("");
        b.setIcon(flagged[row][col] ? flagIcon : null);
        b.setDisabledIcon(flagged[row][col] ? flagIcon : null);
        b.setForeground(flagged[row][col] ? UiTheme.COLOR_FLAG_FG : new Color(229, 231, 235));
        applyCellTheme(row, col);
    }

    private void updateCellSizesAndIcons() {
        if (buttons == null || boardPanel == null)
            return;

        Insets insets = boardPanel.getInsets();
        int w = Math.max(1, boardPanel.getWidth() - insets.left - insets.right);
        int h = Math.max(1, boardPanel.getHeight() - insets.top - insets.bottom);
        int cell = Math.max(18, Math.min(w, h) / Math.max(1, size));
        int icon = Math.max(14, (int) Math.round(cell * 0.55));

        flagIcon = iconManager.getFlagIcon(icon);
        mineIcon = iconManager.getMineIcon(icon);
        starburstIcon = iconManager.getStarburstIcon(icon);

        Font cellFont = new Font("Segoe UI", Font.BOLD, Math.max(12, (int) Math.round(cell * 0.35)));
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                JButton b = buttons[r][c];
                if (b == null)
                    continue;

                b.setPreferredSize(new Dimension(cell, cell));
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
        if (revealed[row][col]) {
            button.setBackground(UiTheme.LIGHT_CELL_REVEALED_BG);
            if (button.getText() == null || button.getText().isEmpty()) {
                button.setForeground(new Color(31, 41, 55));
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

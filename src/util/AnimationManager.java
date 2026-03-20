package util;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnimationManager {

    private static final Random RANDOM = new Random();

    public static void shakeButton(JButton button, int intensity, int duration) {
        if (button == null || !button.isVisible())
            return;

        Point originalLocation = button.getLocation();
        int steps = duration / 16;
        final int[] currentStep = { 0 };

        Timer timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep[0] >= steps) {
                    ((Timer) e.getSource()).stop();
                    button.setLocation(originalLocation);
                    return;
                }

                double progress = (double) currentStep[0] / steps;
                int offsetX = (int) (Math.sin(progress * Math.PI * 4) * intensity * (1 - progress));
                int offsetY = (int) (Math.cos(progress * Math.PI * 3) * intensity * (1 - progress));

                button.setLocation(originalLocation.x + offsetX, originalLocation.y + offsetY);
                currentStep[0]++;
            }
        });
        timer.start();
    }

    public static void pulseButton(JButton button, Color baseColor, Color pulseColor, int duration) {
        if (button == null)
            return;

        int steps = duration / 16;
        final int[] currentStep = { 0 };

        Timer timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep[0] >= steps) {
                    ((Timer) e.getSource()).stop();
                    button.setBackground(baseColor);
                    return;
                }

                double progress = (double) currentStep[0] / steps;
                double pulse = Math.sin(progress * Math.PI);
                Color currentColor = interpolateColor(baseColor, pulseColor, pulse);
                button.setBackground(currentColor);
                currentStep[0]++;
            }
        });
        timer.start();
    }

    public static void fadeIcon(JButton button, ImageIcon fromIcon, ImageIcon toIcon, int duration) {
        if (button == null)
            return;

        int steps = duration / 16;
        final int[] currentStep = { 0 };

        Timer timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep[0] >= steps) {
                    ((Timer) e.getSource()).stop();
                    button.setIcon(toIcon);
                    return;
                }

                double progress = (double) currentStep[0] / steps;
                if (progress < 0.5) {
                    button.setIcon(fromIcon);
                } else {
                    button.setIcon(toIcon);
                }
                currentStep[0]++;
            }
        });
        timer.start();
    }

    public static void colorTransition(Component component, Color fromColor, Color toColor, int duration) {
        if (component == null)
            return;

        int steps = duration / 16;
        final int[] currentStep = { 0 };

        Timer timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep[0] >= steps) {
                    ((Timer) e.getSource()).stop();
                    component.setBackground(toColor);
                    return;
                }

                double progress = (double) currentStep[0] / steps;
                Color currentColor = interpolateColor(fromColor, toColor, easeOutCubic(progress));
                component.setBackground(currentColor);
                currentStep[0]++;
            }
        });
        timer.start();
    }

    public static void revealWave(List<Point> points, java.util.function.Consumer<Point> revealAction,
            int delayBetweenReveals) {
        if (points == null || points.isEmpty())
            return;

        final int[] index = { 0 };
        Timer timer = new Timer(delayBetweenReveals, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (index[0] >= points.size()) {
                    ((Timer) e.getSource()).stop();
                    return;
                }

                revealAction.accept(points.get(index[0]));
                index[0]++;
            }
        });
        timer.start();
    }

    public static void rippleEffect(Component centerComponent, Component[] affectedComponents, int duration) {
        if (centerComponent == null || affectedComponents == null || affectedComponents.length == 0)
            return;

        Point center = centerComponent.getLocation();
        int steps = duration / 16;
        final int[] currentStep = { 0 };

        Timer timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep[0] >= steps) {
                    ((Timer) e.getSource()).stop();
                    for (Component comp : affectedComponents) {
                        if (comp instanceof JButton) {
                            ((JButton) comp).setBackground(UiTheme.DARK_CELL_BG);
                        }
                    }
                    return;
                }

                double progress = (double) currentStep[0] / steps;
                double waveRadius = progress * 200;

                for (Component comp : affectedComponents) {
                    if (comp instanceof JButton) {
                        Point compLoc = comp.getLocation();
                        double distance = Math
                                .sqrt(Math.pow(compLoc.x - center.x, 2) + Math.pow(compLoc.y - center.y, 2));

                        if (Math.abs(distance - waveRadius) < 30) {
                            ((JButton) comp).setBackground(new Color(99, 102, 241));
                        } else if (distance < waveRadius - 30) {
                            ((JButton) comp).setBackground(UiTheme.DARK_CELL_BG);
                        }
                    }
                }
                currentStep[0]++;
            }
        });
        timer.start();
    }

    public static void smoothRevealWave(List<Point> points,
            java.util.function.BiConsumer<Integer, Point> revealBatchAction, int totalDuration, boolean isHexagonal) {
        if (points == null || points.isEmpty())
            return;

        int steps = totalDuration / 16;
        int totalPoints = points.size();

        Timer timer = new Timer(16, new ActionListener() {
            final int[] currentStep = { 0 };

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep[0] >= steps) {
                    ((Timer) e.getSource()).stop();
                    revealBatchAction.accept(totalPoints - 1, null);
                    return;
                }

                double progress = easeOutQuart((double) currentStep[0] / steps);
                int revealIndex = (int) (progress * totalPoints);

                if (revealIndex > 0 && revealIndex <= totalPoints) {
                    revealBatchAction.accept(revealIndex - 1, null);
                }

                currentStep[0]++;
            }
        });
        timer.start();
    }

    public static void showConfetti(JFrame frame, int particleCount, int duration) {
        if (frame == null)
            return;

        List<ConfettiParticle> particles = new ArrayList<>();

        Color[] confettiColors = {
                new Color(239, 68, 68),
                new Color(34, 197, 94),
                new Color(59, 130, 246),
                new Color(168, 85, 247),
                new Color(251, 146, 60),
                new Color(236, 72, 153),
                new Color(234, 179, 8)
        };

        // Get the root pane's layered pane dimensions
        JLayeredPane layeredPane = frame.getLayeredPane();
        int initialWidth = layeredPane.getWidth();
        int initialHeight = layeredPane.getHeight();
        int centerX = initialWidth / 2;
        int centerY = initialHeight / 2;

        for (int i = 0; i < particleCount; i++) {
            // Spawn from left or right side
            boolean fromLeft = RANDOM.nextBoolean();
            int x = fromLeft ? -RANDOM.nextInt(100) : initialWidth + RANDOM.nextInt(100);
            int y = RANDOM.nextInt(Math.max(1, initialHeight));

            // Larger confetti pieces (12-28)
            int size = 12 + RANDOM.nextInt(16);
            Color color = confettiColors[RANDOM.nextInt(confettiColors.length)];

            // Velocity toward center with arc
            int targetX = centerX + RANDOM.nextInt(200) - 100;
            int targetY = centerY + RANDOM.nextInt(100) - 50;
            double distanceX = targetX - x;
            double distanceY = targetY - y;

            double speedBase = 4 + RANDOM.nextDouble() * 4;
            double velocityScale = speedBase / Math.sqrt(distanceX * distanceX + distanceY * distanceY);

            double speedX = distanceX * velocityScale * (0.8 + RANDOM.nextDouble() * 0.4);
            double speedY = distanceY * velocityScale * (0.5 + RANDOM.nextDouble() * 0.5);

            // Add slight upward arc
            speedY -= 2 + RANDOM.nextDouble() * 2;

            double rotation = RANDOM.nextDouble() * Math.PI * 2;
            double rotationSpeed = (RANDOM.nextDouble() - 0.5) * 0.3;

            particles.add(new ConfettiParticle(x, y, size, color, speedX, speedY, rotation, rotationSpeed));
        }

        JPanel confettiPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                for (ConfettiParticle p : particles) {
                    g2.setColor(p.color);
                    g2.rotate(p.rotation, p.x + p.size / 2.0, p.y + p.size / 2.0);
                    g2.fillRect((int) p.x, (int) p.y, p.size, p.size);
                    g2.rotate(-p.rotation, p.x + p.size / 2.0, p.y + p.size / 2.0);
                }

                g2.dispose();
            }
        };
        confettiPanel.setOpaque(false);
        confettiPanel.setBounds(0, 0, initialWidth, initialHeight);

        // Add to glass pane - doesn't interfere with layout
        Component oldGlassPane = frame.getGlassPane();
        frame.setGlassPane(confettiPanel);
        confettiPanel.setVisible(true);

        // Add component listener to handle resizing
        final ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int newWidth = frame.getContentPane().getWidth();
                int newHeight = frame.getContentPane().getHeight();
                confettiPanel.setBounds(0, 0, newWidth, newHeight);
                confettiPanel.repaint();
            }
        };
        frame.addComponentListener(resizeListener);

        int steps = duration / 12;
        Timer timer = new Timer(12, new ActionListener() {
            final int[] currentStep = { 0 };

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep[0] >= steps) {
                    ((Timer) e.getSource()).stop();
                    frame.removeComponentListener(resizeListener);
                    confettiPanel.setVisible(false);
                    frame.setGlassPane(oldGlassPane);
                    return;
                }

                for (ConfettiParticle p : particles) {
                    p.y += p.speedY;
                    p.x += p.speedX;
                    p.rotation += p.rotationSpeed;
                    // Gravity - faster fall
                    p.speedY += 0.15;
                    // Air resistance - less for smoother motion
                    p.speedX *= 0.995;
                }

                confettiPanel.repaint();
                currentStep[0]++;
            }
        });
        timer.start();
    }

    private static double easeOutCubic(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    private static double easeOutQuart(double t) {
        return 1 - Math.pow(1 - t, 4);
    }

    private static Color interpolateColor(Color c1, Color c2, double ratio) {
        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);
        int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * ratio);
        return new Color(r, g, b, a);
    }

    private static class ConfettiParticle {
        double x, y;
        int size;
        Color color;
        double speedX, speedY;
        double rotation, rotationSpeed;

        ConfettiParticle(double x, double y, int size, Color color, double speedX, double speedY, double rotation,
                double rotationSpeed) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
            this.speedX = speedX;
            this.speedY = speedY;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
        }
    }
}

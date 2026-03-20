import javax.swing.*;
import java.awt.*;

public class SquareBoardContainer extends JPanel {

    private final JPanel boardPanel;

    public SquareBoardContainer(JPanel boardPanel) {
        this.boardPanel = boardPanel;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (boardPanel == null)
            return;
        Insets insets = getInsets();
        int w = Math.max(0, getWidth() - insets.left - insets.right);
        int h = Math.max(0, getHeight() - insets.top - insets.bottom);
        int side = Math.max(0, Math.min(w, h));
        int x = insets.left + (w - side) / 2;
        int y = insets.top + (h - side) / 2;
        boardPanel.setBounds(x, y, side, side);
    }
}

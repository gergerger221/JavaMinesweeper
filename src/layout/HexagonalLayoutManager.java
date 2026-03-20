package layout;

import ui.HexButton;
import java.awt.*;

public class HexagonalLayoutManager implements LayoutManager2 {
    private int cellWidth = 40;
    private int cellHeight = 40;
    private int horizontalSpacing = 0;
    private int verticalSpacing = 0;
    private int gridSize = 8;

    public HexagonalLayoutManager(int gridSize) {
        this.gridSize = gridSize;
    }

    public void setGridSize(int size) {
        this.gridSize = size;
    }

    public void setCellSize(int cell) {
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
        int w = (int) Math.ceil((gridSize - 1) * stepX + cellWidth);
        int h = (int) Math.ceil((gridSize - 1) * stepY + cellHeight + (cellHeight / 2.0));
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

            int r = hb.getRow();
            int c = hb.getCol();

            int x = startX + (int) Math.round(c * stepX);
            int y = startY + r * stepY + ((c % 2 == 0) ? 0 : (cellHeight / 2));

            comp.setBounds(x, y, cellWidth, cellHeight);
        }
    }
}

package ru.lolipoka.client.util;

import javax.swing.*;
import java.awt.*;

public class FilesTable extends JTable {

    public FilesTable(FileTableModel model) {
        super(model);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setAutoCreateRowSorter(true);
        setShowVerticalLines(false);
        setIconColumnWidth(30);
        setIntercellSpacing(new Dimension(10, 0));

        addKeyListener(new TableKeyListener(this, model));
        addMouseListener(new TableMouseListener(this, model));
    }

    private void setIconColumnWidth(int width) {
        this.getColumnModel().getColumn(0).setMaxWidth(width);
    }
}


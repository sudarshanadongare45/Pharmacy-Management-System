package pharmacy;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Small Swing helpers shared by all panels. */
public class Ui {
    public static void error(Component parent, Exception e) {
        JOptionPane.showMessageDialog(parent, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Pharmacy", JOptionPane.INFORMATION_MESSAGE);
    }
    public static JTable table() {
        JTable t = new JTable();
        t.setRowHeight(24);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return t;
    }
    public static List<Item> items(String sql, Object... p) throws SQLException {
        List<Item> out = new ArrayList<>();
        javax.swing.table.DefaultTableModel m = Db.query(sql, p);
        for (int i = 0; i < m.getRowCount(); i++)
            out.add(new Item(((Number) m.getValueAt(i, 0)).intValue(), String.valueOf(m.getValueAt(i, 1))));
        return out;
    }
    public static void fill(JComboBox<Item> box, List<Item> items) {
        box.removeAllItems();
        for (Item i : items) box.addItem(i);
    }
}

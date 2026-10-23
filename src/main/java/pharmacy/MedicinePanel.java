package pharmacy;

import javax.swing.*;
import java.awt.*;
import java.sql.Date;

/** Manage medicines: add / update / delete / search. */
public class MedicinePanel extends JPanel {
    private final JTable table = Ui.table();
    private final JTextField name = new JTextField(14), batch = new JTextField(8), price = new JTextField(6),
            qty = new JTextField(5), reorder = new JTextField(5), expiry = new JTextField(9), search = new JTextField(14);
    private final JComboBox<Item> category = new JComboBox<>(), supplier = new JComboBox<>();
    private int selectedId = -1;

    private static final String SELECT =
        "SELECT m.medicine_id AS ID, m.name AS Name, c.name AS Category, s.name AS Supplier, m.batch_no AS Batch, " +
        "m.unit_price AS Price, m.stock_qty AS Stock, m.reorder_level AS Reorder, m.expiry_date AS Expiry " +
        "FROM medicines m JOIN categories c ON c.category_id=m.category_id JOIN suppliers s ON s.supplier_id=m.supplier_id ";

    public MedicinePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search name:")); top.add(search);
        JButton go = new JButton("Search"), all = new JButton("Show All");
        top.add(go); top.add(all);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Name")); row1.add(name);
        row1.add(new JLabel("Category")); row1.add(category);
        row1.add(new JLabel("Supplier")); row1.add(supplier);
        row1.add(new JLabel("Batch")); row1.add(batch);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("Price")); row2.add(price);
        row2.add(new JLabel("Stock")); row2.add(qty);
        row2.add(new JLabel("Reorder")); row2.add(reorder);
        row2.add(new JLabel("Expiry (yyyy-mm-dd)")); row2.add(expiry);
        JButton add = new JButton("Add"), upd = new JButton("Update"), del = new JButton("Delete"), clr = new JButton("Clear");
        row2.add(add); row2.add(upd); row2.add(del); row2.add(clr);
        JPanel form = new JPanel(new GridLayout(2, 1));
        form.add(row1); form.add(row2);
        add(form, BorderLayout.SOUTH);

        go.addActionListener(e -> load("WHERE m.name LIKE ? ORDER BY m.name", "%" + search.getText().trim() + "%"));
        all.addActionListener(e -> { search.setText(""); load("ORDER BY m.name"); });
        add.addActionListener(e -> save(true));
        upd.addActionListener(e -> save(false));
        del.addActionListener(e -> delete());
        clr.addActionListener(e -> clear());
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) pick(); });
        refresh();
    }

    /** Reload combos and table (called when tab is opened). */
    public void refresh() {
        try {
            Ui.fill(category, Ui.items("SELECT category_id, name FROM categories ORDER BY name"));
            Ui.fill(supplier, Ui.items("SELECT supplier_id, name FROM suppliers ORDER BY name"));
            load("ORDER BY m.name");
        } catch (Exception ex) { Ui.error(this, ex); }
    }

    private void load(String tail, Object... p) {
        try { table.setModel(Db.query(SELECT + tail, p)); } catch (Exception ex) { Ui.error(this, ex); }
    }

    private void pick() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        r = table.convertRowIndexToModel(r);
        var m = table.getModel();
        selectedId = ((Number) m.getValueAt(r, 0)).intValue();
        name.setText(String.valueOf(m.getValueAt(r, 1)));
        selectCombo(category, String.valueOf(m.getValueAt(r, 2)));
        selectCombo(supplier, String.valueOf(m.getValueAt(r, 3)));
        batch.setText(m.getValueAt(r, 4) == null ? "" : String.valueOf(m.getValueAt(r, 4)));
        price.setText(String.valueOf(m.getValueAt(r, 5)));
        qty.setText(String.valueOf(m.getValueAt(r, 6)));
        reorder.setText(String.valueOf(m.getValueAt(r, 7)));
        expiry.setText(String.valueOf(m.getValueAt(r, 8)));
    }

    private void selectCombo(JComboBox<Item> box, String label) {
        for (int i = 0; i < box.getItemCount(); i++) if (box.getItemAt(i).label.equals(label)) { box.setSelectedIndex(i); return; }
    }

    private void save(boolean isNew) {
        try {
            if (name.getText().trim().isEmpty()) throw new Exception("Name is required.");
            if (category.getSelectedItem() == null || supplier.getSelectedItem() == null) throw new Exception("Select category and supplier.");
            if (!isNew && selectedId < 0) throw new Exception("Select a medicine from the table first.");
            Object[] v = { name.getText().trim(), ((Item) category.getSelectedItem()).id, ((Item) supplier.getSelectedItem()).id,
                    batch.getText().trim(), new java.math.BigDecimal(price.getText().trim()),
                    Integer.parseInt(qty.getText().trim()), Integer.parseInt(reorder.getText().trim()), Date.valueOf(expiry.getText().trim()) };
            if (isNew) {
                Db.update("INSERT INTO medicines(name,category_id,supplier_id,batch_no,unit_price,stock_qty,reorder_level,expiry_date) VALUES (?,?,?,?,?,?,?,?)", v);
            } else {
                Db.update("UPDATE medicines SET name=?,category_id=?,supplier_id=?,batch_no=?,unit_price=?,stock_qty=?,reorder_level=?,expiry_date=? WHERE medicine_id=?",
                        v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], selectedId);
            }
            clear(); load("ORDER BY m.name");
        } catch (IllegalArgumentException ex) {
            Ui.error(this, new Exception("Check inputs: price/stock/reorder must be numbers, expiry must be yyyy-mm-dd."));
        } catch (Exception ex) { Ui.error(this, ex); }
    }

    private void delete() {
        if (selectedId < 0) { Ui.info(this, "Select a medicine first."); return; }
        if (JOptionPane.showConfirmDialog(this, "Delete this medicine?", "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            Db.update("DELETE FROM medicines WHERE medicine_id=?", selectedId);
            clear(); load("ORDER BY m.name");
        } catch (Exception ex) {
            Ui.error(this, new Exception("Cannot delete: this medicine is used in bills or prescriptions."));
        }
    }

    private void clear() {
        selectedId = -1;
        for (JTextField f : new JTextField[]{name, batch, price, qty, reorder, expiry}) f.setText("");
        table.clearSelection();
    }
}

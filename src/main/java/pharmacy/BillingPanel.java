package pharmacy;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Billing: build a cart, generate bill, auto-deduct stock. */
public class BillingPanel extends JPanel {
    private final JComboBox<Item> medicine = new JComboBox<>();
    private final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
    private final JTextField customer = new JTextField(12), phone = new JTextField(10), rx = new JTextField(5);
    private final JLabel totalLbl = new JLabel("Total: 0.00");
    private final DefaultTableModel cartModel = new DefaultTableModel(new String[]{"Medicine", "Qty", "Price", "Total"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final List<BillingService.Line> cart = new ArrayList<>();
    private final JTextArea receipt = new JTextArea(12, 40);

    public BillingPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Customer")); top.add(customer);
        top.add(new JLabel("Phone")); top.add(phone);
        top.add(new JLabel("Prescription ID (optional)")); top.add(rx);
        JPanel pick = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pick.add(new JLabel("Medicine")); pick.add(medicine);
        pick.add(new JLabel("Qty")); pick.add(qty);
        JButton addBtn = new JButton("Add to Cart"), rem = new JButton("Remove Selected"), gen = new JButton("Generate Bill");
        pick.add(addBtn); pick.add(rem);
        JPanel north = new JPanel(new GridLayout(2, 1)); north.add(top); north.add(pick);
        add(north, BorderLayout.NORTH);

        JTable cartTable = Ui.table(); cartTable.setModel(cartModel);
        receipt.setEditable(false); receipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JSplitPane mid = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(cartTable), new JScrollPane(receipt));
        mid.setResizeWeight(0.5);
        add(mid, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalLbl.setFont(totalLbl.getFont().deriveFont(Font.BOLD, 16f));
        bottom.add(totalLbl); bottom.add(gen);
        add(bottom, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addToCart());
        rem.addActionListener(e -> { int r = cartTable.getSelectedRow(); if (r >= 0) { cart.remove(r); redraw(); } });
        gen.addActionListener(e -> generate());
        refresh();
    }

    public void refresh() {
        try {
            Ui.fill(medicine, Ui.items("SELECT medicine_id, CONCAT(name,' (stock ',stock_qty,')') FROM medicines " +
                    "WHERE stock_qty>0 AND expiry_date>=CURDATE() ORDER BY name"));
        } catch (Exception ex) { Ui.error(this, ex); }
    }

    private void addToCart() {
        Item sel = (Item) medicine.getSelectedItem();
        if (sel == null) return;
        try {
            int q = (Integer) qty.getValue();
            DefaultTableModel m = Db.query("SELECT name, unit_price, stock_qty FROM medicines WHERE medicine_id=?", sel.id);
            int inCart = 0;
            for (BillingService.Line l : cart) if (l.medicineId == sel.id) inCart += l.qty;
            if (inCart + q > ((Number) m.getValueAt(0, 2)).intValue()) throw new Exception("Not enough stock available.");
            cart.add(new BillingService.Line(sel.id, (String) m.getValueAt(0, 0), q, (BigDecimal) m.getValueAt(0, 1)));
            redraw();
        } catch (Exception ex) { Ui.error(this, ex); }
    }

    private void redraw() {
        cartModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (BillingService.Line l : cart) {
            cartModel.addRow(new Object[]{l.name, l.qty, l.price, l.total()});
            total = total.add(l.total());
        }
        totalLbl.setText("Total: " + total.setScale(2).toPlainString());
    }

    private void generate() {
        try {
            Integer rxId = rx.getText().trim().isEmpty() ? null : Integer.valueOf(rx.getText().trim());
            int billId = BillingService.createBill(customer.getText(), phone.getText(), rxId, cart);
            receipt.setText(BillingService.receipt(billId));
            cart.clear(); redraw(); refresh();
        } catch (NumberFormatException ex) {
            Ui.error(this, new Exception("Prescription ID must be a number."));
        } catch (Exception ex) {
            String msg = ex.getMessage() != null && ex.getMessage().contains("foreign key")
                    ? "Prescription ID does not exist." : ex.getMessage();
            Ui.error(this, new Exception(msg));
        }
    }
}

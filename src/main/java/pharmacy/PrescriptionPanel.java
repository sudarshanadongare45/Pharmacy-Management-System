package pharmacy;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Track prescriptions: create with multiple medicines, view items. */
public class PrescriptionPanel extends JPanel {
    private final JTextField patient = new JTextField(12), phone = new JTextField(9), doctor = new JTextField(12),
            date = new JTextField(9), notes = new JTextField(14), dosage = new JTextField(12);
    private final JComboBox<Item> medicine = new JComboBox<>();
    private final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
    private final JTable rxTable = Ui.table(), itemTable = Ui.table();
    private final DefaultTableModel pending = new DefaultTableModel(new String[]{"Medicine", "Dosage", "Qty"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final List<Object[]> pendingItems = new ArrayList<>(); // {medicineId, dosage, qty}

    public PrescriptionPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        date.setText(java.time.LocalDate.now().toString());

        JPanel f1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        f1.add(new JLabel("Patient")); f1.add(patient); f1.add(new JLabel("Phone")); f1.add(phone);
        f1.add(new JLabel("Doctor")); f1.add(doctor); f1.add(new JLabel("Date")); f1.add(date);
        f1.add(new JLabel("Notes")); f1.add(notes);
        JPanel f2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        f2.add(new JLabel("Medicine")); f2.add(medicine); f2.add(new JLabel("Dosage")); f2.add(dosage);
        f2.add(new JLabel("Qty")); f2.add(qty);
        JButton addItem = new JButton("Add Medicine"), saveBtn = new JButton("Save Prescription");
        f2.add(addItem); f2.add(saveBtn);
        JTable pendTable = Ui.table(); pendTable.setModel(pending);
        JScrollPane pendScroll = new JScrollPane(pendTable); pendScroll.setPreferredSize(new Dimension(300, 90));
        JPanel north = new JPanel(new BorderLayout());
        JPanel forms = new JPanel(new GridLayout(2, 1)); forms.add(f1); forms.add(f2);
        north.add(forms, BorderLayout.NORTH); north.add(pendScroll, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(rxTable), new JScrollPane(itemTable));
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);

        addItem.addActionListener(e -> {
            Item m = (Item) medicine.getSelectedItem();
            if (m == null) return;
            pendingItems.add(new Object[]{m.id, dosage.getText().trim(), qty.getValue()});
            pending.addRow(new Object[]{m.label.replaceAll(" \\(stock.*", ""), dosage.getText().trim(), qty.getValue()});
        });
        saveBtn.addActionListener(e -> save());
        rxTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showItems(); });
        refresh();
    }

    public void refresh() {
        try {
            Ui.fill(medicine, Ui.items("SELECT medicine_id, name FROM medicines ORDER BY name"));
            rxTable.setModel(Db.query(
                "SELECT p.prescription_id AS ID, c.name AS Patient, c.phone AS Phone, d.name AS Doctor, p.issue_date AS Date, p.notes AS Notes " +
                "FROM prescriptions p JOIN customers c ON c.customer_id=p.customer_id JOIN doctors d ON d.doctor_id=p.doctor_id " +
                "ORDER BY p.prescription_id DESC"));
        } catch (Exception ex) { Ui.error(this, ex); }
    }

    private void showItems() {
        int r = rxTable.getSelectedRow();
        if (r < 0) return;
        int id = ((Number) rxTable.getValueAt(r, 0)).intValue();
        try {
            itemTable.setModel(Db.query(
                "SELECT m.name AS Medicine, pi.dosage AS Dosage, pi.quantity AS Qty, m.stock_qty AS 'In Stock' " +
                "FROM prescription_items pi JOIN medicines m ON m.medicine_id=pi.medicine_id WHERE pi.prescription_id=?", id));
        } catch (Exception ex) { Ui.error(this, ex); }
    }

    private void save() {
        try {
            if (patient.getText().trim().isEmpty() || doctor.getText().trim().isEmpty()) throw new Exception("Patient and doctor are required.");
            if (pendingItems.isEmpty()) throw new Exception("Add at least one medicine.");
            Date d = Date.valueOf(date.getText().trim());
            try (Connection c = Db.get()) {
                c.setAutoCommit(false);
                try {
                    int cust = Db.getOrCreateCustomer(c, patient.getText().trim(), phone.getText().trim());
                    int doc = Db.getOrCreateDoctor(c, doctor.getText().trim());
                    int rx = Db.insertReturningId(c, "INSERT INTO prescriptions(customer_id,doctor_id,issue_date,notes) VALUES (?,?,?,?)",
                            cust, doc, d, notes.getText().trim());
                    for (Object[] it : pendingItems)
                        Db.insertReturningId(c, "INSERT INTO prescription_items(prescription_id,medicine_id,dosage,quantity) VALUES (?,?,?,?)",
                                rx, it[0], it[1], it[2]);
                    c.commit();
                    Ui.info(this, "Prescription saved with ID " + rx);
                } catch (SQLException ex) { c.rollback(); throw ex; }
            }
            pendingItems.clear(); pending.setRowCount(0);
            patient.setText(""); phone.setText(""); doctor.setText(""); notes.setText("");
            refresh();
        } catch (IllegalArgumentException ex) {
            Ui.error(this, new Exception("Date must be yyyy-mm-dd."));
        } catch (Exception ex) { Ui.error(this, ex); }
    }
}

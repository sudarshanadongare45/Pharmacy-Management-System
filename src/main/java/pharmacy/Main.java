package pharmacy;

import javax.swing.*;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
            try (Connection c = Db.get()) {
                // connection OK
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Cannot connect to MySQL:\n" + e.getMessage() +
                    "\n\n1) Start MySQL\n2) Run database/schema.sql\n3) Edit db.properties with your username/password",
                    "Database Connection Failed", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            JFrame f = new JFrame("Pharmacy Management System");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            MedicinePanel med = new MedicinePanel();
            BillingPanel bill = new BillingPanel();
            PrescriptionPanel rx = new PrescriptionPanel();
            ReportsPanel rep = new ReportsPanel();
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Medicines", med);
            tabs.addTab("Billing", bill);
            tabs.addTab("Prescriptions", rx);
            tabs.addTab("Reports", rep);
            tabs.addChangeListener(e -> {
                switch (tabs.getSelectedIndex()) {
                    case 0: med.refresh(); break;
                    case 1: bill.refresh(); break;
                    case 2: rx.refresh(); break;
                    default: rep.refresh();
                }
            });
            f.add(tabs);
            f.setSize(1150, 650);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            rep.refresh();
        });
    }
}

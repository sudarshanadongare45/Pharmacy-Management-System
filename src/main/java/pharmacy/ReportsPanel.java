package pharmacy;

import javax.swing.*;
import java.awt.*;

/** SQL reports: stock levels, expiry, sales (joins + aggregations). */
public class ReportsPanel extends JPanel {
    private static final String[][] REPORTS = {
        {"Stock Levels",
         "SELECT m.medicine_id AS ID, m.name AS Medicine, c.name AS Category, s.name AS Supplier, m.stock_qty AS Stock, m.reorder_level AS Reorder, " +
         "CASE WHEN m.stock_qty <= m.reorder_level THEN 'LOW' ELSE 'OK' END AS Status " +
         "FROM medicines m JOIN categories c ON c.category_id=m.category_id JOIN suppliers s ON s.supplier_id=m.supplier_id ORDER BY m.stock_qty"},
        {"Low Stock Only",
         "SELECT m.name AS Medicine, m.stock_qty AS Stock, m.reorder_level AS Reorder, s.name AS Supplier, s.phone AS 'Supplier Phone' " +
         "FROM medicines m JOIN suppliers s ON s.supplier_id=m.supplier_id WHERE m.stock_qty <= m.reorder_level ORDER BY m.stock_qty"},
        {"Expired / Expiring in 90 Days",
         "SELECT m.name AS Medicine, m.batch_no AS Batch, m.stock_qty AS Stock, m.expiry_date AS Expiry, DATEDIFF(m.expiry_date, CURDATE()) AS 'Days Left' " +
         "FROM medicines m WHERE m.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY) ORDER BY m.expiry_date"},
        {"Daily Sales",
         "SELECT DATE(bill_date) AS 'Sale Date', COUNT(*) AS Bills, SUM(total_amount) AS Revenue FROM bills GROUP BY DATE(bill_date) ORDER BY 1 DESC"},
        {"Top Selling Medicines",
         "SELECT m.name AS Medicine, SUM(bi.quantity) AS 'Units Sold', SUM(bi.line_total) AS Revenue " +
         "FROM bill_items bi JOIN medicines m ON m.medicine_id=bi.medicine_id GROUP BY m.medicine_id, m.name ORDER BY 2 DESC"},
        {"Sales by Category",
         "SELECT c.name AS Category, SUM(bi.quantity) AS 'Units Sold', SUM(bi.line_total) AS Revenue " +
         "FROM bill_items bi JOIN medicines m ON m.medicine_id=bi.medicine_id JOIN categories c ON c.category_id=m.category_id GROUP BY c.name ORDER BY 3 DESC"},
        {"All Bills",
         "SELECT b.bill_id AS 'Bill No', b.bill_date AS Date, IFNULL(cu.name,'Walk-in') AS Customer, b.prescription_id AS 'Rx ID', b.total_amount AS Total " +
         "FROM bills b LEFT JOIN customers cu ON cu.customer_id=b.customer_id ORDER BY b.bill_id DESC"}
    };
    private final JComboBox<String> chooser = new JComboBox<>();
    private final JTable table = Ui.table();

    public ReportsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (String[] r : REPORTS) chooser.addItem(r[0]);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton run = new JButton("Run Report");
        top.add(new JLabel("Report")); top.add(chooser); top.add(run);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        run.addActionListener(e -> refresh());
        chooser.addActionListener(e -> refresh());
    }

    public void refresh() {
        try { table.setModel(Db.query(REPORTS[chooser.getSelectedIndex()][1])); } catch (Exception ex) { Ui.error(this, ex); }
    }
}

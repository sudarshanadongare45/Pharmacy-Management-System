package pharmacy;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/** Business logic for billing: transactional bill creation with automatic stock deduction. */
public class BillingService {

    public static class Line {
        public final int medicineId; public final String name; public final int qty; public final BigDecimal price;
        public Line(int medicineId, String name, int qty, BigDecimal price) {
            this.medicineId = medicineId; this.name = name; this.qty = qty; this.price = price;
        }
        public BigDecimal total() { return price.multiply(BigDecimal.valueOf(qty)); }
    }

    /** Creates a bill, its items, and deducts stock in ONE transaction. Returns the new bill id. */
    public static int createBill(String customerName, String phone, Integer prescriptionId, List<Line> lines) throws SQLException {
        if (lines.isEmpty()) throw new SQLException("Cart is empty.");
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                Integer customerId = (customerName == null || customerName.trim().isEmpty())
                        ? null : Db.getOrCreateCustomer(c, customerName.trim(), phone == null ? "" : phone.trim());
                BigDecimal total = BigDecimal.ZERO;
                for (Line l : lines) total = total.add(l.total());

                int billId = Db.insertReturningId(c,
                        "INSERT INTO bills(customer_id,prescription_id,total_amount) VALUES (?,?,?)",
                        customerId, prescriptionId, total);

                for (Line l : lines) {
                    // atomic stock check + deduction
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE medicines SET stock_qty = stock_qty - ? WHERE medicine_id=? AND stock_qty >= ? AND expiry_date >= CURDATE()")) {
                        ps.setInt(1, l.qty); ps.setInt(2, l.medicineId); ps.setInt(3, l.qty);
                        if (ps.executeUpdate() == 0)
                            throw new SQLException("Cannot sell '" + l.name + "': insufficient stock or medicine expired.");
                    }
                    Db.insertReturningId(c,
                            "INSERT INTO bill_items(bill_id,medicine_id,quantity,unit_price,line_total) VALUES (?,?,?,?,?)",
                            billId, l.medicineId, l.qty, l.price, l.total());
                }
                c.commit();
                return billId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Printable bill text. */
    public static String receipt(int billId) throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (Connection c = Db.get()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT b.bill_date, b.total_amount, IFNULL(cu.name,'Walk-in') AS cust FROM bills b " +
                    "LEFT JOIN customers cu ON cu.customer_id=b.customer_id WHERE b.bill_id=?")) {
                ps.setInt(1, billId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return "Bill not found";
                    sb.append("========== PHARMACY BILL ==========\n");
                    sb.append("Bill No : ").append(billId).append("\n");
                    sb.append("Date    : ").append(rs.getTimestamp(1)).append("\n");
                    sb.append("Customer: ").append(rs.getString(3)).append("\n");
                    sb.append("-----------------------------------\n");
                    String total = rs.getBigDecimal(2).toPlainString();
                    try (PreparedStatement p2 = c.prepareStatement(
                            "SELECT m.name, bi.quantity, bi.unit_price, bi.line_total FROM bill_items bi " +
                            "JOIN medicines m ON m.medicine_id=bi.medicine_id WHERE bi.bill_id=?")) {
                        p2.setInt(1, billId);
                        try (ResultSet r2 = p2.executeQuery()) {
                            while (r2.next())
                                sb.append(String.format("%-20s %3d x %7.2f = %8.2f%n", r2.getString(1), r2.getInt(2), r2.getDouble(3), r2.getDouble(4)));
                        }
                    }
                    sb.append("-----------------------------------\n");
                    sb.append(String.format("TOTAL: %s%n", total));
                    sb.append("===================================\n");
                }
            }
        }
        return sb.toString();
    }
}

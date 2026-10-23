package pharmacy;

import javax.swing.table.DefaultTableModel;
import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;
import java.util.Vector;

/** JDBC helper: connection handling + generic query/update helpers. */
public class Db {
    private static final Properties P = new Properties();

    static {
        P.setProperty("db.url", "jdbc:mysql://localhost:3306/pharmacy_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        P.setProperty("db.user", "root");
        P.setProperty("db.password", "root");
        try (FileInputStream in = new FileInputStream("db.properties")) {
            P.load(in);
        } catch (Exception ignored) { /* use defaults */ }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(P.getProperty("db.url"), P.getProperty("db.user"), P.getProperty("db.password"));
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }

    /** Runs a SELECT and returns a non-editable table model. */
    public static DefaultTableModel query(String sql, Object... params) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                Vector<String> cols = new Vector<>();
                for (int i = 1; i <= md.getColumnCount(); i++) cols.add(md.getColumnLabel(i));
                Vector<Vector<Object>> rows = new Vector<>();
                while (rs.next()) {
                    Vector<Object> r = new Vector<>();
                    for (int i = 1; i <= cols.size(); i++) r.add(rs.getObject(i));
                    rows.add(r);
                }
                return new DefaultTableModel(rows, cols) {
                    @Override public boolean isCellEditable(int r, int c) { return false; }
                };
            }
        }
    }

    /** INSERT / UPDATE / DELETE. Returns affected rows. */
    public static int update(String sql, Object... params) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Returns id of the first matching row or -1. */
    public static int lookupId(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    public static int insertReturningId(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        }
    }

    public static int getOrCreateCustomer(Connection c, String name, String phone) throws SQLException {
        int id = lookupId(c, "SELECT customer_id FROM customers WHERE name=? AND IFNULL(phone,'')=?", name, phone == null ? "" : phone);
        return id > 0 ? id : insertReturningId(c, "INSERT INTO customers(name,phone) VALUES (?,?)", name, phone == null || phone.isEmpty() ? null : phone);
    }

    public static int getOrCreateDoctor(Connection c, String name) throws SQLException {
        int id = lookupId(c, "SELECT doctor_id FROM doctors WHERE name=?", name);
        return id > 0 ? id : insertReturningId(c, "INSERT INTO doctors(name) VALUES (?)", name);
    }
}

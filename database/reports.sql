-- Reports used by the application (Reports tab). Can also be run manually in MySQL.
USE pharmacy_db;

-- 1. Stock level report (JOINs)
SELECT m.medicine_id, m.name, c.name AS category, s.name AS supplier, m.stock_qty, m.reorder_level,
       CASE WHEN m.stock_qty <= m.reorder_level THEN 'LOW' ELSE 'OK' END AS status
FROM medicines m JOIN categories c ON c.category_id = m.category_id
JOIN suppliers s ON s.supplier_id = m.supplier_id ORDER BY m.stock_qty;

-- 2. Expiry report: expired or expiring within 90 days
SELECT m.medicine_id, m.name, m.batch_no, m.expiry_date, DATEDIFF(m.expiry_date, CURDATE()) AS days_left
FROM medicines m WHERE m.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY) ORDER BY m.expiry_date;

-- 3. Daily sales (aggregation)
SELECT DATE(bill_date) AS sale_date, COUNT(*) AS bills, SUM(total_amount) AS revenue
FROM bills GROUP BY DATE(bill_date) ORDER BY sale_date DESC;

-- 4. Top-selling medicines
SELECT m.name, SUM(bi.quantity) AS units_sold, SUM(bi.line_total) AS revenue
FROM bill_items bi JOIN medicines m ON m.medicine_id = bi.medicine_id
GROUP BY m.medicine_id, m.name ORDER BY units_sold DESC;

-- 5. Sales by category
SELECT c.name AS category, SUM(bi.line_total) AS revenue
FROM bill_items bi JOIN medicines m ON m.medicine_id = bi.medicine_id
JOIN categories c ON c.category_id = m.category_id GROUP BY c.name ORDER BY revenue DESC;

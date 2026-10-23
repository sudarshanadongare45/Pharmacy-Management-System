-- Pharmacy Management System : schema (normalized to 3NF) + sample data
DROP DATABASE IF EXISTS pharmacy_db;
CREATE DATABASE pharmacy_db CHARACTER SET utf8mb4;
USE pharmacy_db;

CREATE TABLE categories (
  category_id INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE suppliers (
  supplier_id INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL UNIQUE,
  phone       VARCHAR(20),
  email       VARCHAR(100)
);

CREATE TABLE medicines (
  medicine_id   INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  category_id   INT NOT NULL,
  supplier_id   INT NOT NULL,
  batch_no      VARCHAR(30),
  unit_price    DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
  stock_qty     INT NOT NULL DEFAULT 0 CHECK (stock_qty >= 0),
  reorder_level INT NOT NULL DEFAULT 10,
  expiry_date   DATE NOT NULL,
  FOREIGN KEY (category_id) REFERENCES categories(category_id),
  FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
);

CREATE TABLE customers (
  customer_id INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  phone       VARCHAR(20)
);

CREATE TABLE doctors (
  doctor_id INT AUTO_INCREMENT PRIMARY KEY,
  name      VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE prescriptions (
  prescription_id INT AUTO_INCREMENT PRIMARY KEY,
  customer_id     INT NOT NULL,
  doctor_id       INT NOT NULL,
  issue_date      DATE NOT NULL,
  notes           VARCHAR(255),
  FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
  FOREIGN KEY (doctor_id)   REFERENCES doctors(doctor_id)
);

CREATE TABLE prescription_items (
  item_id         INT AUTO_INCREMENT PRIMARY KEY,
  prescription_id INT NOT NULL,
  medicine_id     INT NOT NULL,
  dosage          VARCHAR(100),
  quantity        INT NOT NULL CHECK (quantity > 0),
  FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id) ON DELETE CASCADE,
  FOREIGN KEY (medicine_id)     REFERENCES medicines(medicine_id)
);

CREATE TABLE bills (
  bill_id         INT AUTO_INCREMENT PRIMARY KEY,
  customer_id     INT NULL,
  prescription_id INT NULL,
  bill_date       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total_amount    DECIMAL(12,2) NOT NULL DEFAULT 0,
  FOREIGN KEY (customer_id)     REFERENCES customers(customer_id),
  FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id)
);

CREATE TABLE bill_items (
  bill_item_id INT AUTO_INCREMENT PRIMARY KEY,
  bill_id      INT NOT NULL,
  medicine_id  INT NOT NULL,
  quantity     INT NOT NULL CHECK (quantity > 0),
  unit_price   DECIMAL(10,2) NOT NULL,
  line_total   DECIMAL(12,2) NOT NULL,
  FOREIGN KEY (bill_id)     REFERENCES bills(bill_id) ON DELETE CASCADE,
  FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
);

CREATE INDEX idx_med_expiry ON medicines(expiry_date);
CREATE INDEX idx_bill_date  ON bills(bill_date);

-- ---------- Sample data ----------
INSERT INTO categories(name) VALUES ('Analgesic'),('Antibiotic'),('Antacid'),('Vitamin'),('Antihistamine'),('Antidiabetic');
INSERT INTO suppliers(name,phone,email) VALUES
 ('MediSupply Pvt Ltd','9876500001','sales@medisupply.com'),
 ('HealthFirst Distributors','9876500002','orders@healthfirst.com'),
 ('CureWell Pharma','9876500003','info@curewell.com');
INSERT INTO medicines(name,category_id,supplier_id,batch_no,unit_price,stock_qty,reorder_level,expiry_date) VALUES
 ('Paracetamol 500mg',1,1,'B1001',2.50,200,50,DATE_ADD(CURDATE(), INTERVAL 18 MONTH)),
 ('Ibuprofen 400mg',1,1,'B1002',4.00,120,40,DATE_ADD(CURDATE(), INTERVAL 14 MONTH)),
 ('Amoxicillin 500mg',2,2,'B2001',9.50,60,30,DATE_ADD(CURDATE(), INTERVAL 60 DAY)),
 ('Azithromycin 250mg',2,2,'B2002',18.00,25,30,DATE_ADD(CURDATE(), INTERVAL 10 MONTH)),
 ('Digene Tablet',3,3,'B3001',1.50,300,60,DATE_ADD(CURDATE(), INTERVAL 20 MONTH)),
 ('Vitamin C 500mg',4,3,'B4001',3.00,15,40,DATE_ADD(CURDATE(), INTERVAL 30 DAY)),
 ('Cetirizine 10mg',5,1,'B5001',2.00,90,30,DATE_ADD(CURDATE(), INTERVAL 12 MONTH)),
 ('Metformin 500mg',6,2,'B6001',3.50,150,50,DATE_ADD(CURDATE(), INTERVAL 16 MONTH)),
 ('Cough Syrup 100ml',5,3,'B7001',55.00,40,20,DATE_SUB(CURDATE(), INTERVAL 15 DAY));
INSERT INTO customers(name,phone) VALUES ('Rahul Sharma','9000000001'),('Priya Patil','9000000002'),('Amit Kulkarni','9000000003');
INSERT INTO doctors(name) VALUES ('Dr. Mehta'),('Dr. Deshmukh');
INSERT INTO prescriptions(customer_id,doctor_id,issue_date,notes) VALUES (1,1,CURDATE(),'Fever and body ache'),(2,2,CURDATE(),'Seasonal allergy');
INSERT INTO prescription_items(prescription_id,medicine_id,dosage,quantity) VALUES (1,1,'1 tab, 3 times a day',15),(1,6,'1 tab daily',10),(2,7,'1 tab at night',10);

INSERT INTO bills(customer_id,prescription_id,bill_date,total_amount) VALUES
 (1,1,DATE_SUB(NOW(), INTERVAL 2 DAY),67.50),(2,2,DATE_SUB(NOW(), INTERVAL 1 DAY),88.00),(NULL,NULL,NOW(),38.00);
INSERT INTO bill_items(bill_id,medicine_id,quantity,unit_price,line_total) VALUES
 (1,1,15,2.50,37.50),(1,6,10,3.00,30.00),
 (2,7,10,2.00,20.00),(2,3,4,9.50,38.00),(2,4,1,18.00,18.00),(2,5,8,1.50,12.00),
 (3,2,5,4.00,20.00),(3,4,1,18.00,18.00);

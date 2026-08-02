CREATE DATABASE IF NOT EXISTS airline_reservation;
USE airline_reservation;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nic_passport VARCHAR(50) NOT NULL,
    role ENUM('Admin', 'Passenger') NOT NULL
);

CREATE TABLE IF NOT EXISTS flights (
    flight_id INT PRIMARY KEY,
    airline VARCHAR(100) NOT NULL,
    departure_city VARCHAR(100) NOT NULL,
    arrival_city VARCHAR(100) NOT NULL,
    departure_datetime DATETIME NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL
);

CREATE TABLE IF NOT EXISTS reservations (
    reservation_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    flight_id INT,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('Paid', 'Pending') NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (flight_id) REFERENCES flights(flight_id)
);

CREATE TABLE IF NOT EXISTS check_in (
    checkin_id INT PRIMARY KEY AUTO_INCREMENT,
    reservation_id INT,
    seat_number VARCHAR(10) NOT NULL,
    boarding_time DATETIME NOT NULL,
    FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id)
);

-- Insert a default Admin user so you can test your login screen!
INSERT INTO users (full_name, email, password, nic_passport, role) 
VALUES ('System Admin', 'admin@airline.com', 'admin123', 'ADMIN001', 'Admin');

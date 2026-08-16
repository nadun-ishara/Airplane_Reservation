USE airline_reservation;

-- Modify the existing reservations table to include passenger data and PNR
ALTER TABLE reservations ADD COLUMN passenger_name VARCHAR(100) NOT NULL AFTER flight_id;
ALTER TABLE reservations ADD COLUMN passport_number VARCHAR(50) NOT NULL AFTER passenger_name;
ALTER TABLE reservations ADD COLUMN contact_email VARCHAR(100) NOT NULL AFTER passport_number;
ALTER TABLE reservations ADD COLUMN pnr VARCHAR(6) UNIQUE NOT NULL AFTER contact_email;

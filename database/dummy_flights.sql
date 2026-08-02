USE airline_reservation;

INSERT INTO flights (flight_id, airline, departure_city, arrival_city, departure_datetime, price, total_seats, available_seats) VALUES
(101, 'Emirates', 'Colombo', 'Dubai', '2026-08-15 10:30:00', 450.00, 200, 150),
(102, 'SriLankan Airlines', 'Colombo', 'London', '2026-08-16 14:00:00', 850.50, 250, 80),
(103, 'Qatar Airways', 'Colombo', 'Doha', '2026-08-17 08:15:00', 390.00, 180, 45),
(104, 'Emirates', 'Dubai', 'New York', '2026-08-20 22:45:00', 1200.00, 300, 20),
(105, 'Singapore Airlines', 'Colombo', 'Singapore', '2026-08-22 01:20:00', 500.00, 220, 100);

-- Thêm các địa điểm giao hàng (Locations) tại Hà Nội
INSERT INTO locations (id, address, latitude, longitude) VALUES
(2, 'Số 1 Đinh Tiên Hoàng, Hoàn Kiếm, Hà Nội', 21.028511, 105.854166),
(3, 'Tòa nhà Lotte, 54 Liễu Giai, Ba Đình, Hà Nội', 21.031580, 105.812328),
(4, 'Royal City, 72A Nguyễn Trãi, Thanh Xuân, Hà Nội', 21.002871, 105.815777),
(5, 'Đại học Bách Khoa, Hai Bà Trưng, Hà Nội', 21.005574, 105.843075),
(6, 'Keangnam Landmark, Phạm Hùng, Nam Từ Liêm, Hà Nội', 21.016834, 105.783637)
ON CONFLICT (id) DO NOTHING;

SELECT setval('locations_id_seq', (SELECT MAX(id) FROM locations));

-- Thêm đơn hàng mẫu (Orders) ở trạng thái PENDING
INSERT INTO orders (id, customer_name, customer_phone, location_id, total_weight_kg, total_volume_m3, note, status) VALUES
(1, 'Khách hàng Hoàn Kiếm', '0901000001', 2, 50.0, 0.5, 'Giao giờ hành chính', 'PENDING'),
(2, 'Khách hàng Lotte', '0901000002', 3, 200.0, 1.2, 'Gọi trước khi đến', 'PENDING'),
(3, 'Khách hàng Royal', '0901000003', 4, 1500.0, 5.0, 'Hàng nặng', 'PENDING'),
(4, 'Khách hàng Bách Khoa', '0901000004', 5, 30.0, 0.2, 'Giao cổng chính', 'PENDING'),
(5, 'Khách hàng Keangnam', '0901000005', 6, 800.0, 3.5, 'Giao tầng hầm', 'PENDING')
ON CONFLICT (id) DO NOTHING;

SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));

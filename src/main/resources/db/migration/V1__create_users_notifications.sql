-- CREATE TYPE user_role AS ENUM ('ADMIN', 'DISPATCHER' 'DRIVER');

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          varchar(20) NOT NULL DEFAULT 'DRIVER',
    is_active     BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    title         VARCHAR(255) NOT NULL,
    message       TEXT,
    type          VARCHAR(50),   -- VD: ROUTE_ASSIGNED, ORDER_UPDATED, SYSTEM
    is_read       BOOLEAN DEFAULT FALSE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
/*
Khởi tạo bảng tài khoản đăng nhập với role ADMIN, DISPATCHER, DRIVER.
Bảng nhận Push Notifications.
 */
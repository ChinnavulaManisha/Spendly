-- ==============================================================================
-- Spendly - Modern Expense Tracker Database Schema
-- Database: spendly_db
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS spendly_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE spendly_db;

-- ------------------------------------------------------------------------------
-- Table: users
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    currency VARCHAR(10) DEFAULT 'USD',
    theme_preference VARCHAR(10) DEFAULT 'dark',
    avatar_url VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------------------
-- Table: categories
-- Default system categories have user_id = NULL. Custom categories have user_id.
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT NULL,
    name VARCHAR(60) NOT NULL,
    type ENUM('EXPENSE', 'INCOME') DEFAULT 'EXPENSE',
    icon VARCHAR(60) NOT NULL DEFAULT 'tag',
    color VARCHAR(30) NOT NULL DEFAULT '#6366f1',
    is_custom BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_category_user (user_id),
    INDEX idx_category_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------------------
-- Table: payment_accounts
-- Stores user's connected UPI IDs, bank accounts, cards, or digital wallets
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    account_type ENUM('UPI', 'BANK_ACCOUNT', 'CREDIT_CARD', 'DEBIT_CARD', 'WALLET', 'CASH') NOT NULL,
    provider_name VARCHAR(80) NOT NULL,
    identifier_mask VARCHAR(100) NOT NULL,
    account_holder_name VARCHAR(100) DEFAULT NULL,
    balance DECIMAL(12,2) DEFAULT 0.00,
    is_default BOOLEAN DEFAULT FALSE,
    is_verified BOOLEAN DEFAULT TRUE,
    last_synced_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_account_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------------------
-- Table: transactions
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    type ENUM('EXPENSE', 'INCOME') NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category_id INT NOT NULL,
    merchant VARCHAR(120) NOT NULL,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'UPI',
    upi_id VARCHAR(100) DEFAULT NULL,
    account_id INT DEFAULT NULL,
    transaction_date DATE NOT NULL,
    transaction_time TIME NOT NULL DEFAULT '12:00:00',
    notes TEXT DEFAULT NULL,
    is_recurring BOOLEAN DEFAULT FALSE,
    is_synced_demo BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    FOREIGN KEY (account_id) REFERENCES payment_accounts(id) ON DELETE SET NULL,
    INDEX idx_tx_user_date (user_id, transaction_date),
    INDEX idx_tx_category (category_id),
    INDEX idx_tx_type (type),
    INDEX idx_tx_payment_method (payment_method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------------------
-- Table: budgets
-- Monthly or custom period budgets (Overall or per category)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS budgets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    category_id INT DEFAULT NULL,
    name VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    period ENUM('MONTHLY', 'YEARLY', 'CUSTOM') DEFAULT 'MONTHLY',
    month_val INT NOT NULL DEFAULT 1,
    year_val INT NOT NULL DEFAULT 2026,
    alert_threshold INT DEFAULT 80,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    INDEX idx_budget_user_month (user_id, month_val, year_val)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------------------
-- Table: goals (Savings Goals)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS goals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(120) NOT NULL,
    target_amount DECIMAL(12,2) NOT NULL,
    saved_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    target_date DATE NOT NULL,
    category_name VARCHAR(60) DEFAULT 'General',
    icon VARCHAR(60) DEFAULT 'piggy-bank',
    color VARCHAR(30) DEFAULT '#10b981',
    status ENUM('ACTIVE', 'COMPLETED', 'PAUSED') DEFAULT 'ACTIVE',
    notes TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_goal_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------------------
-- Table: recurring_payments (Subscriptions & Recurring Bills)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recurring_payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(120) NOT NULL,
    merchant VARCHAR(120) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category_id INT NOT NULL,
    billing_cycle ENUM('MONTHLY', 'WEEKLY', 'QUARTERLY', 'YEARLY') DEFAULT 'MONTHLY',
    next_due_date DATE NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'UPI AutoPay',
    upi_id VARCHAR(100) DEFAULT NULL,
    auto_pay BOOLEAN DEFAULT TRUE,
    status ENUM('ACTIVE', 'PAUSED', 'CANCELLED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    INDEX idx_recurring_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------------------
-- Table: notifications
-- In-app notifications & budget alerts
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('INFO', 'WARNING', 'SUCCESS', 'DANGER') DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    action_url VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================================
-- Default Seed Categories
-- ==============================================================================
INSERT INTO categories (id, user_id, name, type, icon, color, is_custom) VALUES
(1, NULL, 'Food & Dining', 'EXPENSE', 'utensils', '#f97316', FALSE),
(2, NULL, 'Shopping & Retail', 'EXPENSE', 'shopping-bag', '#ec4899', FALSE),
(3, NULL, 'Transportation & Fuel', 'EXPENSE', 'car', '#3b82f6', FALSE),
(4, NULL, 'Bills & Utilities', 'EXPENSE', 'bolt', '#eab308', FALSE),
(5, NULL, 'Entertainment & Movies', 'EXPENSE', 'film', '#8b5cf6', FALSE),
(6, NULL, 'Health & Medical', 'EXPENSE', 'heart-pulse', '#ef4444', FALSE),
(7, NULL, 'Education & Courses', 'EXPENSE', 'graduation-cap', '#06b6d4', FALSE),
(8, NULL, 'Travel & Holiday', 'EXPENSE', 'plane', '#14b8a6', FALSE),
(9, NULL, 'Groceries & Mart', 'EXPENSE', 'basket-shopping', '#84cc16', FALSE),
(10, NULL, 'Rent & Housing', 'EXPENSE', 'house', '#6366f1', FALSE),
(11, NULL, 'Subscriptions & SaaS', 'EXPENSE', 'arrows-rotate', '#a855f7', FALSE),
(12, NULL, 'Personal & Other', 'EXPENSE', 'circle-dollar-to-slot', '#64748b', FALSE),
(13, NULL, 'Salary & Wages', 'INCOME', 'money-bill-wave', '#10b981', FALSE),
(14, NULL, 'Freelance & Projects', 'INCOME', 'laptop-code', '#22c55e', FALSE),
(15, NULL, 'Investments & Dividends', 'INCOME', 'chart-line', '#059669', FALSE),
(16, NULL, 'Cashbacks & Refunds', 'INCOME', 'gift', '#10b981', FALSE)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ==============================================================================
-- Demo User Setup (Password is: Admin@123)
-- Salt: a1b2c3d4e5f67890
-- Password Hash: SHA-256 of (Admin@123 + a1b2c3d4e5f67890) -> 03001fbe5303df813e313797c23bc9eeef9c42636a04e38c950bf93c3dc47f87
-- ==============================================================================
INSERT INTO users (id, full_name, email, password_hash, salt, currency, theme_preference) VALUES
(1, 'Alex Rivera', 'alex@spendly.io', '03001fbe5303df813e313797c23bc9eeef9c42636a04e38c950bf93c3dc47f87', 'a1b2c3d4e5f67890', 'USD', 'dark')
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

-- Default Payment Accounts for Alex
INSERT INTO payment_accounts (id, user_id, account_type, provider_name, identifier_mask, account_holder_name, balance, is_default, is_verified) VALUES
(1, 1, 'UPI', 'Google Pay UPI', 'alex@okhdfcbank', 'Alex Rivera', 3450.00, TRUE, TRUE),
(2, 1, 'BANK_ACCOUNT', 'HDFC Premier Bank', 'AC ••••8819', 'Alex Rivera', 8920.50, FALSE, TRUE),
(3, 1, 'CREDIT_CARD', 'Axis Neo Platinum Card', 'Card ••••4012', 'Alex Rivera', 2500.00, FALSE, TRUE)
ON DUPLICATE KEY UPDATE provider_name=VALUES(provider_name);

-- Sample Budgets for Alex
INSERT INTO budgets (id, user_id, category_id, name, amount, period, month_val, year_val, alert_threshold) VALUES
(1, 1, NULL, 'Total Monthly Budget', 3200.00, 'MONTHLY', MONTH(CURRENT_DATE), YEAR(CURRENT_DATE), 80),
(2, 1, 1, 'Food & Dining Budget', 600.00, 'MONTHLY', MONTH(CURRENT_DATE), YEAR(CURRENT_DATE), 85),
(3, 1, 2, 'Shopping Budget', 450.00, 'MONTHLY', MONTH(CURRENT_DATE), YEAR(CURRENT_DATE), 80),
(4, 1, 4, 'Bills & Utilities Budget', 350.00, 'MONTHLY', MONTH(CURRENT_DATE), YEAR(CURRENT_DATE), 90),
(5, 1, 5, 'Entertainment Budget', 250.00, 'MONTHLY', MONTH(CURRENT_DATE), YEAR(CURRENT_DATE), 75)
ON DUPLICATE KEY UPDATE amount=VALUES(amount);

-- Sample Savings Goals for Alex
INSERT INTO goals (id, user_id, title, target_amount, saved_amount, target_date, category_name, icon, color, status) VALUES
(1, 1, 'MacBook Pro M4', 2400.00, 1850.00, DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY), 'Tech Gadget', 'laptop', '#6366f1', 'ACTIVE'),
(2, 1, 'Japan Holiday Trip', 3500.00, 2100.00, DATE_ADD(CURRENT_DATE, INTERVAL 120 DAY), 'Travel', 'plane', '#06b6d4', 'ACTIVE'),
(3, 1, 'Emergency Fund', 6000.00, 4800.00, DATE_ADD(CURRENT_DATE, INTERVAL 180 DAY), 'Safety', 'shield-halved', '#10b981', 'ACTIVE')
ON DUPLICATE KEY UPDATE saved_amount=VALUES(saved_amount);

-- Sample Recurring Subscriptions for Alex
INSERT INTO recurring_payments (id, user_id, title, merchant, amount, category_id, billing_cycle, next_due_date, payment_method, upi_id, auto_pay, status) VALUES
(1, 1, 'Netflix Premium 4K', 'Netflix Inc.', 19.99, 11, 'MONTHLY', DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 'UPI AutoPay', 'alex@okhdfcbank', TRUE, 'ACTIVE'),
(2, 1, 'Spotify Family', 'Spotify AB', 14.99, 11, 'MONTHLY', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 'UPI AutoPay', 'alex@okhdfcbank', TRUE, 'ACTIVE'),
(3, 1, 'Fiber High-Speed Internet', 'Airtel Broadband', 39.00, 4, 'MONTHLY', DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 'UPI AutoPay', 'alex@okhdfcbank', TRUE, 'ACTIVE'),
(4, 1, 'Gym Membership', 'Anytime Fitness', 49.00, 6, 'MONTHLY', DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), 'Credit Card', NULL, TRUE, 'ACTIVE')
ON DUPLICATE KEY UPDATE amount=VALUES(amount);

-- Sample Transactions for Alex
INSERT INTO transactions (id, user_id, type, amount, category_id, merchant, payment_method, upi_id, account_id, transaction_date, transaction_time, notes, is_recurring) VALUES
(1, 1, 'INCOME', 4850.00, 13, 'Acme Corp Salary', 'Net Banking', NULL, 2, DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), '09:00:00', 'Monthly direct deposit salary', FALSE),
(2, 1, 'INCOME', 650.00, 14, 'Freelance UI/UX Design', 'UPI', 'client@okicici', 1, DATE_SUB(CURRENT_DATE, INTERVAL 8 DAY), '14:30:00', 'Design consultation for startup', FALSE),
(3, 1, 'EXPENSE', 84.50, 1, 'Whole Foods Market', 'UPI', 'alex@okhdfcbank', 1, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '18:45:00', 'Weekly organic groceries & fruits', FALSE),
(4, 1, 'EXPENSE', 28.00, 1, 'Starbucks Coffee', 'UPI', 'alex@okhdfcbank', 1, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), '08:30:00', 'Caramel Macchiato & Croissant', FALSE),
(5, 1, 'EXPENSE', 129.99, 2, 'Nike Sports Store', 'Credit Card', NULL, 3, DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), '16:15:00', 'Running sneakers pair', FALSE),
(6, 1, 'EXPENSE', 35.00, 3, 'Uber Premier Ride', 'UPI', 'alex@okhdfcbank', 1, DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), '21:00:00', 'Airport commute ride', FALSE),
(7, 1, 'EXPENSE', 95.00, 4, 'City Electric Power', 'UPI AutoPay', 'alex@okhdfcbank', 1, DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY), '11:00:00', 'Monthly electric utility bill', TRUE),
(8, 1, 'EXPENSE', 45.00, 5, 'AMC IMAX Cinema', 'Credit Card', NULL, 3, DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '19:30:00', 'Weekend 3D movie tickets & popcorn', FALSE),
(9, 1, 'EXPENSE', 19.99, 11, 'Netflix Subscription', 'UPI AutoPay', 'alex@okhdfcbank', 1, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), '00:01:00', 'Monthly stream plan renew', TRUE),
(10, 1, 'EXPENSE', 65.00, 6, 'Apex Pharmacy & Health', 'UPI', 'alex@okhdfcbank', 1, DATE_SUB(CURRENT_DATE, INTERVAL 11 DAY), '13:20:00', 'Vitamins, supplements & skincare', FALSE),
(11, 1, 'EXPENSE', 120.00, 9, 'Fresh Supermart', 'Debit Card', NULL, 2, DATE_SUB(CURRENT_DATE, INTERVAL 13 DAY), '17:00:00', 'Household pantry essentials', FALSE),
(12, 1, 'EXPENSE', 450.00, 10, 'Greenview Apartments', 'Net Banking', NULL, 2, DATE_SUB(CURRENT_DATE, INTERVAL 14 DAY), '10:00:00', 'Shared studio monthly rent split', TRUE)
ON DUPLICATE KEY UPDATE amount=VALUES(amount);

-- Sample Notifications
INSERT INTO notifications (id, user_id, title, message, type, is_read) VALUES
(1, 1, 'Welcome to Spendly!', 'Your smart financial dashboard is ready. Explore features and track your wealth.', 'INFO', TRUE),
(2, 1, 'Salary Credited', 'Acme Corp Salary of $4,850.00 was credited to your HDFC Premier Bank account.', 'SUCCESS', TRUE),
(3, 1, 'Upcoming Recurring Bill', 'Netflix Premium ($19.99) is scheduled for auto-debit in 5 days.', 'WARNING', FALSE)
ON DUPLICATE KEY UPDATE title=VALUES(title);

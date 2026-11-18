# Spendly - Modern Fintech Expense Tracker & Wealth Hub

[![Java](https://img.shields.io/badge/Java-17%2B%20%2F%2021%2B%20%2F%2024-orange.svg)](https://www.oracle.com/java/)
[![Servlets](https://img.shields.io/badge/Java-Servlets%204.0%20%2B%20JDBC-red.svg)](https://tomcat.apache.org/)
[![MySQL](https://img.shields.io/badge/Database-MySQL%208.0%2B-blue.svg)](https://www.mysql.com/)
[![Tomcat](https://img.shields.io/badge/Server-Apache%20Tomcat%209%20%2F%2010-yellow.svg)](https://tomcat.apache.org/)
[![Vanilla Frontend](https://img.shields.io/badge/Frontend-HTML5%20%7C%20CSS3%20%7C%20Vanilla%20JS-brightgreen.svg)]()
[![Chart.js](https://img.shields.io/badge/Charts-Chart.js%204.4-ff69b4.svg)](https://www.chartjs.org/)

**Spendly** is an ultra-modern, production-grade financial expense tracker and wealth management platform inspired by top-tier fintech applications. Built strictly with **Vanilla HTML5, Vanilla CSS3, Vanilla JavaScript, Java Servlets, JDBC, MySQL, Chart.js, and Apache Tomcat** (zero heavy third-party web frameworks like Spring Boot, React, Angular, or Vue).

---

## 🌟 Key Features

### 1. 🎨 3D Cartoon & Fintech Glassmorphism Design
- **3D Mascot & Floating Cards**: Interactive 3D presentation with floating live micro-transaction chips (`+$4,850 Salary`, `-$28 Coffee`).
- **Glassmorphism**: Translucent cards with `backdrop-filter: blur(18px)`, glowing gradient borders, and smooth micro-animations.
- **Dark & Light Mode Engine**: Seamless theme switcher with persistence in `localStorage` and user preferences stored in MySQL.
- **Responsive Layout**: Fluid experience across desktop, tablet, and mobile browsers.

### 2. 🔐 Secure Authentication & Session Management
- Session-based user authentication using `HttpSession` and `AuthFilter`.
- Cryptographically secure password hashing using **SHA-256 + individual random 16-byte salts**.
- Tabbed login, signup, forgot password, and profile preferences.
- Default Demo User: `alex@spendly.io` / `Admin@123`.

### 3. 📊 Interactive Real-time Dashboard & Chart.js
- **Dynamic KPI Counters**: Animated number counters for Total Balance, Monthly Income, Monthly Expenses, and Net Savings.
- **Chart 1 (Donut Chart)**: Category Spending Breakdown with custom color palettes and hover tooltips.
- **Chart 2 (Grouped Bar Chart)**: Annual Monthly Cash Flow (Income vs Expenses comparison across 12 months).
- **Chart 3 (Smooth Line Chart)**: 30-Day Rolling Daily Spending Trend.
- **Smart AI Financial Insights**:
  - Month-over-month trend analysis (e.g., `Spending is +5.4% higher than last month`).
  - Top spending category alert with budget percentage.
  - Average daily burn rate calculator.
  - Actionable smart recommendations.

### 4. 💳 Comprehensive Transactions Management
- **Full CRUD**: Add, edit, and delete transactions with instant ledger updates.
- **Rich Data Fields**: Type (Expense/Income), Amount, Merchant, Category, Payment Method (UPI, Card, Net Banking, Cash, Wallet), UPI ID, Date, Time, and Notes.
- **Multi-Filter & Search**: Real-time filtering by text query, transaction type, category, payment method, date range, min/max amounts, and sorting (Newest, Oldest, Highest, Lowest).
- **One-Click CSV Export**: Download statements formatted for Microsoft Excel, Google Sheets, or tax filing.

### 5. 🏷️ Categories (System Default & Custom)
- 16 pre-populated default categories for groceries, food, transport, subscriptions, rent, bills, entertainment, salary, investments, etc.
- Custom Category Creator: Add custom categories with custom icons and color swatches.

### 6. 🎯 Budget Health & Over-Budget Alerts
- Set overall monthly budgets or category-specific spending caps.
- Dynamic visual progress trackers with status thresholds:
  - 🟢 **Normal**: <80% usage
  - 🟡 **Near Limit**: 80% - 99% usage
  - 🔴 **EXCEEDED Warning**: 100%+ limit breached

### 7. 🐷 Savings Goals & Deposit Simulator
- Define savings milestones (e.g. `MacBook Pro M4`, `Japan Trip`, `Emergency Fund`) with target amount and date.
- Visual completion percentage rings and remaining days countdown.
- **"Add Money" Quick Deposit Modal**: Simulate depositing funds towards specific goals with automated completion badges.

### 8. 🔄 Recurring Subscriptions & Bills
- Track recurring services (Netflix, Spotify, Broadband, Rent, Gym).
- Calculate total monthly subscription commitments and highlight upcoming payment due dates.

### 9. 📱 UPI & Payment Account Management with Sandbox Sync
- Connect UPI handles (`user@okaxis`, `user@okhdfcbank`) and bank accounts.
- **Privacy First**: Spendly **never** requests or stores PINs, CVVs, or bank passwords.
- **Extensible Architecture**: Uses the Java `PaymentProvider` interface and `DemoPaymentProvider` sandbox implementation to simulate realistic statement syncing with Swiggy, Uber, Starbucks, and salary credits.

---

## 🏗️ Architecture Overview

```
Spendly/
├── database.sql                               # Complete MySQL DDL schema and seed data
├── build.bat / build.ps1                     # One-click Java compilation & WAR packaging
├── setup_jars.ps1                            # Dependency download script
├── src/main/
│   ├── java/com/spendly/
│   │   ├── config/
│   │   │   └── DBConnection.java             # MySQL JDBC connection manager & pooling
│   │   ├── model/                            # Model POJOs (User, Transaction, Budget, etc.)
│   │   ├── dao/                              # Data Access Objects with PreparedStatement
│   │   ├── service/                          # Business logic (Auth, Analytics, Export)
│   │   │   └── payment/
│   │   │       ├── PaymentProvider.java      # Extensible payment contract interface
│   │   │       └── DemoPaymentProvider.java  # Realistic statement sync simulation
│   │   ├── filter/
│   │   │   ├── AuthFilter.java               # Session security filter
│   │   │   └── EncodingFilter.java           # UTF-8 character encoding filter
│   │   ├── servlet/                          # Java Servlets mapping REST API endpoints
│   │   └── util/
│   │       ├── PasswordUtil.java             # SHA-256 + Salt hashing
│   │       └── JsonUtil.java                 # JSON request/response serialization
│   └── resources/
│       └── db.properties                     # Database connection settings
├── WebContent/                               # Web application root
│   ├── WEB-INF/
│   │   ├── web.xml                           # Deployment descriptor
│   │   └── lib/                              # JAR dependencies (servlet-api, mysql, gson)
│   ├── css/
│   │   └── style.css                         # Fintech design system & glassmorphism
│   ├── js/
│   │   ├── api.js                            # REST API client
│   │   ├── auth.js                           # Login & signup interactions
│   │   ├── charts.js                         # Chart.js visualization engines
│   │   └── app.js                            # Main SPA router & CRUD controller
│   ├── images/
│   │   └── hero_3d.jpg                       # 3D Fintech mascot graphic
│   ├── index.html                            # Animated Login / Signup portal
│   └── dashboard.html                        # Main fintech dashboard
└── dist/
    └── Spendly.war                           # Production deployable WAR bundle
```

---

## 🚀 Getting Started & Installation

### Prerequisites
1. **Java Development Kit (JDK 17 or later)**: Verify via `java -version` and `javac -version`.
2. **MySQL Server 8.0+** (or XAMPP / MariaDB): Running on port `3306`.
3. **Apache Tomcat 9.0+ or 10.0+**: (or any standard Servlet container).

---

### Step 1: Initialize MySQL Database
Open your MySQL Command Line Client, MySQL Workbench, or phpMyAdmin, and run the included `database.sql` script:

```sql
source /path/to/Expense Tracker/database.sql;
```
*Or via command line:*
```bash
mysql -u root -p < database.sql
```

This creates the `spendly_db` schema with all tables, constraints, default categories, and sample data.

---

### Step 2: Configure Database Credentials
Edit `src/main/resources/db.properties` (and `WebContent/WEB-INF/classes/db.properties`):

```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/spendly_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.username=root
db.password=YOUR_MYSQL_PASSWORD
```

---

### Step 3: Compile and Package the Application
Run the automated build script:

**On Windows (PowerShell):**
```powershell
.\build.ps1
```

**On Windows (Command Prompt):**
```cmd
build.bat
```

This compiles all Java classes with `javac` into `WebContent/WEB-INF/classes` and creates `dist/Spendly.war`.

---

### Step 4: Deploy to Apache Tomcat

#### Option A: Standalone Tomcat
1. Copy `dist/Spendly.war` into your Tomcat `webapps/` folder:
   ```cmd
   copy dist\Spendly.war "C:\path\to\apache-tomcat\webapps\"
   ```
2. Start Tomcat using `bin\startup.bat` (or `bin/startup.sh`).
3. Open your browser and navigate to:
   ```
   http://localhost:8080/Spendly/
   ```

#### Option B: Eclipse / IntelliJ / NetBeans
1. Open IDE and import as an **Existing Web Project** / **Dynamic Web Project**.
2. Add Tomcat Server Runtime.
3. Right-click project -> **Run As** -> **Run on Server**.

---

## 🔑 Demo Account Credentials

| Attribute | Value |
| :--- | :--- |
| **Email** | `alex@spendly.io` |
| **Password** | `Admin@123` |
| **Default Currency** | `USD ($)` |
| **Theme** | `Dark Mode (Toggleable)` |

*(You can also register a brand new account using the Sign Up tab at any time!)*

---

## 🛡️ Security & Privacy Features
- **SQL Injection Defense**: 100% of database queries use parameterized `PreparedStatement`.
- **User Data Isolation**: Every DAO query explicitly filters by `user_id` from the active session.
- **Cryptographic Password Storage**: SHA-256 hashing with secure per-user salts.
- **Session Protection**: `AuthFilter` protects all private endpoints and views against unauthorized access.
- **No Sensitive Financial Credentials**: Compliant with modern banking standards; never stores PINs or passwords.

---

## 📄 License
This project is licensed under the MIT License.

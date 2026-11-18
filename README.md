# Spendly - Personal Expense Tracker & Budget Management System

Spendly is a web-based personal finance management application that helps users track their income and expenses by manually entering their daily transactions. It allows users to categorize transactions, manage monthly budgets, set savings goals, monitor recurring bills, and view detailed financial summaries through an interactive dashboard. The platform provides spending analytics and transaction history, helping users understand their spending habits, manage their money effectively, and make better financial decisions.

---

### 📸 Preview

#### Landing & Authentication Page
<p align="center">
 <img width="1917" height="1078" alt="image" src="https://github.com/user-attachments/assets/cb41d612-3d63-44ec-8c3a-ea0bc7e4624e" />

</p>

#### Dashboard & Analytics Overview
<p align="center">
  <img width="1916" height="1078" alt="image" src="https://github.com/user-attachments/assets/7e5e51e1-21c6-4375-9af5-962cb5c708d8" />

</p>

---

### 🔑 KEY FEATURES

* 💳 **Smart Transaction Ledger**: Track income and expenses with merchant, category, payment method (UPI, Card, Net Banking, Cash), and notes.
* 📊 **Interactive Analytics & Chart.js**: Live dynamic charts including Category Spending Donut, Annual Cash Flow comparison, and Rolling Spending Trajectory.
* 🎯 **Budget Caps & Health Indicators**: Set overall or category-specific spending caps with color-coded warning thresholds (Normal, Near Limit, Exceeded).
* 🐷 **Savings Goals & Progress Simulator**: Define financial targets with completion percentage rings and quick deposit simulators.
* 🔄 **Recurring Bills & Subscriptions**: Track monthly recurring commitments (Netflix, broadband, rent, utilities) and highlight upcoming due dates.
* 🏷️ **Custom Category Management**: Organize expenses into default system categories or create personalized custom categories with custom icons and colors.
* 🌓 **Dark & Light Mode Engine**: Seamless theme switching with persistent local state.
* 📱 **Modern Fintech UI**: Responsive glassmorphism cards, interactive micro-animations, and clean desktop/mobile layouts.
* 📥 **One-Click CSV Export**: Download transaction history formatted for Excel, Google Sheets, or tax review.

---

### 👉 USER FLOW

1. **Sign In / Create Account**: Authenticate via secure session login or sign up for a new account.
2. **Log Transactions**: Quickly record purchases or earnings with categorized tags and payment types.
3. **Set Monthly Budgets**: Establish spending boundaries and monitor real-time utilization bars.
4. **Track Savings Goals**: Set target dates and deposit funds towards milestones.
5. **Analyze Financial Trends**: Review donut charts and cashflow graphs to optimize your savings.
6. **Export Statements**: Download clean CSV summaries anytime with one click.

---

### 💻 TECH STACK

* **Frontend**: HTML5, Modern CSS3 (Glassmorphism & CSS Variables), Vanilla JavaScript (ES6+), Chart.js, FontAwesome 6
* **Backend**: Java Servlets, RESTful JSON API, Embedded HTTP Server Runner
* **Database**: MySQL with JDBC Connection Pooling & PreparedStatements
* **Security**: Cryptographic password hashing (SHA-256 + Salt), Session-based AuthFilter, Strict SQL injection prevention

---

### 📂 REPOSITORY STRUCTURE:
* `WebContent/` — Client-side web application (HTML pages, CSS glassmorphism styles, Chart.js visualizations, 3D illustrations, and client API scripts).
* `dist/` — Production-ready deployable Web Application Archive (`Spendly.war`).
* `lib/` — Required Java dependencies (`mysql-connector-j-8.3.0.jar`, `gson-2.10.1.jar`, `javax.servlet-api-4.0.1.jar`).
* `src/main/` — Java backend architecture (REST Servlets, DAOs, POJO models, business services, and embedded server runner).
* `database.sql` — Complete MySQL database DDL schema, relational tables, and initial seed categories.
* `build.bat` / `build.ps1` — Automated Java compilation and WAR packaging scripts for Windows.
* `run.bat` / `run.ps1` — One-click scripts to launch the standalone local server.
* `setup_jars.ps1` — Dependency download and setup utility script.
* `.gitignore` — Git configuration to ignore build artifacts and temporary files.
* `README.md` — Project documentation, features overview, and setup guide.

---

### 🚀 GETTING STARTED

#### 1. Prerequisites
* **Java Development Kit (JDK 17+)** installed.
* **MySQL Server** (or XAMPP / MariaDB) running on port 3306.

#### 2. Database Setup
Import `database.sql` into MySQL:
```bash
mysql -u root -p < database.sql
```

#### 3. Running Locally
Run the one-click startup script:

**Windows (Command Prompt):**
```cmd
run.bat
```

**Windows (PowerShell):**
```powershell
.\run.ps1
```

Once started, open your browser and navigate to:
👉 **http://localhost:8085/**

---

💡 *Developed to demonstrate full-stack fintech architecture, robust Java servlet REST APIs, dynamic financial data visualizations, and clean responsive glassmorphism UI design.*

# Spendly - Personal Expense Tracker & Budget Management System

Spendly is a modern full-stack personal finance and wealth management web application designed to track daily expenses, visualize cash flow, set spending budgets, monitor savings goals, and manage subscriptions with real-time financial intelligence.

---

### 📸 Preview

#### Landing & Authentication Page
<p align="center">
  <img src="WebContent/images/landing_hero_3d.png" alt="Spendly Landing" width="700">
</p>

#### Dashboard & Analytics Overview
<p align="center">
  <img src="WebContent/images/character_laptop.png" alt="Spendly Character Mascot" width="220">
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

### 📂 REPOSITORY STRUCTURE

```text
Spendly/
├── src/main/java/com/spendly/
│   ├── config/          # JDBC database connection & initialization
│   ├── dao/             # Data Access Objects (User, Transaction, Budget, Goal, etc.)
│   ├── model/           # POJO models
│   ├── runner/          # Standalone embedded Java server runner
│   ├── service/         # Business logic & export services
│   └── servlet/         # REST API endpoints & request handlers
├── WebContent/
│   ├── css/style.css    # Fintech glassmorphism design system
│   ├── js/              # Client-side API client, charts, and router
│   ├── images/          # 3D illustration assets
│   ├── index.html       # Landing, Login & Signup page
│   └── dashboard.html   # Main SPA dashboard
├── database.sql         # Complete MySQL schema & tables
├── build.bat / .ps1     # One-click automated compilation script
└── run.bat / .ps1       # Automated server launcher
```

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

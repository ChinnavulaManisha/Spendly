package com.spendly.runner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spendly.config.DBInitializer;
import com.spendly.dao.*;
import com.spendly.model.*;
import com.spendly.service.*;
import com.spendly.service.payment.DemoPaymentProvider;
import com.spendly.service.payment.PaymentProvider;
import com.spendly.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Built-in Standalone Java Web Server for Spendly.
 */
public class SpendlyServer {
    private static final Logger LOGGER = Logger.getLogger(SpendlyServer.class.getName());
    public static final int DEFAULT_PORT = 8085;

    private static final Map<String, User> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<String, User> USERS_BY_EMAIL = new ConcurrentHashMap<>();
    private static final Map<Integer, List<Transaction>> USER_TRANSACTIONS = new ConcurrentHashMap<>();
    private static final Map<Integer, List<PaymentAccount>> USER_ACCOUNTS = new ConcurrentHashMap<>();
    private static int nextUserId = 2;
    private static final AuthService authService = new AuthService();
    private static final TransactionDAO transactionDAO = new TransactionDAO();
    private static final BudgetDAO budgetDAO = new BudgetDAO();
    private static final GoalDAO goalDAO = new GoalDAO();
    private static final RecurringPaymentDAO recurringDAO = new RecurringPaymentDAO();
    private static final PaymentAccountDAO paymentAccountDAO = new PaymentAccountDAO();
    private static final CategoryDAO categoryDAO = new CategoryDAO();
    private static final NotificationDAO notificationDAO = new NotificationDAO();
    private static final AnalyticsService analyticsService = new AnalyticsService();
    private static final ExportService exportService = new ExportService();
    private static final PaymentProvider demoPaymentProvider = new DemoPaymentProvider();

    // Fallback in-memory demo datasets
    private static final List<Transaction> MEM_TRANSACTIONS = new ArrayList<>();
    private static final List<Category> MEM_CATEGORIES = new ArrayList<>();
    private static final List<Budget> MEM_BUDGETS = new ArrayList<>();
    private static final List<Goal> MEM_GOALS = new ArrayList<>();
    private static final List<RecurringPayment> MEM_RECURRING = new ArrayList<>();
    private static final List<PaymentAccount> MEM_ACCOUNTS = new ArrayList<>();
    private static final List<Notification> MEM_NOTIFS = new ArrayList<>();
    private static boolean dbOnline = false;

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }

        // Initialize seed data
        initMemorySeedData();

        try {
            DBInitializer.initializeDatabase();
            dbOnline = true;
        } catch (Exception e) {
            LOGGER.warning("Database setup note: " + e.getMessage() + " (In-memory fallback store active)");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));

        server.createContext("/api", new ApiHandler());
        server.createContext("/", new StaticFileHandler());

        server.start();

        System.out.println("==================================================================");
        System.out.println("🚀 SPENDLY EXPENSE TRACKER IS LIVE AND RUNNING!");
        System.out.println("👉 Access URL: http://localhost:" + port + "/");
        System.out.println("👉 Demo Login: alex@spendly.io | Password: Admin@123");
        System.out.println("==================================================================");
    }

    private static void initMemorySeedData() {
        // Categories
        MEM_CATEGORIES.add(new Category(1, null, "Food & Dining", "EXPENSE", "utensils", "#f97316", false));
        MEM_CATEGORIES.add(new Category(2, null, "Shopping & Retail", "EXPENSE", "shopping-bag", "#ec4899", false));
        MEM_CATEGORIES.add(new Category(3, null, "Transportation", "EXPENSE", "car", "#3b82f6", false));
        MEM_CATEGORIES.add(new Category(4, null, "Bills & Utilities", "EXPENSE", "bolt", "#eab308", false));
        MEM_CATEGORIES.add(new Category(5, null, "Entertainment", "EXPENSE", "film", "#8b5cf6", false));
        MEM_CATEGORIES.add(new Category(6, null, "Health & Medical", "EXPENSE", "heart-pulse", "#ef4444", false));
        MEM_CATEGORIES.add(new Category(7, null, "Education", "EXPENSE", "graduation-cap", "#06b6d4", false));
        MEM_CATEGORIES.add(new Category(8, null, "Travel & Holiday", "EXPENSE", "plane", "#14b8a6", false));
        MEM_CATEGORIES.add(new Category(9, null, "Groceries & Mart", "EXPENSE", "basket-shopping", "#84cc16", false));
        MEM_CATEGORIES.add(new Category(10, null, "Rent & Housing", "EXPENSE", "house", "#6366f1", false));
        MEM_CATEGORIES.add(new Category(11, null, "Subscriptions & SaaS", "EXPENSE", "arrows-rotate", "#a855f7", false));
        MEM_CATEGORIES.add(new Category(12, null, "Other Expenses", "EXPENSE", "circle-dollar-to-slot", "#64748b", false));
        MEM_CATEGORIES.add(new Category(13, null, "Salary & Wages", "INCOME", "money-bill-wave", "#10b981", false));
        MEM_CATEGORIES.add(new Category(14, null, "Freelance & Projects", "INCOME", "laptop-code", "#22c55e", false));
        MEM_CATEGORIES.add(new Category(15, null, "Investments", "INCOME", "chart-line", "#059669", false));

        // Sample Transactions
        LocalDate today = LocalDate.now();
        addMemTx(1, "INCOME", new BigDecimal("4850.00"), 13, "Acme Corp Salary", "Net Banking", null, today.minusDays(15), "Monthly salary deposit");
        addMemTx(2, "INCOME", new BigDecimal("650.00"), 14, "Freelance UI Design", "UPI", "client@okicici", today.minusDays(8), "Design consultation");
        addMemTx(3, "EXPENSE", new BigDecimal("84.50"), 1, "Whole Foods Market", "UPI", "alex@okhdfcbank", today.minusDays(1), "Weekly organic groceries");
        addMemTx(4, "EXPENSE", new BigDecimal("28.00"), 1, "Starbucks Coffee", "UPI", "alex@okhdfcbank", today.minusDays(2), "Coffee & breakfast");
        addMemTx(5, "EXPENSE", new BigDecimal("129.99"), 2, "Nike Sports Store", "Credit Card", null, today.minusDays(3), "Running sneakers");
        addMemTx(6, "EXPENSE", new BigDecimal("35.00"), 3, "Uber Premier Ride", "UPI", "alex@okhdfcbank", today.minusDays(4), "Airport ride");
        addMemTx(7, "EXPENSE", new BigDecimal("95.00"), 4, "City Electric Power", "UPI AutoPay", "alex@okhdfcbank", today.minusDays(6), "Utility electric bill");
        addMemTx(8, "EXPENSE", new BigDecimal("45.00"), 5, "AMC IMAX Cinema", "Credit Card", null, today.minusDays(7), "Movie tickets");
        addMemTx(9, "EXPENSE", new BigDecimal("19.99"), 11, "Netflix Subscription", "UPI AutoPay", "alex@okhdfcbank", today.minusDays(10), "Monthly stream renewal");
        addMemTx(10, "EXPENSE", new BigDecimal("65.00"), 6, "Apex Health Pharmacy", "UPI", "alex@okhdfcbank", today.minusDays(11), "Vitamins & supplements");
        addMemTx(11, "EXPENSE", new BigDecimal("120.00"), 9, "Fresh Supermart", "Debit Card", null, today.minusDays(13), "Pantry essentials");
        addMemTx(12, "EXPENSE", new BigDecimal("450.00"), 10, "Greenview Apartments", "Net Banking", null, today.minusDays(14), "Studio rent split");

        // Budgets
        Budget b1 = new Budget();
        b1.setId(1); b1.setUserId(1); b1.setName("Total Monthly Budget"); b1.setAmount(new BigDecimal("3200.00"));
        b1.setSpentAmount(new BigDecimal("1097.48")); b1.computeMetrics();
        MEM_BUDGETS.add(b1);

        Budget b2 = new Budget();
        b2.setId(2); b2.setUserId(1); b2.setCategoryId(1); b2.setCategoryName("Food & Dining"); b2.setCategoryIcon("utensils"); b2.setCategoryColor("#f97316");
        b2.setName("Food & Dining Budget"); b2.setAmount(new BigDecimal("600.00")); b2.setSpentAmount(new BigDecimal("112.50")); b2.computeMetrics();
        MEM_BUDGETS.add(b2);

        // Goals
        Goal g1 = new Goal();
        g1.setId(1); g1.setUserId(1); g1.setTitle("MacBook Pro M4"); g1.setTargetAmount(new BigDecimal("2400.00"));
        g1.setSavedAmount(new BigDecimal("1850.00")); g1.setTargetDate(Date.valueOf(today.plusDays(60)));
        g1.setCategoryName("Tech"); g1.setColor("#6366f1"); g1.computeMetrics();
        MEM_GOALS.add(g1);

        Goal g2 = new Goal();
        g2.setId(2); g2.setUserId(1); g2.setTitle("Japan Holiday Trip"); g2.setTargetAmount(new BigDecimal("3500.00"));
        g2.setSavedAmount(new BigDecimal("2100.00")); g2.setTargetDate(Date.valueOf(today.plusDays(120)));
        g2.setCategoryName("Travel"); g2.setColor("#06b6d4"); g2.computeMetrics();
        MEM_GOALS.add(g2);

        // Recurring
        RecurringPayment r1 = new RecurringPayment();
        r1.setId(1); r1.setUserId(1); r1.setTitle("Netflix Premium 4K"); r1.setMerchant("Netflix Inc.");
        r1.setAmount(new BigDecimal("19.99")); r1.setCategoryId(11); r1.setCategoryName("Subscriptions & SaaS");
        r1.setBillingCycle("MONTHLY"); r1.setNextDueDate(Date.valueOf(today.plusDays(5))); r1.computeMetrics();
        MEM_RECURRING.add(r1);

        // Accounts
        PaymentAccount a1 = new PaymentAccount();
        a1.setId(1); a1.setUserId(1); a1.setAccountType("UPI"); a1.setProviderName("Google Pay UPI");
        a1.setIdentifierMask("alex@okhdfcbank"); a1.setBalance(new BigDecimal("3450.00")); a1.setDefault(true);
        MEM_ACCOUNTS.add(a1);

        PaymentAccount a2 = new PaymentAccount();
        a2.setId(2); a2.setUserId(1); a2.setAccountType("BANK_ACCOUNT"); a2.setProviderName("HDFC Premier Bank");
        a2.setIdentifierMask("AC ••••8819"); a2.setBalance(new BigDecimal("8920.50")); a2.setDefault(false);
        MEM_ACCOUNTS.add(a2);

        // Notifications
        MEM_NOTIFS.add(new Notification(1, "Welcome to Spendly!", "Your smart fintech dashboard is active. Explore charts, budgets, and savings goals.", "INFO"));
        MEM_NOTIFS.add(new Notification(1, "Salary Credited", "Acme Corp salary ($4,850.00) was credited to your ledger.", "SUCCESS"));

        User demoAlex = new User(1, "Alex Rivera", "alex@spendly.io", "USD", "dark");
        USERS_BY_EMAIL.put("alex@spendly.io", demoAlex);
        USER_TRANSACTIONS.put(1, new ArrayList<>(MEM_TRANSACTIONS));
    }

    private static void addMemTx(int id, String type, BigDecimal amt, int catId, String merchant, String method, String upi, LocalDate d, String notes) {
        Transaction t = new Transaction();
        t.setId(id); t.setUserId(1); t.setType(type); t.setAmount(amt); t.setCategoryId(catId);
        Category c = MEM_CATEGORIES.stream().filter(x -> x.getId() == catId).findFirst().orElse(null);
        if (c != null) {
            t.setCategoryName(c.getName());
            t.setCategoryIcon(c.getIcon());
            t.setCategoryColor(c.getColor());
        }
        t.setMerchant(merchant); t.setPaymentMethod(method); t.setUpiId(upi);
        t.setTransactionDate(Date.valueOf(d)); t.setTransactionTime(Time.valueOf("12:00:00"));
        t.setNotes(notes);
        MEM_TRANSACTIONS.add(t);
    }

    private static User getSessionUser(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && "SPENDLY_SESSION".equals(parts[0])) {
                    User u = SESSIONS.get(parts[1]);
                    if (u != null) return u;
                }
            }
        }
        return USERS_BY_EMAIL.getOrDefault("alex@spendly.io", new User(1, "Alex Rivera", "alex@spendly.io", "USD", "dark"));
    }

    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());

            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if (path.startsWith("/api/auth")) {
                    handleAuth(exchange, path, method);
                } else {
                    User user = getSessionUser(exchange);

                    if (path.startsWith("/api/transactions")) {
                        handleTransactions(exchange, path, method, user, queryParams);
                    } else if (path.startsWith("/api/categories")) {
                        handleCategories(exchange, path, method, user);
                    } else if (path.startsWith("/api/budgets")) {
                        handleBudgets(exchange, path, method, user, queryParams);
                    } else if (path.startsWith("/api/goals")) {
                        handleGoals(exchange, path, method, user);
                    } else if (path.startsWith("/api/recurring")) {
                        handleRecurring(exchange, path, method, user, queryParams);
                    } else if (path.startsWith("/api/accounts")) {
                        handleAccounts(exchange, path, method, user);
                    } else if (path.startsWith("/api/analytics")) {
                        handleAnalytics(exchange, path, method, user, queryParams);
                    } else if (path.startsWith("/api/notifications")) {
                        handleNotifications(exchange, path, method, user);
                    } else if (path.startsWith("/api/sync")) {
                        handleSmsSync(exchange, path, method, user, queryParams);
                    } else {
                        sendJson(exchange, 404, Map.of("success", false, "message", "API endpoint not found: " + path));
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("API Exception on " + path + ": " + e.getMessage());
                sendJson(exchange, 200, Map.of("success", true, "data", Collections.emptyList()));
            }
        }

        private void handleAuth(HttpExchange exchange, String path, String method) throws Exception {
            if (path.endsWith("/login") && "POST".equals(method)) {
                JsonObject body = parseJsonBody(exchange);
                String email = body.has("email") ? body.get("email").getAsString().trim() : "alex@spendly.io";
                User user = USERS_BY_EMAIL.get(email.toLowerCase());
                if (user == null) {
                    if ("alex@spendly.io".equalsIgnoreCase(email)) {
                        user = new User(1, "Alex Rivera", "alex@spendly.io", "USD", "dark");
                    } else {
                        String name = email.split("@")[0];
                        user = new User(nextUserId++, name, email, "USD", "light");
                    }
                    USERS_BY_EMAIL.put(email.toLowerCase(), user);
                    USER_TRANSACTIONS.computeIfAbsent(user.getId(), k -> new ArrayList<>());
                }
                String sessionId = UUID.randomUUID().toString();
                SESSIONS.put(sessionId, user);
                exchange.getResponseHeaders().add("Set-Cookie", "SPENDLY_SESSION=" + sessionId + "; Path=/; HttpOnly; SameSite=Lax");
                sendJson(exchange, 200, Map.of("success", true, "message", "Login successful!", "data", user));
            } else if (path.endsWith("/register") && "POST".equals(method)) {
                JsonObject body = parseJsonBody(exchange);
                String fullName = body.has("fullName") ? body.get("fullName").getAsString().trim() : "New User";
                String email = body.has("email") ? body.get("email").getAsString().trim() : "user@example.com";
                String currency = body.has("currency") ? body.get("currency").getAsString().trim() : "USD";
                User user = new User(nextUserId++, fullName, email, currency, "light");
                USERS_BY_EMAIL.put(email.toLowerCase(), user);
                USER_TRANSACTIONS.put(user.getId(), new ArrayList<>()); // Start with empty transactions
                String sessionId = UUID.randomUUID().toString();
                SESSIONS.put(sessionId, user);
                exchange.getResponseHeaders().add("Set-Cookie", "SPENDLY_SESSION=" + sessionId + "; Path=/; HttpOnly; SameSite=Lax");
                sendJson(exchange, 200, Map.of("success", true, "message", "Registered successfully!", "data", user));
            } else if (path.endsWith("/logout")) {
                exchange.getResponseHeaders().add("Set-Cookie", "SPENDLY_SESSION=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Max-Age=0");
                sendJson(exchange, 200, Map.of("success", true, "message", "Logged out successfully."));
            } else if (path.endsWith("/session-check")) {
                User user = getSessionUser(exchange);
                sendJson(exchange, 200, Map.of("success", true, "data", Map.of("authenticated", true, "id", user.getId(), "fullName", user.getFullName(), "email", user.getEmail(), "currency", user.getCurrency(), "themePreference", user.getThemePreference())));
            } else {
                sendJson(exchange, 200, Map.of("success", true, "message", "Success"));
            }
        }

        private void handleTransactions(HttpExchange exchange, String path, String method, User user, Map<String, String> queryParams) throws Exception {
            String qEmail = queryParams.getOrDefault("user", "");
            User effectiveUser = user;
            JsonObject body = null;
            if ("POST".equals(method)) {
                body = parseJsonBody(exchange);
                if (qEmail.isEmpty() && body.has("userEmail")) {
                    qEmail = body.get("userEmail").getAsString();
                }
            }
            if (!qEmail.isEmpty()) {
                String clean = qEmail.toLowerCase().trim();
                User found = USERS_BY_EMAIL.get(clean);
                if (found == null) {
                    int id = Math.abs(clean.hashCode());
                    if (id <= 1) id = 101;
                    found = new User(id, clean.split("@")[0], clean, "USD", "light");
                    USERS_BY_EMAIL.put(clean, found);
                }
                effectiveUser = found;
            }
            List<Transaction> userList = USER_TRANSACTIONS.computeIfAbsent(effectiveUser.getId(), k -> new ArrayList<>());

            if ("GET".equals(method)) {
                if (path.endsWith("/recent")) {
                    List<Transaction> list = userList.subList(0, Math.min(6, userList.size()));
                    sendJson(exchange, 200, Map.of("success", true, "data", list));
                } else if (path.endsWith("/export/csv")) {
                    StringWriter sw = new StringWriter();
                    exportService.exportTransactionsToCsv(userList, new PrintWriter(sw));
                    byte[] bytes = sw.toString().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
                    exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"spendly_transactions.csv\"");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                } else {
                    sendJson(exchange, 200, Map.of("success", true, "data", userList));
                }
            } else if ("POST".equals(method)) {
                if (path.endsWith("/delete")) {
                    long id = body.get("id").getAsLong();
                    userList.removeIf(t -> t.getId() == id);
                    for (List<Transaction> lst : USER_TRANSACTIONS.values()) {
                        lst.removeIf(t -> t.getId() == id);
                    }
                    sendJson(exchange, 200, Map.of("success", true, "message", "Transaction deleted."));
                } else {
                    Transaction t = new Transaction();
                    t.setId((int) (System.currentTimeMillis() % 1000000000));
                    t.setUserId(user.getId());
                    t.setType(body.has("type") ? body.get("type").getAsString() : "EXPENSE");
                    t.setAmount(body.has("amount") ? body.get("amount").getAsBigDecimal() : new BigDecimal("50.00"));
                    t.setMerchant(body.has("merchant") ? body.get("merchant").getAsString() : "Store");
                    t.setCategoryId(body.has("categoryId") ? body.get("categoryId").getAsInt() : 1);
                    Category c = MEM_CATEGORIES.stream().filter(x -> x.getId() == t.getCategoryId()).findFirst().orElse(null);
                    if (c != null) {
                        t.setCategoryName(c.getName());
                        t.setCategoryIcon(c.getIcon());
                        t.setCategoryColor(c.getColor());
                    }
                    t.setPaymentMethod(body.has("paymentMethod") ? body.get("paymentMethod").getAsString() : "UPI");
                    t.setTransactionDate(Date.valueOf(LocalDate.now()));
                    t.setTransactionTime(Time.valueOf("12:00:00"));
                    userList.add(0, t);
                    sendJson(exchange, 200, Map.of("success", true, "message", "Transaction saved.", "data", t));
                }
            }
        }

        private void handleCategories(HttpExchange exchange, String path, String method, User user) throws Exception {
            if ("GET".equals(method)) {
                sendJson(exchange, 200, Map.of("success", true, "data", MEM_CATEGORIES));
            } else {
                JsonObject body = parseJsonBody(exchange);
                Category c = new Category(MEM_CATEGORIES.size() + 1, user.getId(), body.get("name").getAsString(), body.get("type").getAsString(), body.get("icon").getAsString(), body.get("color").getAsString(), true);
                MEM_CATEGORIES.add(c);
                sendJson(exchange, 200, Map.of("success", true, "message", "Category added.", "data", c));
            }
        }

        private void handleBudgets(HttpExchange exchange, String path, String method, User user, Map<String, String> qp) throws Exception {
            if ("GET".equals(method)) {
                sendJson(exchange, 200, Map.of("success", true, "data", MEM_BUDGETS));
            } else {
                JsonObject body = parseJsonBody(exchange);
                if (path.endsWith("/delete")) {
                    int id = body.get("id").getAsInt();
                    MEM_BUDGETS.removeIf(b -> b.getId() == id);
                    sendJson(exchange, 200, Map.of("success", true, "message", "Budget deleted."));
                } else {
                    Budget b = new Budget();
                    b.setId(MEM_BUDGETS.size() + 1);
                    b.setName(body.get("name").getAsString());
                    b.setAmount(body.get("amount").getAsBigDecimal());
                    b.setSpentAmount(BigDecimal.ZERO);
                    b.computeMetrics();
                    MEM_BUDGETS.add(b);
                    sendJson(exchange, 200, Map.of("success", true, "message", "Budget saved.", "data", b));
                }
            }
        }

        private void handleGoals(HttpExchange exchange, String path, String method, User user) throws Exception {
            if ("GET".equals(method)) {
                sendJson(exchange, 200, Map.of("success", true, "data", MEM_GOALS));
            } else {
                JsonObject body = parseJsonBody(exchange);
                if (path.endsWith("/add-funds")) {
                    int id = body.get("id").getAsInt();
                    BigDecimal amt = body.get("amount").getAsBigDecimal();
                    Goal g = MEM_GOALS.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
                    if (g != null) {
                        g.setSavedAmount(g.getSavedAmount().add(amt));
                        g.computeMetrics();
                    }
                    sendJson(exchange, 200, Map.of("success", true, "message", "Funds added to goal!"));
                } else if (path.endsWith("/delete")) {
                    int id = body.get("id").getAsInt();
                    MEM_GOALS.removeIf(g -> g.getId() == id);
                    sendJson(exchange, 200, Map.of("success", true, "message", "Goal deleted."));
                } else {
                    Goal g = new Goal();
                    g.setId(MEM_GOALS.size() + 1);
                    g.setTitle(body.get("title").getAsString());
                    g.setTargetAmount(body.get("targetAmount").getAsBigDecimal());
                    g.setSavedAmount(BigDecimal.ZERO);
                    g.setTargetDate(Date.valueOf(LocalDate.now().plusMonths(6)));
                    g.computeMetrics();
                    MEM_GOALS.add(g);
                    sendJson(exchange, 200, Map.of("success", true, "message", "Goal created.", "data", g));
                }
            }
        }

        private void handleRecurring(HttpExchange exchange, String path, String method, User user, Map<String, String> qp) throws Exception {
            if ("GET".equals(method)) {
                sendJson(exchange, 200, Map.of("success", true, "data", MEM_RECURRING));
            } else {
                JsonObject body = parseJsonBody(exchange);
                if (path.endsWith("/delete")) {
                    int id = body.get("id").getAsInt();
                    MEM_RECURRING.removeIf(r -> r.getId() == id);
                    sendJson(exchange, 200, Map.of("success", true, "message", "Recurring subscription removed."));
                } else {
                    RecurringPayment r = new RecurringPayment();
                    r.setId(MEM_RECURRING.size() + 1);
                    r.setTitle(body.get("title").getAsString());
                    r.setMerchant(body.get("merchant").getAsString());
                    r.setAmount(body.get("amount").getAsBigDecimal());
                    r.setBillingCycle("MONTHLY");
                    r.setNextDueDate(Date.valueOf(LocalDate.now().plusDays(30)));
                    r.computeMetrics();
                    MEM_RECURRING.add(r);
                    sendJson(exchange, 200, Map.of("success", true, "message", "Subscription added.", "data", r));
                }
            }
        }

        private void handleAccounts(HttpExchange exchange, String path, String method, User user) throws Exception {
            List<PaymentAccount> accList = USER_ACCOUNTS.computeIfAbsent(user.getId(), k -> new ArrayList<>());
            List<Transaction> userTxList = USER_TRANSACTIONS.computeIfAbsent(user.getId(), k -> new ArrayList<>());

            if ("GET".equals(method)) {
                sendJson(exchange, 200, Map.of("success", true, "data", accList));
            } else if (path.endsWith("/sync-demo")) {
                LocalDate today = LocalDate.now();
                String upi = accList.isEmpty() ? "user@upi" : accList.get(0).getIdentifierMask();

                Transaction t1 = new Transaction();
                t1.setId((int) (System.currentTimeMillis() % 1000000000));
                t1.setUserId(user.getId());
                t1.setType("EXPENSE");
                t1.setAmount(new BigDecimal("32.50"));
                t1.setCategoryId(1);
                t1.setCategoryName("Food & Dining");
                t1.setCategoryIcon("utensils");
                t1.setCategoryColor("#f97316");
                t1.setMerchant("Swiggy Gourmet UPI");
                t1.setPaymentMethod("UPI");
                t1.setUpiId(upi);
                t1.setTransactionDate(Date.valueOf(today));
                t1.setTransactionTime(Time.valueOf("12:00:00"));
                t1.setNotes("Auto-synced from UPI (" + upi + ")");

                Transaction t2 = new Transaction();
                t2.setId(t1.getId() + 1);
                t2.setUserId(user.getId());
                t2.setType("EXPENSE");
                t2.setAmount(new BigDecimal("14.00"));
                t2.setCategoryId(3);
                t2.setCategoryName("Transportation");
                t2.setCategoryIcon("car");
                t2.setCategoryColor("#3b82f6");
                t2.setMerchant("Uber Premier Ride");
                t2.setPaymentMethod("UPI");
                t2.setUpiId(upi);
                t2.setTransactionDate(Date.valueOf(today));
                t2.setTransactionTime(Time.valueOf("12:00:00"));
                t2.setNotes("Auto-synced from UPI (" + upi + ")");

                Transaction t3 = new Transaction();
                t3.setId(t1.getId() + 2);
                t3.setUserId(user.getId());
                t3.setType("INCOME");
                t3.setAmount(new BigDecimal("250.00"));
                t3.setCategoryId(14);
                t3.setCategoryName("Freelance & Projects");
                t3.setCategoryIcon("laptop-code");
                t3.setCategoryColor("#22c55e");
                t3.setMerchant("UPI Client Payment Credit");
                t3.setPaymentMethod("UPI");
                t3.setUpiId(upi);
                t3.setTransactionDate(Date.valueOf(today));
                t3.setTransactionTime(Time.valueOf("12:00:00"));
                t3.setNotes("Direct UPI credit to " + upi);

                userTxList.add(0, t1);
                userTxList.add(0, t2);
                userTxList.add(0, t3);

                sendJson(exchange, 200, Map.of("success", true, "message", "Synced 3 transactions from " + upi + "!", "data", List.of(t1, t2, t3)));
            } else if (path.endsWith("/delete")) {
                JsonObject body = parseJsonBody(exchange);
                int id = body.get("id").getAsInt();
                accList.removeIf(a -> a.getId() == id);
                sendJson(exchange, 200, Map.of("success", true, "message", "Account removed."));
            } else {
                JsonObject body = parseJsonBody(exchange);
                PaymentAccount acc = new PaymentAccount();
                acc.setId((int) (System.currentTimeMillis() % 1000000000));
                acc.setUserId(user.getId());
                acc.setProviderName(body.get("providerName").getAsString());
                acc.setIdentifierMask(body.get("identifierMask").getAsString());
                acc.setBalance(body.has("balance") ? body.get("balance").getAsBigDecimal() : new BigDecimal("5000.00"));
                acc.setAccountType("UPI");
                acc.setDefault(true);
                accList.add(acc);
                sendJson(exchange, 200, Map.of("success", true, "message", "UPI account linked successfully.", "data", acc));
            }
        }

        private void handleAnalytics(HttpExchange exchange, String path, String method, User user, Map<String, String> qp) throws Exception {
            List<Transaction> userList = USER_TRANSACTIONS.computeIfAbsent(user.getId(), k -> new ArrayList<>());
            if (path.endsWith("/summary")) {
                BigDecimal income = userList.stream().filter(t -> "INCOME".equals(t.getType())).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal expense = userList.stream().filter(t -> "EXPENSE".equals(t.getType())).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal balance = income.subtract(expense);

                DashboardSummary ds = new DashboardSummary();
                ds.setTotalIncome(income);
                ds.setTotalExpenses(expense);
                ds.setTotalBalance(balance);
                ds.setNetSavings(balance);
                ds.setMonthlyBudget(BigDecimal.ZERO);
                ds.setRemainingBudget(BigDecimal.ZERO);
                ds.setRecentTransactions(userList.subList(0, Math.min(6, userList.size())));

                SpendingInsight si = new SpendingInsight();
                si.setSpendingTrend("STABLE");
                si.setTrendDescription("Spending ledger updated for " + user.getFullName());
                si.setHighestCategoryName(userList.isEmpty() ? "None" : "General");
                si.setHighestCategoryAmount(expense);
                si.setHighestCategoryPercent(100.0);
                si.setAverageDailySpending(expense);
                si.setSmartRecommendations(List.of(
                        "Keep tracking your transactions to optimize your savings."
                ));
                ds.setInsights(si);

                sendJson(exchange, 200, Map.of("success", true, "data", ds));
            } else if (path.endsWith("/charts/category")) {
                List<Map<String, Object>> catList = new ArrayList<>();
                catList.add(Map.of("categoryName", "Food & Dining", "totalAmount", 225.0, "color", "#f97316", "icon", "utensils"));
                catList.add(Map.of("categoryName", "Shopping", "totalAmount", 129.99, "color", "#ec4899", "icon", "shopping-bag"));
                catList.add(Map.of("categoryName", "Rent & Housing", "totalAmount", 450.0, "color", "#6366f1", "icon", "house"));
                catList.add(Map.of("categoryName", "Bills & Utilities", "totalAmount", 95.0, "color", "#eab308", "icon", "bolt"));
                catList.add(Map.of("categoryName", "Entertainment", "totalAmount", 45.0, "color", "#8b5cf6", "icon", "film"));
                catList.add(Map.of("categoryName", "Groceries", "totalAmount", 120.0, "color", "#84cc16", "icon", "basket-shopping"));
                sendJson(exchange, 200, Map.of("success", true, "data", catList));
            } else if (path.endsWith("/charts/cashflow")) {
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                List<Map<String, Object>> list = new ArrayList<>();
                for (String m : months) {
                    list.add(Map.of("monthName", m, "income", 5500.0, "expense", 1097.48));
                }
                sendJson(exchange, 200, Map.of("success", true, "data", list));
            } else if (path.endsWith("/charts/trend")) {
                List<Map<String, Object>> list = new ArrayList<>();
                LocalDate today = LocalDate.now();
                for (int i = 20; i >= 0; i--) {
                    list.add(Map.of("date", today.minusDays(i).toString(), "amount", 20 + (i * 7 % 45)));
                }
                sendJson(exchange, 200, Map.of("success", true, "data", list));
            }
        }

        private void handleNotifications(HttpExchange exchange, String path, String method, User user) throws Exception {
            sendJson(exchange, 200, Map.of("success", true, "data", Map.of("notifications", MEM_NOTIFS, "unreadCount", MEM_NOTIFS.size())));
        }

        private void handleSmsSync(HttpExchange exchange, String path, String method, User sessionUser, Map<String, String> queryParams) throws Exception {
            if ("POST".equals(method)) {
                JsonObject body = parseJsonBody(exchange);
                String message = "";
                if (body.has("message")) message = body.get("message").getAsString();
                else if (body.has("text")) message = body.get("text").getAsString();
                else if (body.has("body")) message = body.get("body").getAsString();
                else if (body.has("sms")) message = body.get("sms").getAsString();
                else if (body.has("content")) message = body.get("content").getAsString();
                else if (body.has("msg")) message = body.get("msg").getAsString();
                else if (body.has("payload")) message = body.get("payload").getAsString();
                else message = body.toString();

                System.out.println("📩 [WEBHOOK RECEIVED] SMS payload: " + message);

                String userEmail = queryParams.getOrDefault("user", "");
                if (userEmail.isEmpty() && body.has("userEmail")) {
                    userEmail = body.get("userEmail").getAsString();
                }

                User targetUser = sessionUser;
                if (!userEmail.isEmpty()) {
                    String clean = userEmail.toLowerCase().trim();
                    User found = USERS_BY_EMAIL.get(clean);
                    if (found == null) {
                        int id = Math.abs(clean.hashCode());
                        if (id <= 1) id = 101;
                        found = new User(id, clean.split("@")[0], clean, "USD", "light");
                        USERS_BY_EMAIL.put(clean, found);
                    }
                    targetUser = found;
                }

                Transaction parsedTx = parseBankSms(message, targetUser.getId());
                if (parsedTx != null) {
                    List<Transaction> userList = USER_TRANSACTIONS.computeIfAbsent(targetUser.getId(), k -> new ArrayList<>());
                    userList.add(0, parsedTx);
                    sendJson(exchange, 200, Map.of(
                            "success", true,
                            "message", "Auto-logged UPI transaction: " + parsedTx.getMerchant() + " ($" + parsedTx.getAmount() + ")",
                            "data", parsedTx
                    ));
                } else {
                    sendJson(exchange, 400, Map.of(
                            "success", false,
                            "message", "Could not extract amount or merchant from SMS. Please ensure it contains a valid bank transaction message."
                    ));
                }
            } else {
                sendJson(exchange, 200, Map.of("success", true, "message", "SMS Sync Webhook Active. Send POST with { 'message': '...' }"));
            }
        }

        private Transaction parseBankSms(String text, int userId) {
            if (text == null || text.trim().isEmpty()) return null;
            String lower = text.toLowerCase();

            // 1. Determine Type
            String type = "EXPENSE";
            if (lower.contains("credited") || lower.contains("credit of") || lower.contains("deposited") || lower.contains("received in")) {
                type = "INCOME";
            }

            // 2. Extract Amount
            BigDecimal amount = null;
            Pattern amtPattern = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)");
            Matcher amtMatcher = amtPattern.matcher(text);
            if (amtMatcher.find()) {
                String raw = amtMatcher.group(1).replace(",", "");
                try { amount = new BigDecimal(raw); } catch (Exception ignored) {}
            } else {
                Pattern fallbackAmt = Pattern.compile("(?i)(?:debited|credited|with|by)\\s*(?:by|with)?\\s*([\\d,]+(?:\\.\\d{1,2})?)");
                Matcher fbMatcher = fallbackAmt.matcher(text);
                if (fbMatcher.find()) {
                    String raw = fbMatcher.group(1).replace(",", "");
                    try { amount = new BigDecimal(raw); } catch (Exception ignored) {}
                }
            }
            if (amount == null) return null;

            // 3. Extract Merchant
            String merchant = "UPI Payment";
            Pattern toPattern = Pattern.compile("(?i)(?:to|at|vpa|by\\s*upi:?|paid to)\\s+([A-Za-z0-9&._ -]{3,35})(?:on|via|ref|rrn|upi|val|bal|\\.|$)");
            Matcher toMatcher = toPattern.matcher(text);
            if (toMatcher.find()) {
                merchant = toMatcher.group(1).trim().replaceAll("(?i)\\s+(on|via|ref|rrn|upi).*$", "");
            } else {
                String[] known = {"Swiggy", "Zomato", "Uber", "Ola", "Amazon", "Flipkart", "Blinkit", "Zepto", "Instamart", "Starbucks", "Chai Point", "Airtel", "Jio", "Apollo Pharmacy", "D-Mart", "BookMyShow", "Netflix"};
                for (String k : known) {
                    if (lower.contains(k.toLowerCase())) {
                        merchant = k;
                        break;
                    }
                }
            }
            if (merchant.length() > 30) merchant = merchant.substring(0, 30);

            // 4. Categorize
            int catId = 12;
            String catName = "Other Expenses";
            String catIcon = "circle-dollar-to-slot";
            String catColor = "#64748b";

            if ("INCOME".equals(type)) {
                catId = 13;
                catName = "Salary & Wages";
                catIcon = "money-bill-wave";
                catColor = "#10b981";
            } else if (lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("starbucks") || lower.contains("chai") || lower.contains("restaurant") || lower.contains("cafe") || lower.contains("kfc") || lower.contains("mcdonald")) {
                catId = 1; catName = "Food & Dining"; catIcon = "utensils"; catColor = "#f97316";
            } else if (lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("metro") || lower.contains("fuel") || lower.contains("petrol") || lower.contains("hpcl") || lower.contains("bpcl") || lower.contains("ioc")) {
                catId = 3; catName = "Transportation"; catIcon = "car"; catColor = "#3b82f6";
            } else if (lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("shopping") || lower.contains("retail") || lower.contains("mart") || lower.contains("blinkit") || lower.contains("zepto") || lower.contains("dmart")) {
                catId = 2; catName = "Shopping & Retail"; catIcon = "shopping-bag"; catColor = "#ec4899";
            } else if (lower.contains("airtel") || lower.contains("jio") || lower.contains("electric") || lower.contains("bill") || lower.contains("broadband") || lower.contains("water") || lower.contains("bescom")) {
                catId = 4; catName = "Bills & Utilities"; catIcon = "bolt"; catColor = "#eab308";
            } else if (lower.contains("pharmacy") || lower.contains("health") || lower.contains("apollo") || lower.contains("1mg") || lower.contains("hospital")) {
                catId = 6; catName = "Health & Medical"; catIcon = "heart-pulse"; catColor = "#ef4444";
            } else if (lower.contains("netflix") || lower.contains("spotify") || lower.contains("hotstar") || lower.contains("prime") || lower.contains("movie") || lower.contains("cinema")) {
                catId = 5; catName = "Entertainment"; catIcon = "film"; catColor = "#8b5cf6";
            }

            Transaction t = new Transaction();
            t.setId((int) (System.currentTimeMillis() % 1000000000));
            t.setUserId(userId);
            t.setType(type);
            t.setAmount(amount);
            t.setCategoryId(catId);
            t.setCategoryName(catName);
            t.setCategoryIcon(catIcon);
            t.setCategoryColor(catColor);
            t.setMerchant(merchant);
            t.setPaymentMethod("UPI");
            t.setTransactionDate(Date.valueOf(LocalDate.now()));
            t.setTransactionTime(Time.valueOf("12:00:00"));
            t.setNotes("Auto-synced from Bank SMS");
            return t;
        }

        private JsonObject parseJsonBody(HttpExchange exchange) throws IOException {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                if (sb.length() == 0) return new JsonObject();
                return JsonParser.parseString(sb.toString()).getAsJsonObject();
            }
        }

        private void sendJson(HttpExchange exchange, int statusCode, Object obj) throws IOException {
            byte[] bytes = JsonUtil.toJson(obj).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }

        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> map = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        try {
                            map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                        } catch (Exception ignored) {}
                    }
                }
            }
            return map;
        }
    }

    static class StaticFileHandler implements HttpHandler {
        private final File webRoot = new File("WebContent");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File file = new File(webRoot, path);
            if (!file.exists() || file.isDirectory()) {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); }
                return;
            }

            String contentType = getContentType(file.getName());
            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String getContentType(String name) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }
}

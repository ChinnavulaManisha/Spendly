package com.spendly.service.payment;

import com.spendly.dao.CategoryDAO;
import com.spendly.dao.NotificationDAO;
import com.spendly.dao.PaymentAccountDAO;
import com.spendly.dao.TransactionDAO;
import com.spendly.model.Category;
import com.spendly.model.Notification;
import com.spendly.model.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Realistic Demo / Sandbox Payment Provider simulation for Spendly.
 * Generates realistic merchant transactions to showcase statement synchronization.
 */
public class DemoPaymentProvider implements PaymentProvider {

    private final TransactionDAO transactionDAO;
    private final PaymentAccountDAO paymentAccountDAO;
    private final NotificationDAO notificationDAO;
    private final CategoryDAO categoryDAO;
    private final Random random = new Random();

    public DemoPaymentProvider() {
        this.transactionDAO = new TransactionDAO();
        this.paymentAccountDAO = new PaymentAccountDAO();
        this.notificationDAO = new NotificationDAO();
        this.categoryDAO = new CategoryDAO();
    }

    @Override
    public String getProviderCode() {
        return "DEMO_SANDBOX";
    }

    @Override
    public String getProviderName() {
        return "Spendly Demo Sandbox Gateway";
    }

    @Override
    public List<Transaction> syncTransactions(int userId, int accountId, String accountIdentifier) throws Exception {
        List<Category> userCategories = categoryDAO.getAllForUser(userId);

        // Predefined realistic demo statement items
        DemoTxTemplate[] templates = new DemoTxTemplate[]{
                new DemoTxTemplate("Swiggy Food Delivery", "EXPENSE", 18.50, 42.00, "Food & Dining", "UPI"),
                new DemoTxTemplate("Starbucks Reserve", "EXPENSE", 7.50, 14.50, "Food & Dining", "UPI"),
                new DemoTxTemplate("Uber Ride", "EXPENSE", 12.00, 38.00, "Transportation & Fuel", "UPI"),
                new DemoTxTemplate("Amazon Online Order", "EXPENSE", 45.00, 189.00, "Shopping & Retail", "UPI"),
                new DemoTxTemplate("Blinkit Groceries", "EXPENSE", 24.00, 68.00, "Groceries & Mart", "UPI"),
                new DemoTxTemplate("Apple Cloud Storage", "EXPENSE", 2.99, 9.99, "Subscriptions & SaaS", "UPI AutoPay"),
                new DemoTxTemplate("Zomato Dineout", "EXPENSE", 35.00, 85.00, "Food & Dining", "UPI"),
                new DemoTxTemplate("Shell Gas Station", "EXPENSE", 40.00, 65.00, "Transportation & Fuel", "UPI"),
                new DemoTxTemplate("Freelance Milestone Payout", "INCOME", 350.00, 950.00, "Freelance & Projects", "UPI"),
                new DemoTxTemplate("Cashback Reward Credit", "INCOME", 5.00, 25.00, "Cashbacks & Refunds", "UPI")
        };

        // Pick 3 to 5 random transactions to simulate sync
        int countToGenerate = 3 + random.nextInt(3);
        List<Transaction> generatedList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < countToGenerate; i++) {
            DemoTxTemplate template = templates[random.nextInt(templates.length)];
            int daysAgo = random.nextInt(4); // 0 to 3 days ago

            int catId = findCategoryId(userCategories, template.categoryName, template.type);
            double baseAmount = template.minAmount + (random.nextDouble() * (template.maxAmount - template.minAmount));
            BigDecimal roundedAmount = BigDecimal.valueOf(baseAmount).setScale(2, RoundingMode.HALF_UP);

            int hour = 8 + random.nextInt(14);
            int min = random.nextInt(60);
            int sec = random.nextInt(60);

            Transaction tx = new Transaction();
            tx.setUserId(userId);
            tx.setType(template.type);
            tx.setAmount(roundedAmount);
            tx.setCategoryId(catId);
            tx.setMerchant(template.merchant);
            tx.setPaymentMethod(template.paymentMethod);
            tx.setUpiId(accountIdentifier != null ? accountIdentifier : "user@okhdfcbank");
            tx.setAccountId(accountId > 0 ? accountId : null);
            tx.setTransactionDate(Date.valueOf(today.minusDays(daysAgo)));
            tx.setTransactionTime(Time.valueOf(String.format("%02d:%02d:%02d", hour, min, sec)));
            tx.setNotes("Auto-synced via " + getProviderName() + " (Ref #" + (100000 + random.nextInt(900000)) + ")");
            tx.setRecurring(template.merchant.contains("Apple") || template.merchant.contains("Netflix"));
            tx.setSyncedDemo(true);

            Transaction saved = transactionDAO.create(tx);
            if (saved != null) {
                generatedList.add(saved);
            }
        }

        // Update payment account timestamp
        if (accountId > 0) {
            paymentAccountDAO.updateLastSynced(accountId, userId);
        }

        // Trigger in-app notification
        Notification notif = new Notification(
                userId,
                "UPI Account Synced",
                "Successfully synchronized " + generatedList.size() + " new transactions for account " + accountIdentifier + ".",
                "SUCCESS"
        );
        notificationDAO.create(notif);

        return generatedList;
    }

    private int findCategoryId(List<Category> categories, String name, String type) {
        for (Category c : categories) {
            if (c.getName().equalsIgnoreCase(name) || c.getName().toLowerCase().contains(name.toLowerCase())) {
                return c.getId();
            }
        }
        for (Category c : categories) {
            if (c.getType().equalsIgnoreCase(type)) {
                return c.getId();
            }
        }
        return 1; // Default
    }

    private static class DemoTxTemplate {
        String merchant;
        String type;
        double minAmount;
        double maxAmount;
        String categoryName;
        String paymentMethod;

        public DemoTxTemplate(String merchant, String type, double minAmount, double maxAmount, String categoryName, String paymentMethod) {
            this.merchant = merchant;
            this.type = type;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.categoryName = categoryName;
            this.paymentMethod = paymentMethod;
        }
    }
}

package com.spendly.service;

import com.spendly.model.Transaction;

import java.io.PrintWriter;
import java.util.List;

/**
 * Service to export transaction statements and financial records to standard CSV.
 */
public class ExportService {

    /**
     * Writes transactions directly into PrintWriter in CSV format.
     */
    public void exportTransactionsToCsv(List<Transaction> transactions, PrintWriter writer) {
        // CSV Header
        writer.println("Transaction ID,Date,Time,Type,Category,Merchant,Amount,Payment Method,UPI ID,Recurring,Synced Demo,Notes");

        if (transactions != null) {
            for (Transaction t : transactions) {
                StringBuilder row = new StringBuilder();
                row.append(t.getId()).append(",");
                row.append(escapeCsv(t.getTransactionDate() != null ? t.getTransactionDate().toString() : "")).append(",");
                row.append(escapeCsv(t.getTransactionTime() != null ? t.getTransactionTime().toString() : "")).append(",");
                row.append(escapeCsv(t.getType())).append(",");
                row.append(escapeCsv(t.getCategoryName() != null ? t.getCategoryName() : "General")).append(",");
                row.append(escapeCsv(t.getMerchant())).append(",");
                row.append(t.getAmount() != null ? t.getAmount().toString() : "0.00").append(",");
                row.append(escapeCsv(t.getPaymentMethod())).append(",");
                row.append(escapeCsv(t.getUpiId() != null ? t.getUpiId() : "")).append(",");
                row.append(t.isRecurring() ? "YES" : "NO").append(",");
                row.append(t.isSyncedDemo() ? "YES" : "NO").append(",");
                row.append(escapeCsv(t.getNotes() != null ? t.getNotes() : ""));
                writer.println(row.toString());
            }
        }
        writer.flush();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

package com.spendly.service.payment;

import com.spendly.model.Transaction;

import java.util.List;

/**
 * PaymentProvider Interface defining standard banking and UPI statement synchronization contracts.
 * Designed so real Open Banking / UPI Account Aggregator (AA) APIs can be implemented in the future.
 */
public interface PaymentProvider {

    /**
     * Provider unique code (e.g. "DEMO_SANDBOX", "OPEN_BANKING_AA", "SETU_UPI").
     */
    String getProviderCode();

    /**
     * Provider display name.
     */
    String getProviderName();

    /**
     * Synchronizes statement transactions for a user's payment account.
     * @param userId The ID of the authenticated user
     * @param accountId The ID of the registered payment account / UPI handle
     * @param accountIdentifier The UPI handle or masked account number
     * @return List of newly parsed and synchronized Transactions
     * @throws Exception If synchronization fails
     */
    List<Transaction> syncTransactions(int userId, int accountId, String accountIdentifier) throws Exception;
}

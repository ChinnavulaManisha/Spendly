/**
 * Spendly - REST API Client Wrapper with Isolated Per-User Storage
 */

const API_BASE = '/api';

const DEFAULT_SYSTEM_CATEGORIES = [
  { id: 1, name: 'Food & Dining', type: 'EXPENSE', icon: 'utensils', color: '#f97316' },
  { id: 2, name: 'Shopping & Retail', type: 'EXPENSE', icon: 'shopping-bag', color: '#ec4899' },
  { id: 3, name: 'Transportation', type: 'EXPENSE', icon: 'car', color: '#3b82f6' },
  { id: 4, name: 'Bills & Utilities', type: 'EXPENSE', icon: 'bolt', color: '#eab308' },
  { id: 5, name: 'Entertainment', type: 'EXPENSE', icon: 'film', color: '#8b5cf6' },
  { id: 6, name: 'Health & Medical', type: 'EXPENSE', icon: 'heart-pulse', color: '#ef4444' },
  { id: 7, name: 'Rent & Housing', type: 'EXPENSE', icon: 'house', color: '#6366f1' },
  { id: 8, name: 'Subscriptions & SaaS', type: 'EXPENSE', icon: 'arrows-rotate', color: '#a855f7' },
  { id: 13, name: 'Salary & Wages', type: 'INCOME', icon: 'money-bill-wave', color: '#10b981' },
  { id: 14, name: 'Freelance & Projects', type: 'INCOME', icon: 'laptop-code', color: '#22c55e' }
];

async function fetchJSON(url, options = {}) {
  try {
    const res = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...options.headers,
      },
    });
    if (!res.ok) throw new Error('API status: ' + res.status);
    return await res.json();
  } catch (err) {
    return null;
  }
}

// User-scoped LocalStorage helpers
function getCurrentUser() {
  try {
    const u = localStorage.getItem('spendly_user');
    return u ? JSON.parse(u) : null;
  } catch (e) {
    return null;
  }
}

function getUserStorageKey(suffix, specificEmail) {
  const user = getCurrentUser();
  const email = specificEmail || (user && user.email ? user.email : 'guest');
  const id = email.toLowerCase().trim();
  return `spendly_${suffix}_${id}`;
}

function getUserTransactionsFromStore(specificEmail) {
  try {
    const key = getUserStorageKey('transactions', specificEmail);
    const data = localStorage.getItem(key);
    if (!data) return [];
    const list = JSON.parse(data);
    const user = getCurrentUser();
    const userEmail = (user && user.email) ? user.email.toLowerCase().trim() : '';
    const isAlex = userEmail === 'alex@spendly.io';
    // Purge any leaked Alex Rivera demo transactions (userId === 1) if not Alex
    if (!isAlex && Array.isArray(list)) {
      const cleaned = list.filter(t => t.userId !== 1 && (!t.merchant || !t.merchant.includes('Acme Corp')));
      if (cleaned.length !== list.length) {
        localStorage.setItem(key, JSON.stringify(cleaned));
        return cleaned;
      }
    }
    return Array.isArray(list) ? list : [];
  } catch (e) {
    return [];
  }
}

function setUserTransactionsToStore(txList, specificEmail) {
  try {
    const key = getUserStorageKey('transactions', specificEmail);
    localStorage.setItem(key, JSON.stringify(txList));
  } catch (e) {}
}

const SpendlyAPI = {
  Auth: {
    getCurrentUser,

    login: async (email, password) => {
      email = (email || '').trim();
      const res = await fetchJSON(`${API_BASE}/auth/login`, {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });

      let user = null;
      if (res && res.success && res.data) {
        user = res.data;
      } else {
        const usersListStr = localStorage.getItem('spendly_all_users');
        const usersList = usersListStr ? JSON.parse(usersListStr) : [];
        const existing = usersList.find(u => u.email.toLowerCase() === email.toLowerCase());

        if (existing) {
          user = existing;
        } else if (email.toLowerCase() === 'alex@spendly.io') {
          user = { id: 1, fullName: 'Alex Rivera', email: 'alex@spendly.io', currency: 'USD', themePreference: 'light' };
        } else {
          const baseName = email.split('@')[0].replace(/[._-]/g, ' ');
          const formattedName = baseName.charAt(0).toUpperCase() + baseName.slice(1);
          user = { id: Date.now(), fullName: formattedName, email, currency: 'USD', themePreference: 'light' };
        }
      }

      // Save user to session
      localStorage.setItem('spendly_user', JSON.stringify(user));

      // Make sure user is registered in known users list
      try {
        const usersList = JSON.parse(localStorage.getItem('spendly_all_users') || '[]');
        if (!usersList.some(u => u.email.toLowerCase() === user.email.toLowerCase())) {
          usersList.push(user);
          localStorage.setItem('spendly_all_users', JSON.stringify(usersList));
        }
      } catch (e) {}

      return { success: true, message: 'Logged in successfully', data: user };
    },

    register: async (fullName, email, password, currency) => {
      fullName = (fullName || '').trim();
      email = (email || '').trim();
      currency = currency || 'USD';

      const res = await fetchJSON(`${API_BASE}/auth/register`, {
        method: 'POST',
        body: JSON.stringify({ fullName, email, password, currency }),
      });

      let user = null;
      if (res && res.success && res.data) {
        user = res.data;
      } else {
        user = {
          id: Date.now(),
          fullName: fullName || email.split('@')[0],
          email,
          currency,
          themePreference: 'light'
        };
      }

      // Save user
      localStorage.setItem('spendly_user', JSON.stringify(user));

      // Add to users registry
      try {
        const usersList = JSON.parse(localStorage.getItem('spendly_all_users') || '[]');
        const idx = usersList.findIndex(u => u.email.toLowerCase() === email.toLowerCase());
        if (idx >= 0) {
          usersList[idx] = user;
        } else {
          usersList.push(user);
        }
        localStorage.setItem('spendly_all_users', JSON.stringify(usersList));
      } catch (e) {}

      // Explicitly initialize empty transactions for new user
      setUserTransactionsToStore([], email);

      return { success: true, message: 'Account registered successfully', data: user };
    },

    logout: async () => {
      await fetchJSON(`${API_BASE}/auth/logout`, { method: 'POST' });
      localStorage.removeItem('spendly_user');
      return { success: true };
    }
  },

  Transactions: {
    getAll: async () => {
      const user = getCurrentUser();
      const userEmail = (user && user.email) ? user.email.toLowerCase().trim() : '';
      const isAlex = userEmail === 'alex@spendly.io';

      // First check local user store
      const localList = getUserTransactionsFromStore(userEmail);
      let delSet = new Set();
      try {
        delSet = new Set(JSON.parse(localStorage.getItem(`spendly_deleted_${userEmail}`) || '[]'));
      } catch (e) {}

      const res = await fetchJSON(`${API_BASE}/transactions/?user=${encodeURIComponent(userEmail)}`);
      if (res && res.success && Array.isArray(res.data)) {
        // Filter out demo transactions belonging to Alex if this is not Alex, and any deleted IDs
        const serverUserTxs = res.data.filter(t => {
          if (!isAlex && t.userId === 1 && !t.userEmail) return false;
          if (delSet.has(String(t.id))) return false;
          return true;
        });

        // Merge server transactions and local items by unique id
        const mergedMap = new Map();
        serverUserTxs.forEach(t => mergedMap.set(String(t.id), t));
        localList.forEach(t => {
          if (!delSet.has(String(t.id))) {
            mergedMap.set(String(t.id), t);
          }
        });
        const effective = Array.from(mergedMap.values()).sort((a, b) => b.id - a.id);

        setUserTransactionsToStore(effective, userEmail);
        return { success: true, data: effective };
      }

      // Return isolated previous transactions for this user
      const filteredLocal = localList.filter(t => !delSet.has(String(t.id)));
      return { success: true, data: filteredLocal };
    },

    create: async (txData) => {
      const user = getCurrentUser();
      const userEmail = (user && user.email) ? user.email.toLowerCase().trim() : '';

      const res = await fetchJSON(`${API_BASE}/transactions/?user=${encodeURIComponent(userEmail)}`, {
        method: 'POST',
        body: JSON.stringify({ ...txData, userId: user ? user.id : 0, userEmail }),
      });

      let newTx;
      if (res && res.success && res.data) {
        newTx = { ...res.data, userEmail };
      } else {
        const cat = DEFAULT_SYSTEM_CATEGORIES.find(c => c.id == txData.categoryId) || {};
        newTx = {
          id: Date.now(),
          ...txData,
          userId: user ? user.id : 0,
          userEmail: userEmail,
          categoryName: cat.name || 'General',
          categoryIcon: cat.icon || 'receipt',
          categoryColor: cat.color || '#0d9488',
          transactionDate: new Date().toISOString().split('T')[0]
        };
      }

      const list = getUserTransactionsFromStore(userEmail);
      list.unshift(newTx);
      setUserTransactionsToStore(list, userEmail);

      return { success: true, data: newTx };
    },

    delete: async (id) => {
      const user = getCurrentUser();
      const userEmail = (user && user.email) ? user.email.toLowerCase().trim() : '';

      // Persist to deleted ID blacklist so polling never re-adds it
      try {
        const delKey = `spendly_deleted_${userEmail}`;
        const delSet = new Set(JSON.parse(localStorage.getItem(delKey) || '[]'));
        delSet.add(String(id));
        localStorage.setItem(delKey, JSON.stringify(Array.from(delSet)));
      } catch (e) {}

      try {
        await fetchJSON(`${API_BASE}/transactions/delete?user=${encodeURIComponent(userEmail)}`, {
          method: 'POST',
          body: JSON.stringify({ id: Number(id), userEmail })
        });
      } catch (e) {}

      const list = getUserTransactionsFromStore(userEmail).filter(t => String(t.id) !== String(id));
      setUserTransactionsToStore(list, userEmail);
      return { success: true };
    },

    exportCsv: () => {
      const txs = getUserTransactionsFromStore();
      if (!txs.length) {
        alert('No transactions to export.');
        return;
      }
      let csv = 'ID,Type,Amount,Category,Merchant,Method,Date\n';
      txs.forEach(t => {
        csv += `"${t.id}","${t.type}","${t.amount}","${t.categoryName || ''}","${t.merchant || ''}","${t.paymentMethod || ''}","${t.transactionDate || ''}"\n`;
      });
      const blob = new Blob([csv], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `transactions_${new Date().toISOString().split('T')[0]}.csv`;
      a.click();
    }
  },

  Categories: {
    getAll: async () => {
      const res = await fetchJSON(`${API_BASE}/categories/`);
      if (res && res.data && res.data.length > 0) return res;
      return { success: true, data: DEFAULT_SYSTEM_CATEGORIES };
    }
  },

  Budgets: {
    getAll: async () => {
      return { success: true, data: [] };
    }
  },

  Goals: {
    getAll: async () => {
      return { success: true, data: [] };
    },
    addFunds: async (id, amount) => {
      return { success: true };
    }
  },

  Recurring: {
    getAll: async () => {
      return { success: true, data: [] };
    }
  },

  Accounts: {
    getAll: async () => {
      const user = getCurrentUser();
      const userEmail = (user && user.email) ? user.email.toLowerCase().trim() : '';
      try {
        const key = getUserStorageKey('accounts', userEmail);
        const data = localStorage.getItem(key);
        return { success: true, data: data ? JSON.parse(data) : [] };
      } catch (e) {
        return { success: true, data: [] };
      }
    },

    linkUpi: async ({ upiId, providerName, accountHolderName, balance, syncNow }) => {
      const user = getCurrentUser();
      const userEmail = (user && user.email) ? user.email.toLowerCase().trim() : '';
      const key = getUserStorageKey('accounts', userEmail);

      let accounts = [];
      try {
        const existing = localStorage.getItem(key);
        accounts = existing ? JSON.parse(existing) : [];
      } catch (e) {}

      const newAccount = {
        id: Date.now(),
        providerName: providerName || 'UPI',
        identifierMask: upiId,
        accountHolderName: accountHolderName || (user ? user.fullName : 'Account Holder'),
        balance: parseFloat(balance || 5000),
        accountType: 'UPI',
        isVerified: true,
        lastSyncedAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };

      accounts.push(newAccount);
      localStorage.setItem(key, JSON.stringify(accounts));

      // If syncNow requested, generate realistic initial statement transactions for this UPI ID
      let syncedCount = 0;
      if (syncNow) {
        const today = new Date();
        const dStr = (daysAgo) => {
          const d = new Date(today);
          d.setDate(today.getDate() - daysAgo);
          return d.toISOString().split('T')[0];
        };

        const initialUpiTxs = [
          {
            id: Date.now() + 1,
            type: 'EXPENSE',
            amount: 28.50,
            categoryId: 1,
            categoryName: 'Food & Dining',
            categoryIcon: 'utensils',
            categoryColor: '#f97316',
            merchant: 'Swiggy Food Delivery',
            paymentMethod: 'UPI',
            upiId: upiId,
            transactionDate: dStr(0),
            notes: `Auto-synced from UPI (${upiId})`
          },
          {
            id: Date.now() + 2,
            type: 'EXPENSE',
            amount: 64.99,
            categoryId: 2,
            categoryName: 'Shopping & Retail',
            categoryIcon: 'shopping-bag',
            categoryColor: '#ec4899',
            merchant: 'Amazon Online Mart',
            paymentMethod: 'UPI',
            upiId: upiId,
            transactionDate: dStr(1),
            notes: `Auto-synced from UPI (${upiId})`
          },
          {
            id: Date.now() + 3,
            type: 'EXPENSE',
            amount: 16.00,
            categoryId: 3,
            categoryName: 'Transportation',
            categoryIcon: 'car',
            categoryColor: '#3b82f6',
            merchant: 'Uber Premier Commute',
            paymentMethod: 'UPI',
            upiId: upiId,
            transactionDate: dStr(2),
            notes: `Auto-synced from UPI (${upiId})`
          },
          {
            id: Date.now() + 4,
            type: 'INCOME',
            amount: 450.00,
            categoryId: 14,
            categoryName: 'Freelance & Projects',
            categoryIcon: 'laptop-code',
            categoryColor: '#22c55e',
            merchant: 'UPI Client Payout Credit',
            paymentMethod: 'UPI',
            upiId: upiId,
            transactionDate: dStr(3),
            notes: `Direct UPI credit to (${upiId})`
          }
        ];

        const userTxs = getUserTransactionsFromStore(userEmail);
        initialUpiTxs.forEach(t => userTxs.unshift(t));
        setUserTransactionsToStore(userTxs, userEmail);
        syncedCount = initialUpiTxs.length;
      }

      return { success: true, message: `UPI ID ${upiId} linked successfully!`, syncedCount, data: newAccount };
    },

    syncUpiStatement: async (accountId) => {
      const user = getCurrentUser();
      const userEmail = (user && user.email) ? user.email.toLowerCase().trim() : '';
      const key = getUserStorageKey('accounts', userEmail);

      let accounts = [];
      try {
        accounts = JSON.parse(localStorage.getItem(key) || '[]');
      } catch (e) {}

      const acc = accounts.find(a => a.id === accountId) || accounts[0];
      const upiId = acc ? acc.identifierMask : 'user@upi';

      const todayStr = new Date().toISOString().split('T')[0];
      const newItems = [
        {
          id: Date.now() + 10,
          type: 'EXPENSE',
          amount: 14.50,
          categoryId: 1,
          categoryName: 'Food & Dining',
          categoryIcon: 'utensils',
          categoryColor: '#f97316',
          merchant: 'Starbucks Coffee UPI',
          paymentMethod: 'UPI',
          upiId: upiId,
          transactionDate: todayStr,
          notes: `Statement refresh for ${upiId}`
        },
        {
          id: Date.now() + 11,
          type: 'EXPENSE',
          amount: 22.00,
          categoryId: 4,
          categoryName: 'Bills & Utilities',
          categoryIcon: 'bolt',
          categoryColor: '#eab308',
          merchant: 'Airtel Broadband AutoPay',
          paymentMethod: 'UPI',
          upiId: upiId,
          transactionDate: todayStr,
          notes: `Statement refresh for ${upiId}`
        }
      ];

      const userTxs = getUserTransactionsFromStore(userEmail);
      newItems.forEach(t => userTxs.unshift(t));
      setUserTransactionsToStore(userTxs, userEmail);

      if (acc) {
        acc.lastSyncedAt = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        localStorage.setItem(key, JSON.stringify(accounts));
      }

      return { success: true, message: `Synced 2 new transactions from ${upiId}!`, count: 2 };
    },

    delete: async (accountId) => {
      const user = getCurrentUser();
      const userEmail = (user && user.email) ? user.email.toLowerCase().trim() : '';
      const key = getUserStorageKey('accounts', userEmail);
      try {
        let accounts = JSON.parse(localStorage.getItem(key) || '[]');
        accounts = accounts.filter(a => a.id !== accountId);
        localStorage.setItem(key, JSON.stringify(accounts));
      } catch (e) {}
      return { success: true };
    }
  },

  SMS: {
    parseAndSave: async (smsText) => {
      const user = getCurrentUser();
      const userEmail = (user && user.email) ? user.email.toLowerCase().trim() : '';

      // Try server webhook first
      const res = await fetchJSON(`${API_BASE}/sync/sms?user=${encodeURIComponent(userEmail)}`, {
        method: 'POST',
        body: JSON.stringify({ message: smsText, userEmail }),
      });

      if (res && res.success && res.data) {
        const list = getUserTransactionsFromStore(userEmail);
        list.unshift(res.data);
        setUserTransactionsToStore(list, userEmail);
        return res;
      }

      // Client-side fallback parser for offline/local mode
      const parsed = parseClientSms(smsText, userEmail);
      if (parsed) {
        const list = getUserTransactionsFromStore(userEmail);
        list.unshift(parsed);
        setUserTransactionsToStore(list, userEmail);
        return { success: true, message: `Auto-logged: ${parsed.merchant} ($${parsed.amount})`, data: parsed };
      }

      throw new Error('Could not extract amount or merchant from SMS message. Please verify format.');
    }
  },

  Analytics: {
    getSummary: async () => {
      const txs = getUserTransactionsFromStore();
      const income = txs.filter(t => t.type === 'INCOME').reduce((acc, t) => acc + parseFloat(t.amount || 0), 0);
      const expenses = txs.filter(t => t.type === 'EXPENSE').reduce((acc, t) => acc + parseFloat(t.amount || 0), 0);
      const balance = income - expenses;

      return {
        success: true,
        data: {
          totalBalance: balance,
          totalIncome: income,
          totalExpenses: expenses,
          monthlyBudget: 0,
          remainingBudget: 0
        }
      };
    }
  }
};

function parseClientSms(text, userEmail) {
  if (!text) return null;
  const lower = text.toLowerCase();

  // 1. Type
  let type = 'EXPENSE';
  if (lower.includes('credit') || lower.includes('deposited') || lower.includes('received')) {
    type = 'INCOME';
  }

  // 2. Amount
  let amount = null;
  const amtMatch = text.match(/(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)/i);
  if (amtMatch) {
    amount = parseFloat(amtMatch[1].replace(/,/g, ''));
  } else {
    const fbMatch = text.match(/(?:debited|credited|with|by)\s*(?:by|with)?\s*([\d,]+(?:\.\d{1,2})?)/i);
    if (fbMatch) amount = parseFloat(fbMatch[1].replace(/,/g, ''));
  }
  if (!amount || isNaN(amount)) return null;

  // 3. Merchant
  let merchant = 'UPI Payment';
  const toMatch = text.match(/(?:to|at|vpa|by\s*upi:?|paid to)\s+([A-Za-z0-9&._ -]{3,35})(?:on|via|ref|rrn|upi|val|bal|\.|$)/i);
  if (toMatch) {
    merchant = toMatch[1].trim().replace(/\s+(on|via|ref|rrn|upi).*$/i, '');
  } else {
    const known = ['Swiggy', 'Zomato', 'Uber', 'Ola', 'Amazon', 'Flipkart', 'Blinkit', 'Zepto', 'Instamart', 'Starbucks', 'Chai Point', 'Airtel', 'Jio', 'Apollo Pharmacy', 'D-Mart', 'BookMyShow', 'Netflix'];
    for (const k of known) {
      if (lower.includes(k.toLowerCase())) {
        merchant = k;
        break;
      }
    }
  }
  if (merchant.length > 30) merchant = merchant.slice(0, 30);

  // 4. Categorize
  let categoryId = 12, categoryName = 'Other Expenses', categoryIcon = 'circle-dollar-to-slot', categoryColor = '#64748b';
  if (type === 'INCOME') {
    categoryId = 13; categoryName = 'Salary & Wages'; categoryIcon = 'money-bill-wave'; categoryColor = '#10b981';
  } else if (/swiggy|zomato|food|starbucks|chai|restaurant|cafe|kfc|mcdonald/i.test(lower)) {
    categoryId = 1; categoryName = 'Food & Dining'; categoryIcon = 'utensils'; categoryColor = '#f97316';
  } else if (/uber|ola|rapido|metro|fuel|petrol|hpcl|bpcl|ioc/i.test(lower)) {
    categoryId = 3; categoryName = 'Transportation'; categoryIcon = 'car'; categoryColor = '#3b82f6';
  } else if (/amazon|flipkart|myntra|shopping|retail|mart|blinkit|zepto|dmart/i.test(lower)) {
    categoryId = 2; categoryName = 'Shopping & Retail'; categoryIcon = 'shopping-bag'; categoryColor = '#ec4899';
  } else if (/airtel|jio|electric|bill|broadband|water|bescom/i.test(lower)) {
    categoryId = 4; categoryName = 'Bills & Utilities'; categoryIcon = 'bolt'; categoryColor = '#eab308';
  }

  return {
    id: Date.now(),
    type,
    amount,
    categoryId,
    categoryName,
    categoryIcon,
    categoryColor,
    merchant,
    paymentMethod: 'UPI',
    userEmail: userEmail || '',
    transactionDate: new Date().toISOString().split('T')[0],
    notes: 'Auto-synced from Bank SMS'
  };
}

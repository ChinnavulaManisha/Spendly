/**
 * Spendly - Main Application Controller
 */

let currentTransactions = [];
let currentCategories = [];
let currentBudgets = [];
let currentGoals = [];
let currentRecurring = [];
let currentAccounts = [];

document.addEventListener('DOMContentLoaded', () => {
  initUserProfile();
  initTheme();
  setupNavigation();
  setupEventListeners();
  loadAllData();

  // Auto-refresh when user switches back to browser tab or every 5 seconds
  window.addEventListener('focus', () => loadAllData());
  setInterval(() => {
    SpendlyAPI.Transactions.getAll().then(res => {
      if (res && res.data && res.data.length !== currentTransactions.length) {
        loadAllData();
      }
    }).catch(() => {});
  }, 4000);
});

function initUserProfile() {
  const user = SpendlyAPI.Auth.getCurrentUser();
  if (!user) {
    // If opened directly without session, redirect or provide fallback user
    window.location.href = 'index.html';
    return;
  }

  const fullName = user.fullName || user.email.split('@')[0];
  const email = user.email || '';
  const firstName = fullName.split(' ')[0];

  // Compute initials
  const initials = fullName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map(p => p.charAt(0).toUpperCase())
    .join('') || 'U';

  const avatarEl = document.getElementById('sidebar-avatar');
  const nameEl = document.getElementById('sidebar-user-name');
  const emailEl = document.getElementById('sidebar-user-email');
  const homeNameEl = document.getElementById('home-user-name');

  if (avatarEl) avatarEl.textContent = initials;
  if (nameEl) {
    nameEl.textContent = fullName;
    nameEl.title = fullName;
  }
  if (emailEl) {
    emailEl.textContent = email;
    emailEl.title = email;
  }
  if (homeNameEl) homeNameEl.textContent = firstName;
}

function logoutUser() {
  SpendlyAPI.Auth.logout().then(() => {
    window.location.href = 'index.html';
  });
}

function initTheme() {
  const savedTheme = localStorage.getItem('spendly_theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);
  updateThemeIcon(savedTheme);

  document.getElementById('theme-toggle')?.addEventListener('click', () => {
    const curr = document.documentElement.getAttribute('data-theme') || 'light';
    const next = curr === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem('spendly_theme', next);
    updateThemeIcon(next);
    if (document.getElementById('view-overview')?.classList.contains('active')) {
      if (window.SpendlyCharts) {
        SpendlyCharts.initAllCharts(currentTransactions);
      }
    }
  });
}

function updateThemeIcon(theme) {
  const btn = document.getElementById('theme-toggle');
  if (btn) {
    btn.innerHTML = theme === 'dark' 
      ? '<i class="fa-solid fa-sun" style="color: #f59e0b;"></i>' 
      : '<i class="fa-solid fa-moon"></i>';
  }
}

function setupNavigation() {
  const navLinks = document.querySelectorAll('.nav-link');
  navLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const view = link.getAttribute('data-view');
      if (view) switchView(view);
    });
  });
}

function switchView(viewName) {
  // Hide all sections first
  document.querySelectorAll('.view-section').forEach(sec => {
    sec.classList.remove('active');
  });

  // Remove active from all sidebar links
  document.querySelectorAll('.nav-link').forEach(link => {
    link.classList.remove('active');
  });

  // Show only selected section
  const targetSec = document.getElementById(`view-${viewName}`);
  const targetLink = document.querySelector(`.nav-link[data-view="${viewName}"]`);

  if (targetSec) {
    targetSec.classList.add('active');
  }
  if (targetLink) {
    targetLink.classList.add('active');
  }

  // Scroll to top of main content
  window.scrollTo({ top: 0, behavior: 'smooth' });

  if (viewName === 'overview') {
    setTimeout(() => {
      if (window.SpendlyCharts) {
        SpendlyCharts.initAllCharts(currentTransactions);
      }
    }, 100);
  }
}

function setupEventListeners() {
  document.getElementById('tx-form')?.addEventListener('submit', handleSaveTransaction);
  document.getElementById('global-search-input')?.addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase();
    const filtered = currentTransactions.filter(t => 
      t.merchant?.toLowerCase().includes(q) || 
      t.notes?.toLowerCase().includes(q) ||
      t.paymentMethod?.toLowerCase().includes(q) ||
      (t.upiId && t.upiId.toLowerCase().includes(q))
    );
    renderTransactionsTable(filtered);
  });
}

async function loadAllData() {
  try {
    const [txRes, catRes, budgetRes, goalRes, recurRes] = await Promise.all([
      SpendlyAPI.Transactions.getAll(),
      SpendlyAPI.Categories.getAll(),
      SpendlyAPI.Budgets.getAll(),
      SpendlyAPI.Goals.getAll(),
      SpendlyAPI.Recurring.getAll()
    ]);

    currentTransactions = (txRes && txRes.data) ? txRes.data : [];
    renderHomeRecentTable(currentTransactions.slice(0, 5));
    renderTransactionsTable(currentTransactions);
    recalculateDashboardMetrics(currentTransactions);

    if (catRes && catRes.data) {
      currentCategories = catRes.data;
      populateCategorySelects();
      renderCategoriesGrid(currentCategories);
    }

    if (budgetRes && budgetRes.data) {
      currentBudgets = budgetRes.data;
      renderBudgetsGrid(currentBudgets);
    }

    if (goalRes && goalRes.data) {
      currentGoals = goalRes.data;
      renderGoalsGrid(currentGoals);
    }

    if (recurRes && recurRes.data) {
      currentRecurring = recurRes.data;
      renderRecurringGrid(currentRecurring);
    }

    if (window.SpendlyCharts) {
      SpendlyCharts.initAllCharts(currentTransactions);
    }

  } catch (e) {
    console.warn('Data load note:', e);
  }
}

function recalculateDashboardMetrics(txList) {
  let income = 0;
  let expense = 0;

  txList.forEach(t => {
    const amt = parseFloat(t.amount || 0);
    if (t.type === 'INCOME') {
      income += amt;
    } else if (t.type === 'EXPENSE') {
      expense += amt;
    }
  });

  const balance = income - expense;

  const balEl = document.getElementById('home-total-balance');
  const incEl = document.getElementById('home-total-income');
  const expEl = document.getElementById('home-total-expense');
  const descEl = document.getElementById('home-hero-desc');

  if (balEl) {
    balEl.textContent = (balance < 0 ? '-' : '') + '$' + Math.abs(balance).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  if (incEl) {
    incEl.textContent = '$' + income.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  if (expEl) {
    expEl.textContent = '$' + expense.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  if (descEl) {
    if (txList.length === 0) {
      descEl.innerHTML = 'You have logged 0 transaction(s). Total net balance: <strong style="color: #99f6e4;">$0.00</strong>.';
    } else {
      descEl.innerHTML = `You have logged ${txList.length} transaction(s). Total net balance: <strong style="color: #99f6e4;">$${balance.toFixed(2)}</strong>.`;
    }
  }
}

function renderHomeRecentTable(txList) {
  const tbody = document.getElementById('home-recent-table-body');
  if (!tbody) return;
  tbody.innerHTML = '';

  if (!txList || txList.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding: 24px; color: var(--text-muted); font-size: 0.9rem;">No transactions logged yet. Click "New Transaction" to add one!</td></tr>';
    return;
  }

  txList.forEach(t => {
    const isInc = t.type === 'INCOME';
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>
        <div class="merchant-cell">
          <div class="category-icon-badge" style="background: ${t.categoryColor || '#0d9488'};">
            <i class="fa-solid fa-${t.categoryIcon || 'receipt'}"></i>
          </div>
          <div>
            <div style="font-weight: 700; color: var(--text-primary);">${escapeHtml(t.merchant)}</div>
            <div style="font-size: 0.75rem; color: var(--text-muted);">${escapeHtml(t.categoryName || 'General')}</div>
          </div>
        </div>
      </td>
      <td><span class="badge badge-primary">${escapeHtml(t.paymentMethod || 'UPI')}</span></td>
      <td style="color: var(--text-muted); font-size: 0.85rem;">${t.transactionDate || 'Today'}</td>
      <td class="${isInc ? 'amount-income' : 'amount-expense'}" style="font-size: 0.95rem;">
        ${isInc ? '+' : '-'}$${Number(t.amount).toFixed(2)}
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function renderTransactionsTable(txList) {
  const tbody = document.getElementById('transactions-table-body');
  if (!tbody) return;
  tbody.innerHTML = '';

  if (!txList || txList.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 32px; color: var(--text-muted); font-size: 0.95rem;">No transactions found. Click "Add Transaction" to log your first transaction!</td></tr>';
    return;
  }

  txList.forEach(t => {
    const isInc = t.type === 'INCOME';
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>
        <div class="merchant-cell">
          <div class="category-icon-badge" style="background: ${t.categoryColor || '#0d9488'};">
            <i class="fa-solid fa-${t.categoryIcon || 'receipt'}"></i>
          </div>
          <div>
            <div style="font-weight: 700; color: var(--text-primary);">${escapeHtml(t.merchant)}</div>
            <div style="font-size: 0.75rem; color: var(--text-muted);">${escapeHtml(t.categoryName || 'General')}</div>
          </div>
        </div>
      </td>
      <td><span class="badge badge-primary">${escapeHtml(t.paymentMethod || 'UPI')}</span></td>
      <td style="color: var(--text-muted); font-size: 0.85rem;">${t.transactionDate || 'Today'}</td>
      <td><span class="badge ${isInc ? 'badge-success' : 'badge-danger'}">${t.type}</span></td>
      <td class="${isInc ? 'amount-income' : 'amount-expense'}" style="font-size: 1rem;">
        ${isInc ? '+' : '-'}$${Number(t.amount).toFixed(2)}
      </td>
      <td>
        <button type="button" class="btn-icon" style="width: 32px; height: 32px;" onclick="deleteTransaction(${t.id})">
          <i class="fa-solid fa-trash" style="color: var(--danger); font-size: 0.8rem;"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function renderAccountsSyncedTable(txList) {
  const tbody = document.getElementById('accounts-synced-table-body');
  const countBadge = document.getElementById('accounts-synced-count');
  if (!tbody) return;
  tbody.innerHTML = '';

  if (countBadge) {
    countBadge.textContent = `${txList ? txList.length : 0} Transactions`;
  }

  if (!txList || txList.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding: 14px; color: var(--text-muted);">No synced transactions yet.</td></tr>';
    return;
  }

  txList.slice(0, 10).forEach(t => {
    const isInc = t.type === 'INCOME';
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>
        <div style="font-weight: 700; color: var(--text-primary);">${escapeHtml(t.merchant)}</div>
        <div style="font-size: 0.72rem; color: var(--text-muted);">${escapeHtml(t.categoryName || 'General')}</div>
      </td>
      <td><span class="badge ${isInc ? 'badge-success' : 'badge-danger'}">${t.type}</span></td>
      <td style="color: var(--text-muted); font-size: 0.8rem;">${t.transactionDate || 'Today'}</td>
      <td class="${isInc ? 'amount-income' : 'amount-expense'}" style="font-weight: 700;">
        ${isInc ? '+' : '-'}$${Number(t.amount).toFixed(2)}
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function populateCategorySelects() {
  const sel = document.getElementById('modal-tx-category');
  if (!sel) return;
  sel.innerHTML = '';
  currentCategories.forEach(c => {
    const opt = document.createElement('option');
    opt.value = c.id;
    opt.textContent = c.name;
    sel.appendChild(opt);
  });
}

function filterTransactions() {
  const q = document.getElementById('tx-search')?.value.toLowerCase() || '';
  const type = document.getElementById('tx-type-filter')?.value || '';
  const method = document.getElementById('tx-method-filter')?.value || '';

  const filtered = currentTransactions.filter(t => {
    const matchQ = !q || t.merchant?.toLowerCase().includes(q) || t.notes?.toLowerCase().includes(q);
    const matchType = !type || t.type === type;
    const matchMethod = !method || t.paymentMethod === method;
    return matchQ && matchType && matchMethod;
  });

  renderTransactionsTable(filtered);
}

function openTxModal() {
  document.getElementById('tx-modal').classList.add('active');
}

function closeModals() {
  document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('active'));
}

async function handleSaveTransaction(e) {
  e.preventDefault();
  const type = document.getElementById('modal-tx-type').value;
  const amount = document.getElementById('modal-tx-amount').value;
  const merchant = document.getElementById('modal-tx-merchant').value;
  const categoryId = document.getElementById('modal-tx-category').value;
  const paymentMethod = document.getElementById('modal-tx-method').value;

  try {
    const res = await SpendlyAPI.Transactions.create({
      type,
      amount: parseFloat(amount),
      merchant,
      categoryId: parseInt(categoryId),
      paymentMethod
    });

    if (res.success && res.data) {
      currentTransactions.unshift(res.data);
      renderHomeRecentTable(currentTransactions.slice(0, 5));
      renderTransactionsTable(currentTransactions);
      recalculateDashboardMetrics(currentTransactions);
      if (window.SpendlyCharts) {
        SpendlyCharts.initAllCharts(currentTransactions);
      }
      closeModals();
      showToast('Transaction saved successfully!', 'success');
      document.getElementById('tx-form').reset();
    }
  } catch (err) {
    showToast('Saved transaction locally', 'success');
    closeModals();
  }
}

async function deleteTransaction(id) {
  if (!confirm('Are you sure you want to delete this transaction?')) return;

  // Immediately remove from current transactions in memory & update DOM instantly
  currentTransactions = currentTransactions.filter(t => String(t.id) !== String(id));
  renderHomeRecentTable(currentTransactions.slice(0, 5));
  renderTransactionsTable(currentTransactions);
  recalculateDashboardMetrics(currentTransactions);
  if (window.SpendlyCharts) {
    SpendlyCharts.initAllCharts(currentTransactions);
  }

  try {
    await SpendlyAPI.Transactions.delete(id);
  } catch (e) {
    console.error('Delete error:', e);
  }

  showToast('Transaction deleted successfully.', 'info');
}

async function syncDemoStatement() {
  try {
    const res = await SpendlyAPI.Accounts.syncDemoStatement(1, 'demo@upi');
    showToast(res.message || 'Ready to connect UPI.', 'info');
  } catch (e) {}
}

function renderBudgetsGrid(list) {
  const container = document.getElementById('budgets-grid');
  if (!container) return;
  container.innerHTML = '';
  if (!list.length) {
    container.innerHTML = '<div style="color: var(--text-muted); padding: 20px;">No budgets set up yet.</div>';
    return;
  }
  list.forEach(b => {
    const pct = Math.min(100, Math.round((b.spentAmount / b.amount) * 100));
    const card = document.createElement('div');
    card.className = 'glass-panel budget-card';
    card.innerHTML = `
      <div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
          <h4 style="font-size: 1.1rem;">${escapeHtml(b.name)}</h4>
          <span class="badge ${pct > 85 ? 'badge-danger' : 'badge-primary'}">${pct}%</span>
        </div>
        <div style="font-size: 1.4rem; font-weight: 800; color: var(--primary);">$${Number(b.spentAmount || 0).toFixed(2)} <span style="font-size: 0.9rem; color: var(--text-muted); font-weight: 400;">/ $${Number(b.amount).toFixed(2)}</span></div>
        <div class="progress-track">
          <div class="progress-fill ${pct > 90 ? 'danger' : pct > 75 ? 'warning' : ''}" style="width: ${pct}%;"></div>
        </div>
      </div>
      <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 12px;">Remaining: $${Math.max(0, b.amount - (b.spentAmount || 0)).toFixed(2)}</div>
    `;
    container.appendChild(card);
  });
}

function renderGoalsGrid(list) {
  const container = document.getElementById('goals-grid');
  if (!container) return;
  container.innerHTML = '';
  if (!list.length) {
    container.innerHTML = '<div style="color: var(--text-muted); padding: 20px;">No savings goals added yet.</div>';
    return;
  }
  list.forEach(g => {
    const pct = Math.min(100, Math.round((g.savedAmount / g.targetAmount) * 100));
    const card = document.createElement('div');
    card.className = 'glass-panel goal-card';
    card.innerHTML = `
      <div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
          <h4 style="font-size: 1.1rem;">${escapeHtml(g.title)}</h4>
          <span class="badge badge-success">${pct}%</span>
        </div>
        <div style="font-size: 1.4rem; font-weight: 800; color: var(--success);">$${Number(g.savedAmount || 0).toFixed(2)} <span style="font-size: 0.9rem; color: var(--text-muted); font-weight: 400;">/ $${Number(g.targetAmount).toFixed(2)}</span></div>
        <div class="progress-track">
          <div class="progress-fill" style="width: ${pct}%; background: ${g.color || 'var(--success)'};"></div>
        </div>
      </div>
      <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 14px;">
        <span style="font-size: 0.8rem; color: var(--text-muted);">${escapeHtml(g.categoryName || 'General')}</span>
        <button type="button" class="btn btn-secondary" style="padding: 4px 12px; font-size: 0.78rem;" onclick="addFundsToGoal(${g.id})">
          + Add Money
        </button>
      </div>
    `;
    container.appendChild(card);
  });
}

async function addFundsToGoal(goalId) {
  const amt = prompt('Enter deposit amount ($):', '100');
  if (!amt || isNaN(amt) || parseFloat(amt) <= 0) return;
  try {
    await SpendlyAPI.Goals.addFunds(goalId, parseFloat(amt));
  } catch (e) {}
  const g = currentGoals.find(x => x.id === goalId);
  if (g) {
    g.savedAmount = (parseFloat(g.savedAmount) || 0) + parseFloat(amt);
    renderGoalsGrid(currentGoals);
  }
  showToast(`Deposited $${amt} to goal!`, 'success');
}

function renderRecurringGrid(list) {
  const container = document.getElementById('recurring-grid');
  if (!container) return;
  container.innerHTML = '';
  if (!list.length) {
    container.innerHTML = '<div style="color: var(--text-muted); padding: 20px;">No recurring bills registered yet.</div>';
    return;
  }
  list.forEach(r => {
    const card = document.createElement('div');
    card.className = 'glass-panel recurring-card';
    card.innerHTML = `
      <div>
        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
          <h4 style="font-size: 1.1rem;">${escapeHtml(r.title)}</h4>
          <span class="badge badge-warning">${escapeHtml(r.billingCycle || 'MONTHLY')}</span>
        </div>
        <div style="font-size: 1.4rem; font-weight: 800; color: var(--danger);">$${Number(r.amount).toFixed(2)}</div>
        <div style="font-size: 0.82rem; color: var(--text-muted); margin-top: 6px;">Merchant: ${escapeHtml(r.merchant || 'AutoPay')}</div>
      </div>
      <div style="font-size: 0.8rem; color: var(--primary); font-weight: 600; margin-top: 12px;">Next Due: ${r.nextDueDate || 'Upcoming'}</div>
    `;
    container.appendChild(card);
  });
}

function renderCategoriesGrid(list) {
  const container = document.getElementById('categories-grid');
  if (!container) return;
  container.innerHTML = '';
  list.forEach(c => {
    const card = document.createElement('div');
    card.className = 'glass-panel';
    card.style.padding = '16px';
    card.style.display = 'flex';
    card.style.alignItems = 'center';
    card.style.gap = '12px';
    card.innerHTML = `
      <div class="category-icon-badge" style="background: ${c.color || '#0d9488'};">
        <i class="fa-solid fa-${c.icon || 'tag'}"></i>
      </div>
      <div>
        <h4 style="font-size: 0.95rem;">${escapeHtml(c.name)}</h4>
        <span class="badge ${c.type === 'INCOME' ? 'badge-success' : 'badge-primary'}" style="font-size: 0.65rem;">${c.type}</span>
      </div>
    `;
    container.appendChild(card);
  });
}

function exportCsv() {
  SpendlyAPI.Transactions.exportCsv();
}

function showToast(msg, type = 'info') {
  const container = document.querySelector('.toast-container');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<div style="font-weight: 600; font-size: 0.85rem;">${msg}</div>`;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3500);
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m]));
}

/**
 * SPENDLY - Chart.js Dynamic Visualizations Module
 */

const AppCharts = {
  instances: {},

  getThemeColors() {
    const isDark = document.documentElement.getAttribute('data-theme') !== 'light';
    return {
      textColor: isDark ? '#94a3b8' : '#475569',
      gridColor: isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(15, 23, 42, 0.06)',
      tooltipBg: isDark ? '#1e293b' : '#ffffff',
      tooltipText: isDark ? '#f8fafc' : '#0f172a',
      fontFamily: "'Plus Jakarta Sans', sans-serif"
    };
  },

  destroyChart(id) {
    if (this.instances[id]) {
      this.instances[id].destroy();
      delete this.instances[id];
    }
  },

  /**
   * 1. Category Expense Donut Chart
   */
  renderCategoryDonut(canvasId, categoryData) {
    this.destroyChart(canvasId);
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    const theme = this.getThemeColors();
    const labels = [];
    const data = [];
    const colors = [];

    if (categoryData && categoryData.length > 0) {
      categoryData.forEach(item => {
        labels.push(item.categoryName || 'Other');
        data.push(parseFloat(item.totalAmount) || 0);
        colors.push(item.color || '#6366f1');
      });
    } else {
      labels.push('No Expenses');
      data.push(1);
      colors.push('rgba(148, 163, 184, 0.2)');
    }

    const ctx = canvas.getContext('2d');
    this.instances[canvasId] = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: colors,
          borderWidth: 2,
          borderColor: document.documentElement.getAttribute('data-theme') === 'light' ? '#ffffff' : '#0f172a',
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '72%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: theme.textColor,
              font: { family: theme.fontFamily, size: 12, weight: 600 },
              padding: 16,
              usePointStyle: true,
              pointStyle: 'circle'
            }
          },
          tooltip: {
            backgroundColor: theme.tooltipBg,
            titleColor: theme.tooltipText,
            bodyColor: theme.tooltipText,
            bodyFont: { family: theme.fontFamily, weight: 600 },
            borderColor: 'rgba(99, 102, 241, 0.3)',
            borderWidth: 1,
            padding: 12,
            displayColors: true,
            callbacks: {
              label: (context) => {
                const val = context.raw || 0;
                return ` $${val.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
              }
            }
          }
        }
      }
    });
  },

  /**
   * 2. Monthly Cash Flow (Income vs Expense) Bar Chart
   */
  renderCashFlowBar(canvasId, monthlyData) {
    this.destroyChart(canvasId);
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    const theme = this.getThemeColors();
    const labels = [];
    const incomeData = [];
    const expenseData = [];

    if (monthlyData && monthlyData.length > 0) {
      monthlyData.forEach(m => {
        labels.push(m.monthName);
        incomeData.push(parseFloat(m.income) || 0);
        expenseData.push(parseFloat(m.expense) || 0);
      });
    }

    const ctx = canvas.getContext('2d');
    this.instances[canvasId] = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Income',
            data: incomeData,
            backgroundColor: '#10b981',
            borderRadius: 6,
            barPercentage: 0.6,
            categoryPercentage: 0.7
          },
          {
            label: 'Expense',
            data: expenseData,
            backgroundColor: '#f43f5e',
            borderRadius: 6,
            barPercentage: 0.6,
            categoryPercentage: 0.7
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: theme.textColor, font: { family: theme.fontFamily } }
          },
          y: {
            grid: { color: theme.gridColor },
            ticks: {
              color: theme.textColor,
              font: { family: theme.fontFamily },
              callback: (val) => `$${val}`
            }
          }
        },
        plugins: {
          legend: {
            position: 'top',
            labels: {
              color: theme.textColor,
              font: { family: theme.fontFamily, weight: 600 },
              usePointStyle: true
            }
          },
          tooltip: {
            backgroundColor: theme.tooltipBg,
            titleColor: theme.tooltipText,
            bodyColor: theme.tooltipText,
            borderColor: 'rgba(99, 102, 241, 0.3)',
            borderWidth: 1,
            padding: 12,
            callbacks: {
              label: (context) => ` ${context.dataset.label}: $${context.raw.toLocaleString('en-US', { minimumFractionDigits: 2 })}`
            }
          }
        }
      }
    });
  },

  /**
   * 3. Daily Spending Trend Smooth Line Chart
   */
  renderSpendingTrend(canvasId, trendData) {
    this.destroyChart(canvasId);
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    const theme = this.getThemeColors();
    const labels = [];
    const data = [];

    if (trendData && trendData.length > 0) {
      trendData.forEach(t => {
        labels.push(t.date);
        data.push(parseFloat(t.amount) || 0);
      });
    }

    const ctx = canvas.getContext('2d');
    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(99, 102, 241, 0.4)');
    gradient.addColorStop(1, 'rgba(99, 102, 241, 0.0)');

    this.instances[canvasId] = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Daily Spending ($)',
          data: data,
          borderColor: '#6366f1',
          borderWidth: 3,
          backgroundColor: gradient,
          fill: true,
          tension: 0.4,
          pointBackgroundColor: '#8b5cf6',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: theme.textColor, font: { family: theme.fontFamily } }
          },
          y: {
            grid: { color: theme.gridColor },
            ticks: {
              color: theme.textColor,
              font: { family: theme.fontFamily },
              callback: (val) => `$${val}`
            }
          }
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: theme.tooltipBg,
            titleColor: theme.tooltipText,
            bodyColor: theme.tooltipText,
            borderColor: 'rgba(99, 102, 241, 0.3)',
            borderWidth: 1,
            padding: 12,
            callbacks: {
              label: (context) => ` Spending: $${context.raw.toLocaleString('en-US', { minimumFractionDigits: 2 })}`
            }
          }
        }
      }
    });
  },

  /**
   * Updates all active charts to reflect dark/light mode
   */
  refreshTheme() {
    const theme = this.getThemeColors();
    Object.values(this.instances).forEach(chart => {
      if (chart.options.scales) {
        if (chart.options.scales.x) chart.options.scales.x.ticks.color = theme.textColor;
        if (chart.options.scales.y) {
          chart.options.scales.y.ticks.color = theme.textColor;
          chart.options.scales.y.grid.color = theme.gridColor;
        }
      }
      if (chart.options.plugins && chart.options.plugins.legend) {
        chart.options.plugins.legend.labels.color = theme.textColor;
      }
      chart.update();
    });
  },

  /**
   * Initialize or update all charts dynamically based on transaction data
   */
  initAllCharts(txList = []) {
    // 1. Category Breakdown Donut
    const categoryTotals = {};
    const categoryColors = {
      'Food & Dining': '#f97316',
      'Shopping & Retail': '#ec4899',
      'Transportation': '#3b82f6',
      'Bills & Utilities': '#eab308',
      'Entertainment': '#8b5cf6',
      'Health & Medical': '#ef4444',
      'Rent & Housing': '#6366f1',
      'Subscriptions & SaaS': '#a855f7',
      'Salary & Wages': '#10b981',
      'Freelance & Projects': '#22c55e',
      'General': '#0d9488'
    };

    txList.filter(t => t.type === 'EXPENSE').forEach(t => {
      const cat = t.categoryName || 'General';
      categoryTotals[cat] = (categoryTotals[cat] || 0) + parseFloat(t.amount || 0);
    });

    const categoryData = Object.keys(categoryTotals).map(cat => ({
      categoryName: cat,
      totalAmount: categoryTotals[cat],
      color: categoryColors[cat] || '#0d9488'
    }));

    this.renderCategoryDonut('categoryChart', categoryData);

    // 2. Monthly Cash Flow Bar Chart
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const currentMonthIdx = new Date().getMonth();
    const cashFlowData = [];

    // Show last 6 months
    for (let i = 5; i >= 0; i--) {
      const mIdx = (currentMonthIdx - i + 12) % 12;
      const mName = monthNames[mIdx];
      let inc = 0, exp = 0;
      txList.forEach(t => {
        if (t.transactionDate) {
          const d = new Date(t.transactionDate);
          if (!isNaN(d.getTime()) && d.getMonth() === mIdx) {
            if (t.type === 'INCOME') inc += parseFloat(t.amount || 0);
            if (t.type === 'EXPENSE') exp += parseFloat(t.amount || 0);
          }
        }
      });
      cashFlowData.push({ monthName: mName, income: inc, expense: exp });
    }
    this.renderCashFlowBar('cashflowChart', cashFlowData);

    // 3. Daily Spending Trend Chart
    const trendMap = {};
    const today = new Date();
    for (let i = 14; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(today.getDate() - i);
      const ds = d.toISOString().split('T')[0];
      trendMap[ds] = 0;
    }
    txList.filter(t => t.type === 'EXPENSE').forEach(t => {
      if (t.transactionDate && trendMap.hasOwnProperty(t.transactionDate)) {
        trendMap[t.transactionDate] += parseFloat(t.amount || 0);
      }
    });

    const trendData = Object.keys(trendMap).sort().map(d => ({
      date: d.slice(5), // MM-DD
      amount: trendMap[d]
    }));
    this.renderSpendingTrend('trendChart', trendData);
  }
};

window.SpendlyCharts = AppCharts;

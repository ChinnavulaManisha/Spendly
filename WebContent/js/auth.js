/**
 * SPENDLY - Authentication & Landing Interactions
 */

document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initTabs();
  initPasswordToggles();
  initAuthForms();
  checkExistingSession();
});

function initTheme() {
  const savedTheme = localStorage.getItem('spendly_theme') || 'dark';
  document.documentElement.setAttribute('data-theme', savedTheme);
}

function initTabs() {
  const tabBtns = document.querySelectorAll('.auth-tab-btn');
  const tabContents = document.querySelectorAll('.auth-tab-content');

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetTab = btn.getAttribute('data-tab');

      tabBtns.forEach(b => b.classList.remove('active'));
      tabContents.forEach(c => c.classList.remove('active'));

      btn.classList.add('active');
      const activeContent = document.getElementById(`${targetTab}-tab`);
      if (activeContent) {
        activeContent.classList.add('active');
      }
    });
  });
}

function initPasswordToggles() {
  const toggleBtns = document.querySelectorAll('.password-toggle-btn');
  toggleBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-target');
      const input = document.getElementById(targetId);
      if (input) {
        const isPassword = input.getAttribute('type') === 'password';
        input.setAttribute('type', isPassword ? 'text' : 'password');
        btn.innerHTML = isPassword ? '<i class="fa-solid fa-eye-slash"></i>' : '<i class="fa-solid fa-eye"></i>';
      }
    });
  });
}

function initAuthForms() {
  // Login Form
  const loginForm = document.getElementById('login-form');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('login-email').value.trim();
      const password = document.getElementById('login-password').value;
      const submitBtn = loginForm.querySelector('button[type="submit"]');

      try {
        setButtonLoading(submitBtn, true, 'Signing In...');
        const res = await API.auth.login({ email, password });
        showToast('success', 'Welcome Back!', res.message || 'Login successful!');
        localStorage.setItem('spendly_user', JSON.stringify(res.data));
        if (res.data && res.data.themePreference) {
          localStorage.setItem('spendly_theme', res.data.themePreference);
        }
        setTimeout(() => {
          window.location.href = 'dashboard.html';
        }, 800);
      } catch (err) {
        showToast('error', 'Login Failed', err.message);
      } finally {
        setButtonLoading(submitBtn, false, 'Sign In');
      }
    });
  }

  // Register Form
  const registerForm = document.getElementById('register-form');
  if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const fullName = document.getElementById('reg-name').value.trim();
      const email = document.getElementById('reg-email').value.trim();
      const password = document.getElementById('reg-password').value;
      const currency = document.getElementById('reg-currency').value;
      const submitBtn = registerForm.querySelector('button[type="submit"]');

      try {
        setButtonLoading(submitBtn, true, 'Creating Account...');
        const res = await API.auth.register({ fullName, email, password, currency });
        showToast('success', 'Account Created!', 'Welcome to Spendly!');
        localStorage.setItem('spendly_user', JSON.stringify(res.data));
        setTimeout(() => {
          window.location.href = 'dashboard.html';
        }, 800);
      } catch (err) {
        showToast('error', 'Registration Failed', err.message);
      } finally {
        setButtonLoading(submitBtn, false, 'Create Free Account');
      }
    });
  }

  // Forgot Password Form
  const forgotForm = document.getElementById('forgot-form');
  if (forgotForm) {
    forgotForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('forgot-email').value.trim();
      const newPassword = document.getElementById('forgot-new-password').value;
      const submitBtn = forgotForm.querySelector('button[type="submit"]');

      try {
        setButtonLoading(submitBtn, true, 'Resetting Password...');
        const res = await API.auth.forgotPassword({ email, newPassword });
        showToast('success', 'Success', res.message || 'Password reset successfully!');
        // Switch to login tab
        document.querySelector('.auth-tab-btn[data-tab="login"]').click();
      } catch (err) {
        showToast('error', 'Reset Failed', err.message);
      } finally {
        setButtonLoading(submitBtn, false, 'Reset Password');
      }
    });
  }
}

async function checkExistingSession() {
  try {
    const res = await API.auth.checkSession();
    if (res.data && res.data.authenticated) {
      window.location.href = 'dashboard.html';
    }
  } catch (ignored) {}
}

function setButtonLoading(btn, isLoading, text) {
  if (!btn) return;
  btn.disabled = isLoading;
  btn.innerHTML = isLoading 
    ? `<i class="fa-solid fa-circle-notch fa-spin"></i> ${text}` 
    : text;
}

function showToast(type, title, message) {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  const icon = type === 'success' 
    ? '<i class="fa-solid fa-circle-check toast-icon"></i>' 
    : type === 'error' 
    ? '<i class="fa-solid fa-circle-exclamation toast-icon"></i>' 
    : '<i class="fa-solid fa-circle-info toast-icon"></i>';

  toast.innerHTML = `
    ${icon}
    <div class="toast-content">
      <div class="toast-title">${title}</div>
      <div class="toast-message">${message}</div>
    </div>
  `;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

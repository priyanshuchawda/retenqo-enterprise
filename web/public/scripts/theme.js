const STORAGE_KEY = 'retenqo-theme';
const THEME_VALUES = ['system', 'light', 'dark'];

function readSavedTheme() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return THEME_VALUES.includes(stored) ? stored : 'system';
  } catch {
    return 'system';
  }
}

function saveTheme(theme) {
  try {
    localStorage.setItem(STORAGE_KEY, theme);
  } catch {
    // Theme changes still apply for this page when storage is unavailable.
  }
}

function applyTheme(theme, themeSelect) {
  if (theme === 'light' || theme === 'dark') {
    document.documentElement.dataset.theme = theme;
  } else {
    delete document.documentElement.dataset.theme;
  }
  if (themeSelect) themeSelect.value = theme;
}

const initialTheme = readSavedTheme();
applyTheme(initialTheme, null);

function connectThemeSelector() {
  const themeSelect = document.querySelector('[data-theme-select]');
  if (!(themeSelect instanceof HTMLSelectElement)) return;
  applyTheme(initialTheme, themeSelect);
  themeSelect.addEventListener('change', () => {
    const theme = THEME_VALUES.includes(themeSelect.value)
      ? themeSelect.value
      : 'system';
    saveTheme(theme);
    applyTheme(theme, themeSelect);
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', connectThemeSelector, {
    once: true,
  });
} else {
  connectThemeSelector();
}

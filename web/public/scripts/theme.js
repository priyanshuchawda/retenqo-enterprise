const STORAGE_KEY = 'retenqo-theme';
const themeSelect = document.querySelector('[data-theme-select]');
function applyTheme(theme) {
  if (theme === 'light' || theme === 'dark') {
    document.documentElement.dataset.theme = theme;
  } else {
    delete document.documentElement.dataset.theme;
  }
  if (themeSelect) themeSelect.value = theme;
}
const storedTheme = localStorage.getItem(STORAGE_KEY);
const initialTheme =
  storedTheme === 'light' || storedTheme === 'dark' ? storedTheme : 'system';
applyTheme(initialTheme);
themeSelect?.addEventListener('change', () => {
  const theme = themeSelect.value;
  localStorage.setItem(STORAGE_KEY, theme);
  applyTheme(theme);
});

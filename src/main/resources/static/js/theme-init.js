(() => {
  const storageKey = 'thesis-management-theme';
  let savedTheme = null;
  try {
    savedTheme = localStorage.getItem(storageKey);
  } catch (_) {
    // Storage can be unavailable in privacy-restricted contexts; OS preference remains safe.
  }
  const theme = savedTheme === 'light' || savedTheme === 'dark'
    ? savedTheme
    : (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  document.documentElement.dataset.theme = theme;
  document.documentElement.style.colorScheme = theme;
})();

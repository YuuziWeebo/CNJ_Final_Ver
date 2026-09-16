(() => {
  const storageKey = 'thesis-management-theme';
  const root = document.documentElement;
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  let transitionRunning = false;

  const setToggleState = theme => {
    document.querySelectorAll('[data-theme-toggle]').forEach(button => {
      const dark = theme === 'dark';
      button.setAttribute('aria-label', dark ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối');
      button.setAttribute('aria-pressed', String(dark));
      const icon = button.querySelector('i');
      if (icon) icon.className = dark ? 'bi bi-sun' : 'bi bi-moon-stars';
    });
  };

  const applyTheme = theme => {
    root.dataset.theme = theme;
    root.style.colorScheme = theme;
    setToggleState(theme);
    try {
      localStorage.setItem(storageKey, theme);
    } catch (_) {
      // Theme switching must remain functional when persistent storage is unavailable.
    }
  };

  const setRandomSpreadGeometry = () => {
    const x = innerWidth * (0.1 + Math.random() * 0.8);
    const y = innerHeight * (0.1 + Math.random() * 0.8);
    // The farthest horizontal and vertical edges form the opposite corner radius.
    const radius = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y));
    root.style.setProperty('--theme-origin-x', `${x}px`);
    root.style.setProperty('--theme-origin-y', `${y}px`);
    root.style.setProperty('--theme-radius', `${radius}px`);
  };

  const switchTheme = () => {
    if (transitionRunning) return;
    const nextTheme = root.dataset.theme === 'dark' ? 'light' : 'dark';
    if (reduceMotion.matches || typeof document.startViewTransition !== 'function') {
      applyTheme(nextTheme);
      return;
    }

    transitionRunning = true;
    root.dataset.themeTransition = 'random-spread';
    setRandomSpreadGeometry();

    const cleanup = () => {
      transitionRunning = false;
      delete root.dataset.themeTransition;
    };

    try {
      const transition = document.startViewTransition(() => applyTheme(nextTheme));
      transition.finished.then(cleanup, cleanup);
    } catch (_) {
      // A browser may expose the API while temporarily refusing a transition.
      applyTheme(nextTheme);
      cleanup();
    }
  };

  const initialize = () => {
    setToggleState(root.dataset.theme || 'light');
    document.querySelectorAll('[data-theme-toggle]').forEach(button => {
      if (button.dataset.themeReady === 'true') return;
      button.dataset.themeReady = 'true';
      button.addEventListener('click', switchTheme);
    });
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initialize, { once: true });
  } else {
    initialize();
  }
})();

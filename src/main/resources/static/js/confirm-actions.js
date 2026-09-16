(() => {
  document.addEventListener('submit', event => {
    const form = event.target.closest('form[data-confirm], form[data-confirm-disabled-field]');
    if (!form) return;

    let message = form.dataset.confirm;
    if (form.dataset.confirmDisabledField) {
      const field = form.elements.namedItem(form.dataset.confirmDisabledField);
      message = field && !field.checked ? form.dataset.confirmDisabledMessage : null;
    }
    if (!message) return;
    if (!window.confirm(message)) {
      event.preventDefault();
      return;
    }

    form.querySelectorAll('button[type="submit"], input[type="submit"]')
      .forEach(control => { control.disabled = true; });
  });
})();

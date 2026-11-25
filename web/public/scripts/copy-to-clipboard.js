document.addEventListener('click', async (event) => {
  if (!(event.target instanceof Element)) return;
  const button = event.target.closest('[data-copy-value]');
  if (!button) return;
  const status = button.nextElementSibling;
  const value = button.dataset.copyValue;
  if (!value || !(status instanceof HTMLElement)) return;
  try {
    await navigator.clipboard.writeText(value);
    status.textContent = 'Copied';
  } catch {
    status.textContent = 'Copy failed. Select and copy the text manually.';
  }
  window.setTimeout(() => {
    status.textContent = '';
  }, 2500);
});

function showPage(pageId){
  switch(pageId) {
    case 'pg-home':
      window.location.href = '/';
      break;
    case 'pg-dashboard':
      window.location.href = '/driver/dashboard';
      break;
    case 'pg-slotMap':
      window.location.href = '/slot-map';
      break;
    case 'pg-bookings':
      window.location.href = '/reservation';
      break;
    case 'pg-vehicles':
      window.location.href = '/driver/vehicles';
      break;
    case 'pg-profile':
      window.location.href = '/driver/profile';
      break;
    case 'pg-feedback':
      window.location.href = '/feedback';
      break;
    case 'pg-billing':
      window.location.href = '/driver/billing';
      break;
    default:
      console.warn('Unknown page:', pageId);
  }
}



function toggleSidebar(page) {
  if (!page) return;
  const sidebar = page.querySelector('.sidebar');
  if (!sidebar) return;
  sidebar.classList.toggle('hidden');
}

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.topbar').forEach(topbar => {
    if (topbar.querySelector('.topbar-toggle')) return;
    const toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.className = 'btn btn-ghost topbar-toggle';
    toggle.setAttribute('aria-label', 'Toggle navigation');
    toggle.textContent = '☰';
    toggle.addEventListener('click', () => {
      const page = topbar.closest('.page');
      toggleSidebar(page);
    });
    topbar.insertBefore(toggle, topbar.firstChild);
  });
});

async function logout() {
  try {
    const response = await fetch('/logout', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
    const data = await response.json().catch(() => ({}));
    sessionStorage.clear();
    window.location.href = data.redirect || '/login';
  } catch {
    sessionStorage.clear();
    window.location.href = '/login';
  }
}
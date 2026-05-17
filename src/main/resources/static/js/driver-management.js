function showPage(pageId) {
  switch (pageId) {
    case 'pg-admin-dash':
      window.location.href = '/admin/dashboard';
      break;
    case 'pg-drivers':
      window.location.href = '/admin/drivers';
      break;
    case 'pg-vehicles':
      window.location.href = '/admin/vehicles';
      break;
    case 'pg-slots':
      window.location.href = '/admin/slots';
      break;
    case 'pg-admins':
      window.location.href = '/admin/admins';
      break;
    case 'pg-reservations':
      window.location.href = '/admin/reservations';
      break;
    case 'pg-feedback':
      window.location.href = '/admin/feedback';
      break;
    default:
      console.warn('Unknown page:', pageId);
  }
}

let allDrivers = [];
let searchQuery = '';
let statusFilter = 'all';

async function deleteDriverById(id, displayName) {
  if (!id) return;
  const vehicleCount =
    allDrivers.find((d) => d.id === id)?.vehicleCount ?? 0;
  let msg = `Remove driver "${displayName}" (${id})? This cannot be undone.`;
  if (vehicleCount > 0) {
    msg += ` ${vehicleCount} registered vehicle(s) will also be removed.`;
  }
  if (!confirm(msg)) return;

  try {
    const res = await fetch('/admin/drivers/delete/' + encodeURIComponent(id), {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: '',
    });
    const data = await res.json().catch(() => ({}));
    if (data.success) {
      await loadDrivers();
    } else {
      alert(data.message || 'Delete failed.');
    }
  } catch (e) {
    console.error(e);
    alert('Could not delete driver.');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const searchEl = document.getElementById('driverSearch');
  const statusEl = document.getElementById('driverStatusFilter');
  if (searchEl) {
    searchEl.addEventListener('input', () => {
      searchQuery = searchEl.value.trim().toLowerCase();
      renderDriverTable();
    });
  }
  if (statusEl) {
    statusEl.addEventListener('change', () => {
      statusFilter = statusEl.value;
      renderDriverTable();
    });
  }

  const tbody = document.getElementById('driversTableBody');
  if (tbody) {
    tbody.addEventListener('click', (ev) => {
      const btn = ev.target.closest('.drv-delete');
      if (!btn) return;
      const id = btn.getAttribute('data-drv-id');
      const name = btn.getAttribute('data-drv-name') || '';
      deleteDriverById(id, name);
    });
  }

  loadDrivers();
});

function escapeHtml(s) {
  if (s == null) return '';
  const d = document.createElement('d' + 'iv');
  d.textContent = s;
  return d.innerHTML;
}

function formatStat(n) {
  return typeof n === 'number' ? n.toLocaleString() : '0';
}

function statusBadge(status) {
  if (status === 'SUSPENDED') {
    return '<span class="badge badge-warning">Suspended</span>';
  }
  return '<span class="badge badge-success">Active</span>';
}

function renderStats(stats) {
  const total = document.getElementById('statTotalDrivers');
  const active = document.getElementById('statActiveDrivers');
  const suspended = document.getElementById('statSuspendedDrivers');
  const newWeek = document.getElementById('statNewThisWeek');
  const activeSub = document.getElementById('statActiveSub');

  if (total) total.textContent = formatStat(stats.total);
  if (active) active.textContent = formatStat(stats.active);
  if (suspended) suspended.textContent = formatStat(stats.suspended);
  if (newWeek) newWeek.textContent = formatStat(stats.newThisWeek);
  if (activeSub) {
    activeSub.textContent =
      stats.total > 0 ? stats.activeRatePercent + ' active rate' : 'No drivers yet';
  }
}

function filteredDrivers() {
  return allDrivers.filter((d) => {
    if (statusFilter !== 'all' && d.status !== statusFilter) return false;
    if (!searchQuery) return true;
    const hay = [d.id, d.fullName, d.userName, d.email, d.phone]
      .join(' ')
      .toLowerCase();
    return hay.includes(searchQuery);
  });
}

function renderDriverTable() {
  const tbody = document.getElementById('driversTableBody');
  if (!tbody) return;

  const rows = filteredDrivers();
  if (!rows.length) {
    tbody.innerHTML =
      '<tr><td colspan="9" style="text-align:center;color:var(--ink-muted);padding:24px;">' +
      (allDrivers.length
        ? 'No drivers match your filters.'
        : 'No registered drivers yet.') +
      '</td></tr>';
    return;
  }

  tbody.innerHTML = rows
    .map((d) => {
      const idEsc = escapeHtml(d.id);
      const nameEsc = escapeHtml(d.fullName || '');
      return (
        '<tr>' +
        `<td><span style="font-family:var(--font-display);font-weight:700;color:var(--teal);">${idEsc}</span></td>` +
        `<td><span style="font-weight:600;">${nameEsc}</span></td>` +
        `<td>${escapeHtml(d.userName)}</td>` +
        `<td>${escapeHtml(d.email)}</td>` +
        `<td>${escapeHtml(d.phone)}</td>` +
        `<td>${escapeHtml(String(d.vehicleCount ?? 0))}</td>` +
        `<td>${escapeHtml(d.joined || '—')}</td>` +
        `<td>${statusBadge(d.status)}</td>` +
        '<td><' + 'div class="td-actions">' +
        `<button type="button" class="btn btn-outline btn-sm" disabled title="Coming soon">Edit</button>` +
        `<button type="button" class="btn btn-danger btn-sm drv-delete" data-drv-id="${idEsc}" data-drv-name="${nameEsc}">Delete</button>` +
        '</div></td>' +
        '</tr>'
      );
    })
    .join('');
}

async function loadDrivers() {
  const tbody = document.getElementById('driversTableBody');
  try {
    const res = await fetch('/admin/drivers/data', { credentials: 'same-origin' });
    if (res.status === 403) {
      if (tbody) {
        tbody.innerHTML =
          '<tr><td colspan="9" style="text-align:center;color:var(--danger);padding:24px;">Admin session required. Log in as an administrator.</td></tr>';
      }
      return;
    }
    if (!res.ok) throw new Error('Failed to load drivers');
    const data = await res.json();
    allDrivers = Array.isArray(data.drivers) ? data.drivers : [];
    if (data.stats) renderStats(data.stats);
    renderDriverTable();
  } catch (e) {
    console.error(e);
    if (tbody) {
      tbody.innerHTML =
        '<tr><td colspan="9" style="text-align:center;color:var(--danger);padding:24px;">Could not load drivers.</td></tr>';
    }
  }
}

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
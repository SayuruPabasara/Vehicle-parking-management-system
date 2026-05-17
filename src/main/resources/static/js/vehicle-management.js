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

let allVehicles = [];
let searchQuery = '';
let typeFilter = 'all';
let statusFilter = 'all';

const COLOR_EMOJI = {
  black: '⬛',
  white: '⬜',
  red: '🔴',
  blue: '🔵',
  green: '🟢',
  gray: '⚪',
  grey: '⚪',
  silver: '⚪',
  yellow: '🟡',
  orange: '🟠',
};

function escapeHtml(s) {
  if (s == null) return '';
  const d = document.createElement('d' + 'iv');
  d.textContent = s;
  return d.innerHTML;
}

function formatStat(n) {
  return typeof n === 'number' ? n.toLocaleString() : '0';
}

function colorDisplay(color) {
  if (!color) return '—';
  const key = String(color).trim().toLowerCase();
  const emoji = COLOR_EMOJI[key] || '●';
  return emoji + ' ' + color;
}

function statusBadge(status) {
  if (status === 'FLAGGED') {
    return '<span class="badge badge-warning">Flagged</span>';
  }
  return '<span class="badge badge-success">Active</span>';
}

function renderStats(stats) {
  const total = document.getElementById('statTotalVehicles');
  const active = document.getElementById('statActiveVehicles');
  const flagged = document.getElementById('statFlaggedVehicles');
  const typeCount = document.getElementById('statTypeCount');
  const typeLabels = document.getElementById('statTypeLabels');

  if (total) total.textContent = formatStat(stats.total);
  if (active) active.textContent = formatStat(stats.active);
  if (flagged) flagged.textContent = formatStat(stats.flagged);
  if (typeCount) typeCount.textContent = formatStat(stats.typeCount);
  if (typeLabels) typeLabels.textContent = stats.typeLabels || '—';
}

function populateTypeFilter(types) {
  const select = document.getElementById('vehicleTypeFilter');
  if (!select) return;

  const current = select.value;
  select.innerHTML = '<option value="all">All Types</option>';
  types.forEach((t) => {
    const opt = document.createElement('option');
    opt.value = t;
    opt.textContent = t;
    select.appendChild(opt);
  });
  if ([...select.options].some((o) => o.value === current)) {
    select.value = current;
  }
}

function filteredVehicles() {
  return allVehicles.filter((v) => {
    if (statusFilter !== 'all' && v.status !== statusFilter) return false;
    if (typeFilter !== 'all' && v.type !== typeFilter) return false;
    if (!searchQuery) return true;
    const hay = [v.id, v.plateNumber, v.type, v.color, v.ownerName, v.ownerId]
      .join(' ')
      .toLowerCase();
    return hay.includes(searchQuery);
  });
}

function renderVehicleTable() {
  const tbody = document.getElementById('vehiclesTableBody');
  if (!tbody) return;

  const rows = filteredVehicles();
  if (!rows.length) {
    tbody.innerHTML =
      '<tr><td colspan="9" style="text-align:center;color:var(--ink-muted);padding:24px;">' +
      (allVehicles.length
        ? 'No vehicles match your filters.'
        : 'No registered vehicles yet.') +
      '</td></tr>';
    return;
  }

  tbody.innerHTML = rows
    .map((v) => {
      const idEsc = escapeHtml(v.id);
      const plateEsc = escapeHtml(v.plateNumber || '');
      return (
        '<tr>' +
        `<td><span style="font-family:var(--font-display);font-weight:700;color:var(--teal);">${idEsc}</span></td>` +
        `<td><span style="font-family:var(--font-display);font-weight:700;">${plateEsc}</span></td>` +
        `<td>${escapeHtml(v.type)}</td>` +
        `<td>${escapeHtml(colorDisplay(v.color))}</td>` +
        `<td>${escapeHtml(v.ownerName)}</td>` +
        `<td>${escapeHtml(v.ownerId)}</td>` +
        `<td>${escapeHtml(v.registered)}</td>` +
        `<td>${statusBadge(v.status)}</td>` +
        '<td><' + 'div class="td-actions">' +
        `<button type="button" class="btn btn-outline btn-sm" disabled title="Coming soon">Edit</button>` +
        `<button type="button" class="btn btn-danger btn-sm veh-delete" data-veh-id="${idEsc}" data-veh-plate="${plateEsc}">Delete</button>` +
        '</div></td>' +
        '</tr>'
      );
    })
    .join('');
}

async function deleteVehicleById(id, plate) {
  if (!id) return;
  const label = plate || id;
  if (!confirm(`Remove vehicle "${label}" (${id})? This cannot be undone.`)) return;

  try {
    const res = await fetch('/admin/vehicles/delete/' + encodeURIComponent(id), {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: '',
    });
    const data = await res.json().catch(() => ({}));
    if (data.success) {
      await loadVehicles();
    } else {
      alert(data.message || 'Delete failed.');
    }
  } catch (e) {
    console.error(e);
    alert('Could not delete vehicle.');
  }
}

async function loadVehicles() {
  const tbody = document.getElementById('vehiclesTableBody');
  try {
    const res = await fetch('/admin/vehicles/data', { credentials: 'same-origin' });
    if (res.status === 403) {
      if (tbody) {
        tbody.innerHTML =
          '<tr><td colspan="9" style="text-align:center;color:var(--danger);padding:24px;">Admin session required. Log in as an administrator.</td></tr>';
      }
      return;
    }
    if (!res.ok) throw new Error('Failed to load vehicles');
    const data = await res.json();
    allVehicles = Array.isArray(data.vehicles) ? data.vehicles : [];
    if (data.stats) {
      renderStats(data.stats);
      const types = Array.isArray(data.stats.typeLabelsList)
        ? data.stats.typeLabelsList
        : (data.stats.typeLabels || '')
            .split(',')
            .map((s) => s.trim())
            .filter(Boolean);
      populateTypeFilter(types);
    }
    renderVehicleTable();
  } catch (e) {
    console.error(e);
    if (tbody) {
      tbody.innerHTML =
        '<tr><td colspan="9" style="text-align:center;color:var(--danger);padding:24px;">Could not load vehicles.</td></tr>';
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const searchEl = document.getElementById('vehicleSearch');
  const typeEl = document.getElementById('vehicleTypeFilter');
  const statusEl = document.getElementById('vehicleStatusFilter');

  if (searchEl) {
    searchEl.addEventListener('input', () => {
      searchQuery = searchEl.value.trim().toLowerCase();
      renderVehicleTable();
    });
  }
  if (typeEl) {
    typeEl.addEventListener('change', () => {
      typeFilter = typeEl.value;
      renderVehicleTable();
    });
  }
  if (statusEl) {
    statusEl.addEventListener('change', () => {
      statusFilter = statusEl.value;
      renderVehicleTable();
    });
  }

  const tbody = document.getElementById('vehiclesTableBody');
  if (tbody) {
    tbody.addEventListener('click', (ev) => {
      const btn = ev.target.closest('.veh-delete');
      if (!btn) return;
      const id = btn.getAttribute('data-veh-id');
      const plate = btn.getAttribute('data-veh-plate') || '';
      deleteVehicleById(id, plate);
    });
  }

  loadVehicles();
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
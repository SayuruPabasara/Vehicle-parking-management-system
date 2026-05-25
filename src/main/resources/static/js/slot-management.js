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

let allSlots = [];
let searchQuery = '';
let sectionFilter = 'all';
let statusFilter = 'all';
let typeFilter = 'all';

function toggleEditSlot() {
  const panel = document.getElementById('editSlotPanel');
  if (!panel) return;
  const hidden = panel.style.display === 'none' || panel.style.display === '';
  panel.style.display = hidden ? 'block' : 'none';
  const msg = document.getElementById('editSlotMsg');
  if (msg) {
    msg.style.display = 'none';
    msg.textContent = '';
  }
  if (hidden) {
    populateEditSlotSelect();
  }
}

function showEditSlotMsg(text, isError) {
  const msg = document.getElementById('editSlotMsg');
  if (!msg) return;
  msg.textContent = text;
  msg.className = 'form-msg ' + (isError ? 'error' : 'success');
  msg.style.display = 'block';
}

function populateEditSlotSelect() {
  const select = document.getElementById('editSlotSelect');
  if (!select) return;

  const current = select.value;
  select.innerHTML = '<option value="">— Choose a slot —</option>';
  const sorted = [...allSlots].sort((a, b) =>
    String(a.slotNumber || '').localeCompare(String(b.slotNumber || ''))
  );
  sorted.forEach((s) => {
    const opt = document.createElement('option');
    opt.value = s.id;
    const label = s.slotNumber + ' · ' + s.section + ' · ' + s.status;
    opt.textContent = label;
    select.appendChild(opt);
  });
  if ([...select.options].some((o) => o.value === current)) {
    select.value = current;
  }
  fillEditSlotForm();
}

function fillEditSlotForm() {
  const select = document.getElementById('editSlotSelect');
  if (!select || !select.value) {
    ['editSlotSection', 'editSlotVehicle'].forEach((id) => {
      const el = document.getElementById(id);
      if (el) el.value = '';
    });
    const statusEl = document.getElementById('editSlotStatus');
    if (statusEl) statusEl.value = 'AVAILABLE';
    return;
  }

  const slot = allSlots.find((s) => s.id === select.value);
  if (!slot) return;

  const sectionEl = document.getElementById('editSlotSection');
  const vehicleEl = document.getElementById('editSlotVehicle');
  const statusEl = document.getElementById('editSlotStatus');

  if (sectionEl) sectionEl.value = slot.section || '—';
  if (vehicleEl) vehicleEl.value = slot.currentVehicle || '—';
  if (statusEl) statusEl.value = slot.status || 'AVAILABLE';
}

function escapeHtml(s) {
  if (s == null) return '';
  const d = document.createElement('d' + 'iv');
  d.textContent = s;
  return d.innerHTML;
}

function formatStat(n) {
  return typeof n === 'number' ? n.toLocaleString() : '0';
}

function formatRate(rate) {
  const n = typeof rate === 'number' ? rate : parseFloat(rate);
  if (Number.isNaN(n)) return '—';
  return 'LKR ' + Math.round(n).toLocaleString();
}

function statusBadge(status) {
  if (status === 'OCCUPIED') {
    return '<span class="badge badge-danger">Occupied</span>';
  }
  if (status === 'MAINTENANCE') {
    return '<span class="badge badge-warning">Maintenance</span>';
  }
  return '<span class="badge badge-success">Available</span>';
}

function renderStats(stats) {
  const total = document.getElementById('statTotalSlots');
  const available = document.getElementById('statAvailableSlots');
  const occupied = document.getElementById('statOccupiedSlots');
  const maintenance = document.getElementById('statMaintenanceSlots');

  if (total) total.textContent = formatStat(stats.total);
  if (available) available.textContent = formatStat(stats.available);
  if (occupied) occupied.textContent = formatStat(stats.occupied);
  if (maintenance) maintenance.textContent = formatStat(stats.maintenance);
}

function populateSelect(selectId, values, allLabel) {
  const select = document.getElementById(selectId);
  if (!select) return;

  const current = select.value;
  select.innerHTML = `<option value="all">${allLabel}</option>`;
  values.forEach((v) => {
    const opt = document.createElement('option');
    opt.value = v;
    opt.textContent = v;
    select.appendChild(opt);
  });
  if ([...select.options].some((o) => o.value === current)) {
    select.value = current;
  }
}

function filteredSlots() {
  return allSlots.filter((s) => {
    if (sectionFilter !== 'all' && s.section !== sectionFilter) return false;
    if (statusFilter !== 'all' && s.status !== statusFilter) return false;
    if (!searchQuery) return true;
    const hay = [s.id, s.slotNumber, s.section, s.type, s.currentVehicle]
      .join(' ')
      .toLowerCase();
    return hay.includes(searchQuery);
  });
}

function renderSlotTable() {
  const tbody = document.getElementById('slotsTableBody');
  if (!tbody) return;

  const rows = filteredSlots();
  if (!rows.length) {
    tbody.innerHTML =
      '<tr><td colspan="9" style="text-align:center;color:var(--ink-muted);padding:24px;">' +
      (allSlots.length
        ? 'No slots match your filters.'
        : 'No parking slots configured yet.') +
      '</td></tr>';
    return;
  }

  tbody.innerHTML = rows
    .map((s) => {
      const slotEsc = escapeHtml(s.slotNumber || '');
      return (
        '<tr>' +
        `<td style="font-family:var(--font-display);font-weight:700;">${slotEsc}</td>` +
        `<td>${escapeHtml(s.section)}</td>` +
        `<td>${statusBadge(s.status)}</td>` +
        `<td>${escapeHtml(s.currentVehicle)}</td>` +
        `<td>${escapeHtml(s.lastUpdated)}</td>` +
        '</tr>'
      );
    })
    .join('');
}

async function loadSlots() {
  const tbody = document.getElementById('slotsTableBody');
  try {
    const res = await fetch('/admin/slots/data', { credentials: 'same-origin' });
    if (res.status === 403) {
      if (tbody) {
        tbody.innerHTML =
          '<tr><td colspan="9" style="text-align:center;color:var(--danger);padding:24px;">Admin session required. Log in as an administrator.</td></tr>';
      }
      return;
    }
    if (!res.ok) throw new Error('Failed to load slots');
    const data = await res.json();
    allSlots = Array.isArray(data.slots) ? data.slots : [];
    if (data.stats) renderStats(data.stats);
    if (data.filters) {
      const sections = Array.isArray(data.filters.sections)
        ? data.filters.sections
        : [];
      const types = Array.isArray(data.filters.types) ? data.filters.types : [];
      populateSelect('slotSectionFilter', sections, 'All Sections');
      populateSelect('slotTypeFilter', types, 'All Types');
    }
    populateEditSlotSelect();
    renderSlotTable();
  } catch (e) {
    console.error(e);
    if (tbody) {
      tbody.innerHTML =
        '<tr><td colspan="9" style="text-align:center;color:var(--danger);padding:24px;">Could not load slots.</td></tr>';
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const searchEl = document.getElementById('slotSearch');
  const sectionEl = document.getElementById('slotSectionFilter');
  const statusEl = document.getElementById('slotStatusFilter');
  const typeEl = document.getElementById('slotTypeFilter');

  if (searchEl) {
    searchEl.addEventListener('input', () => {
      searchQuery = searchEl.value.trim().toLowerCase();
      renderSlotTable();
    });
  }
  if (sectionEl) {
    sectionEl.addEventListener('change', () => {
      sectionFilter = sectionEl.value;
      renderSlotTable();
    });
  }
  if (statusEl) {
    statusEl.addEventListener('change', () => {
      statusFilter = statusEl.value;
      renderSlotTable();
    });
  }
  if (typeEl) {
    typeEl.addEventListener('change', () => {
      typeFilter = typeEl.value;
      renderSlotTable();
    });
  }

  const editSelect = document.getElementById('editSlotSelect');
  if (editSelect) {
    editSelect.addEventListener('change', fillEditSlotForm);
  }

  const editForm = document.getElementById('slotEditForm');
  if (editForm) {
    editForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const slotId = document.getElementById('editSlotSelect')?.value;
      const status = document.getElementById('editSlotStatus')?.value;

      if (!slotId) {
        showEditSlotMsg('Please select a slot to edit.', true);
        return;
      }
      if (!status) {
        showEditSlotMsg('Please select a status.', true);
        return;
      }

      const params = new URLSearchParams();
      params.set('slotId', slotId);
      params.set('status', status);

      const btn = document.getElementById('editSlotSubmitBtn');
      if (btn) btn.disabled = true;

      try {
        const res = await fetch('/admin/slots/update', {
          method: 'POST',
          credentials: 'same-origin',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: params.toString(),
        });
        const data = await res.json().catch(() => ({}));

        if (res.ok && data.success) {
          showEditSlotMsg(data.message || 'Slot updated.', false);
          await loadSlots();
          toggleEditSlot();
        } else {
          showEditSlotMsg(data.message || 'Could not update slot.', true);
        }
      } catch (err) {
        console.error(err);
        showEditSlotMsg('Network error — try again.', true);
      } finally {
        if (btn) btn.disabled = false;
      }
    });
  }

  loadSlots();
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
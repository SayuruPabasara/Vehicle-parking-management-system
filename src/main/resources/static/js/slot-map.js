function showPage(pageId) {
  switch (pageId) {
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

function nav(target) {
  if (target === 'pg-booking') {
    if (selectedSlotId) sessionStorage.setItem('prefSlotId', selectedSlotId);
    window.location.href = '/reservation';
    return;
  }
  if (target === 'pg-vehicles') {
    window.location.href = '/driver/vehicles';
    return;
  }
  showPage(target);
}

let selectedSlotId = null;

function escapeHtml(text) {
  const d = document.createElement('div');
  d.textContent = text == null ? '' : String(text);
  return d.innerHTML;
}

function statusLabel(status) {
  switch (status) {
    case 'AVAILABLE':
      return 'Available';
    case 'OCCUPIED':
      return 'Occupied';
    case 'MAINTENANCE':
      return 'Maintenance';
    default:
      return status || '—';
  }
}

function iconFor(status, selected) {
  if (selected) return '⭐';
  switch (status) {
    case 'AVAILABLE':
      return '🟢';
    case 'OCCUPIED':
      return '🔴';
    case 'MAINTENANCE':
      return '🔧';
    default:
      return '—';
  }
}

function baseSlotClass(status) {
  if (status === 'AVAILABLE') return 'slot av';
  if (status === 'OCCUPIED') return 'slot oc';
  return 'slot ds';
}

function sectionLetter(slotNumber) {
  if (!slotNumber || slotNumber.length < 1) return '?';
  const ch = slotNumber.charAt(0).toUpperCase();
  if (ch === 'A' || ch === 'B' || ch === 'C') return ch;
  return '—';
}

function renderSlotCell(slot) {
  const d = document.createElement('div');
  const id = slot.id;
  const status = slot.status;
  d.className = baseSlotClass(status);
  d.dataset.slotId = id;
  d.dataset.status = status;

  let title = `${slot.slotNumber} — ${statusLabel(status)}`;
  if (slot.occupant) title += ` · ${slot.occupant}`;
  d.title = title;

  d.innerHTML =
    `<div class="slot-id">${escapeHtml(slot.slotNumber)}</div>` +
    `<div class="slot-status">${escapeHtml(statusLabel(status))}</div>` +
    `<div class="slot-ico">${iconFor(status, false)}</div>`;

  if (status === 'AVAILABLE') {
    d.addEventListener('click', () => selectSlotCard(d, slot));
  }

  return d;
}

function selectSlotCard(el, slot) {
  document.querySelectorAll('.slot.sl').forEach((node) => {
    const st = node.dataset.status;
    node.className = baseSlotClass(st);
    const ico = node.querySelector('.slot-ico');
    if (ico) ico.textContent = iconFor(st, false);
  });

  selectedSlotId = slot.id;
  el.className = 'slot sl';
  const ico = el.querySelector('.slot-ico');
  if (ico) ico.textContent = iconFor(slot.status, true);

  const badge = document.getElementById('selectedSlotId');
  const sec = document.getElementById('selectedSlotSection');
  const meta = document.getElementById('selectedSlotMeta');
  const panel = document.getElementById('selectedPanel');
  const bookBtn = document.getElementById('bookSlotBtn');

  if (badge) badge.textContent = slot.slotNumber;
  if (sec) sec.textContent = 'Section ' + sectionLetter(slot.slotNumber);
  if (meta) {
    const rate = slot.hourlyRate != null ? Number(slot.hourlyRate) : 150;
    meta.textContent = 'LKR ' + rate + '/hr · ' + statusLabel(slot.status);
  }
  if (panel) panel.style.display = 'flex';
  if (bookBtn) bookBtn.style.display = 'inline-flex';
}

function clearSelection() {
  document.querySelectorAll('.slot.sl').forEach((node) => {
    const st = node.dataset.status;
    node.className = baseSlotClass(st);
    const ico = node.querySelector('.slot-ico');
    if (ico) ico.textContent = iconFor(st, false);
  });
  selectedSlotId = null;
  const panel = document.getElementById('selectedPanel');
  const bookBtn = document.getElementById('bookSlotBtn');
  if (panel) panel.style.display = 'none';
  if (bookBtn) bookBtn.style.display = 'none';
}

function updateSummaryBar(grouped) {
  let total = 0;
  let avail = 0;
  let occ = 0;
  const sections = ['Section A', 'Section B', 'Section C'];
  sections.forEach((k) => {
    const list = grouped[k] || [];
    list.forEach((s) => {
      total++;
      if (s.status === 'AVAILABLE') avail++;
      else if (s.status === 'OCCUPIED') occ++;
    });
  });

  const elTot = document.getElementById('mapTotal');
  const elAv = document.getElementById('mapAvailable');
  const elOc = document.getElementById('mapOccupied');
  const elFr = document.getElementById('mapFreeRate');
  if (elTot) elTot.textContent = String(total);
  if (elAv) elAv.textContent = String(avail);
  if (elOc) elOc.textContent = String(occ);
  if (elFr) {
    const pct = total > 0 ? ((avail / total) * 100).toFixed(1) : '0';
    elFr.textContent = pct + '%';
  }
}

function renderSection(containerId, slots) {
  const g = document.getElementById(containerId);
  if (!g) return;
  g.innerHTML = '';
  slots.forEach((slot) => g.appendChild(renderSlotCell(slot)));
}

async function loadParkingMap() {
  const updated = document.getElementById('mapUpdated');
  try {
    const res = await fetch('/api/slots/map');
    if (!res.ok) throw new Error('HTTP ' + res.status);

    const grouped = await res.json();
    renderSection('gridA', grouped['Section A'] || []);
    renderSection('gridB', grouped['Section B'] || []);
    renderSection('gridC', grouped['Section C'] || []);
    updateSummaryBar(grouped);

    if (updated) {
      updated.textContent = new Date().toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
      });
    }
  } catch (e) {
    console.error('Slot map load failed:', e);
    if (updated) updated.textContent = 'load failed';
    ['gridA', 'gridB', 'gridC'].forEach((id) => {
      const g = document.getElementById(id);
      if (g)
        g.innerHTML =
          '<div style="grid-column:1/-1;padding:12px;color:var(--danger);font-size:0.85rem;">Could not load slots from the server.</div>';
    });
  }
}

document.addEventListener('DOMContentLoaded', () => {
  loadParkingMap();
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
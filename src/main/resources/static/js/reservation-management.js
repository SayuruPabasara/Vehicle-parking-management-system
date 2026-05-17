function showPage(pageId) {
  switch (pageId) {
    case 'pg-home':
      window.location.href = '/';
      break;
    case 'pg-login':
      window.location.href = '/login';
      break;
    case 'pg-admin-dash':
      window.location.href = '/admin/dashboard';
      break;
    case 'pg-slotMap':
      window.location.href = '/slot-map';
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
      console.warn('navigate:', pageId);
  }
}

function closeDropdown() {}

let reservations = [];
let activeFilter = 'all';
let searchQuery = '';
let sortMode = 'newest';
let selectedRows = new Set();

document.addEventListener('DOMContentLoaded', () => {
  loadReservations();
});

async function loadReservations() {
  try {
    const res = await fetch('/admin/reservations/data', { credentials: 'same-origin' });
    if (res.status === 403) {
      reservations = [];
      showToast('🔒', 'Admin session required.');
      renderAll();
      return;
    }
    if (!res.ok) throw new Error('load failed');
    reservations = await res.json();
    renderAll();
  } catch (e) {
    console.error(e);
    reservations = [];
    showToast('⚠️', 'Could not load reservations.');
    renderAll();
  }
}

function durString(r) {
  const start = new Date(r.checkIn);
  const end = r.checkOut ? new Date(r.checkOut) : new Date();
  const hrs = (end - start) / 3600000;
  const h = Math.floor(hrs);
  const m = Math.round((hrs - h) * 60);
  return h + 'h ' + String(m).padStart(2, '0') + 'm';
}

function fmtDT(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return (
    d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) +
    ' ' +
    d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
  );
}

function renderAll() {
  updateStats();
  renderTable();
  updateBulkActions();
}

function updateStats() {
  const total = reservations.length;
  const active = reservations.filter((r) => r.uiStatus === 'active').length;
  const pending = reservations.filter((r) => r.uiStatus === 'payment_pending').length;
  const elT = document.getElementById('stat-total');
  const elA = document.getElementById('stat-active');
  const elP = document.getElementById('stat-pending');
  if (elT) elT.textContent = String(total);
  if (elA) elA.textContent = String(active);
  if (elP) elP.textContent = String(pending);
}

function getFiltered() {
  return reservations
    .filter((r) => {
      if (activeFilter === 'active' && r.uiStatus !== 'active') return false;
      if (activeFilter === 'completed' && r.status !== 'COMPLETED') return false;
      if (activeFilter === 'payment_pending' && r.uiStatus !== 'payment_pending') return false;
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const blob = [r.id, r.driverName, r.slot, r.plate, r.status, r.paymentStatus].join(' ').toLowerCase();
        if (!blob.includes(q)) return false;
      }
      return true;
    })
    .sort((a, b) => {
      const ta = new Date(a.checkIn).getTime();
      const tb = new Date(b.checkIn).getTime();
      if (sortMode === 'newest') return tb - ta;
      if (sortMode === 'oldest') return ta - tb;
      if (sortMode === 'longest') {
        const endA = a.checkOut ? new Date(a.checkOut) : new Date();
        const endB = b.checkOut ? new Date(b.checkOut) : new Date();
        return endB - new Date(b.checkIn) - (endA - new Date(a.checkIn));
      }
      return 0;
    });
}

function sessionBadge(r) {
  if (r.uiStatus === 'active')
    return '<span class="badge badge-teal"><span class="live-dot" style="width:5px;height:5px;background:var(--teal);border-radius:50%;display:inline-block;margin-right:2px;"></span>Active</span>';
  if (r.uiStatus === 'payment_pending')
    return '<span class="badge badge-orange">Completed · Unpaid</span>';
  return '<span class="badge badge-success">Completed</span>';
}

function paymentBadge(r) {
  if (r.paymentStatus === 'PAID') return '<span class="badge badge-success">Paid</span>';
  return '<span class="badge badge-orange">Unpaid</span>';
}

function escapeAttr(s) {
  return String(s).replace(/'/g, '&#39;');
}

function renderTable() {
  const list = getFiltered();
  const tbody = document.getElementById('reservations-tbody');
  const empty = document.getElementById('empty-state');
  const wrap = document.querySelector('.table-wrap');

  if (!tbody) return;

  if (!list.length) {
    tbody.innerHTML = '';
    if (empty) empty.style.display = 'block';
    if (wrap) wrap.style.display = 'none';
    return;
  }
  if (empty) empty.style.display = 'none';
  if (wrap) wrap.style.display = '';

  tbody.innerHTML = list
    .map((r) => {
      const isSelected = selectedRows.has(r.id);
      const dur =
        r.uiStatus === 'active'
          ? `<span data-live-dur data-live-res="${escapeAttr(r.id)}" style="font-weight:600;font-size:0.88rem;">${durString(r)}</span>`
          : `<span style="font-weight:600;font-size:0.88rem;">${durString(r)}</span>`;
      const fee = Number(r.fee || 0).toLocaleString(undefined, { maximumFractionDigits: 2 });
      const canSelectPay = r.paymentStatus === 'UNPAID' && Number(r.fee) > 0;
      return `<tr class="${isSelected ? 'selected-row' : ''}">
        <td><input type="checkbox" class="custom-checkbox row-cb" value="${escapeAttr(
          r.id
        )}" ${isSelected ? 'checked' : ''} onchange="toggleRow(this, '${escapeAttr(r.id)}')"></td>
        <td><span style="font-family:var(--font-display);font-size:0.82rem;font-weight:700;color:var(--dark);">${escapeHtml(
          r.id
        )}</span></td>
        <td><div style="font-weight:600;font-size:0.88rem;">${escapeHtml(r.driverName)}</div></td>
        <td><div style="font-weight:600;font-size:0.88rem;">${escapeHtml(r.plate)}</div></td>
        <td><div style="font-weight:700;font-size:0.9rem;">${escapeHtml(r.slot)}</div></td>
        <td style="font-size:0.83rem;">${fmtDT(r.checkIn)}</td>
        <td>${dur}</td>
        <td style="font-weight:600;">${fee}</td>
        <td>${sessionBadge(r)}</td>
        <td>${paymentBadge(r)}</td>
        <td><div class="td-actions">
          ${
            canSelectPay
              ? `<button type="button" class="btn btn-teal btn-sm" onclick="markSinglePaid('${escapeAttr(r.id)}')">Confirm paid</button>`
              : '<span style="font-size:0.75rem;color:var(--ink-muted);">—</span>'
          }
        </div></td>
      </tr>`;
    })
    .join('');

  const visibleIds = list.map((r) => r.id);
  const allVisibleSelected = visibleIds.length > 0 && visibleIds.every((id) => selectedRows.has(id));
  const ca = document.getElementById('check-all');
  if (ca) ca.checked = allVisibleSelected;
}

function escapeHtml(s) {
  const d = document.createElement('div');
  d.textContent = s == null ? '' : String(s);
  return d.innerHTML;
}

function toggleAll(cb) {
  const list = getFiltered();
  if (cb.checked) list.forEach((r) => selectedRows.add(r.id));
  else list.forEach((r) => selectedRows.delete(r.id));
  renderTable();
  updateBulkActions();
}

function toggleRow(cb, id) {
  if (cb.checked) selectedRows.add(id);
  else selectedRows.delete(id);
  const tr = cb.closest('tr');
  if (cb.checked) tr.classList.add('selected-row');
  else tr.classList.remove('selected-row');
  const list = getFiltered();
  const visibleIds = list.map((r) => r.id);
  const allVisibleSelected = visibleIds.length > 0 && visibleIds.every((i) => selectedRows.has(i));
  const ca = document.getElementById('check-all');
  if (ca) ca.checked = allVisibleSelected;
  updateBulkActions();
}

function updateBulkActions() {
  const ba = document.getElementById('bulk-actions');
  const count = document.getElementById('sel-count');
  if (count) count.textContent = selectedRows.size;
  if (ba) {
    if (selectedRows.size > 0) ba.classList.add('active');
    else ba.classList.remove('active');
  }
}

async function markSinglePaid(id) {
  selectedRows.clear();
  selectedRows.add(id);
  await bulkAction('pay');
  selectedRows.clear();
}

async function bulkAction(action) {
  if (selectedRows.size === 0) return;

  if (action === 'pay') {
    const ids = [...selectedRows].filter((id) => {
      const r = reservations.find((x) => x.id === id);
      return r && r.paymentStatus === 'UNPAID' && Number(r.fee) > 0;
    });
    if (!ids.length) {
      showToast('ℹ️', 'No unpaid reservations with a fee in the selection.');
      return;
    }
    const body = new URLSearchParams();
    ids.forEach((id) => body.append('reservationIds', id));
    try {
      const res = await fetch('/admin/reservations/confirm-payment', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body.toString(),
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok && data.success) {
        showToast('✅', data.message || 'Payment confirmed.');
        selectedRows.clear();
        await loadReservations();
      } else {
        showToast('❌', data.message || 'Could not confirm payment.');
      }
    } catch (e) {
      console.error(e);
      showToast('❌', 'Network error.');
    }
    return;
  }

  showToast('ℹ️', `Action “${action}” is not available from the server yet.`);
}

function filterBy(f, btn) {
  activeFilter = f;
  document.querySelectorAll('.status-tab').forEach((b) => b.classList.remove('active'));
  btn.classList.add('active');
  renderTable();
}

function onSearch(q) {
  searchQuery = q;
  renderTable();
}

function sortBy(v) {
  sortMode = v;
  renderTable();
}

function showToast(icon, msg) {
  const t = document.getElementById('toast');
  if (!t) {
    alert(msg);
    return;
  }
  const ic = document.getElementById('toast-icon');
  const m = document.getElementById('toast-msg');
  if (ic) ic.textContent = icon;
  if (m) m.textContent = msg;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 3200);
}

setInterval(() => {
  document.querySelectorAll('[data-live-dur]').forEach((el) => {
    const id = el.dataset.liveRes;
    const r = reservations.find((x) => x.id === id);
    if (r) el.textContent = durString(r);
  });
}, 30000);


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
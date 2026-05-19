function showPage(pageId){
    switch(pageId){
        case 'pg-admin-dash':
            window.location.href = "/admin/dashboard";
            break;
        case 'pg-slotMap':
            window.location.href = "/admin/slot-map";
            break;
        case 'pg-usage-stats':
            window.location.href = "/admin/usage-stats";
            break;
        case 'pg-revenue':
            window.location.href = "/admin/revenue";
            break;
        case 'pg-drivers':
            window.location.href = "/admin/drivers";
            break;
        case 'pg-vehicles':
            window.location.href = "/admin/vehicles";
            break;
        case 'pg-slots':
            window.location.href = "/admin/slots";
            break;
        case 'pg-admins':
            window.location.href = "/admin/admins";
            break;
        case 'pg-reservations':
            window.location.href = "/admin/reservations";
            break;
        case 'pg-feedback':
            window.location.href = "/admin/feedback";
            break;
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

let feedbackRows = [];
let searchQuery = '';

document.addEventListener('DOMContentLoaded', () => {
  loadFeedbackData();
});

async function loadFeedbackData() {
  try {
    const res = await fetch('/admin/feedback/data', { credentials: 'same-origin' });
    if (res.status === 403) {
      feedbackRows = [];
      renderFeedbackTable();
      return;
    }
    if (!res.ok) throw new Error('load failed');
    const data = await res.json();
    applyStats(data.stats || {});
    feedbackRows = data.rows || [];
    renderFeedbackTable();
  } catch (e) {
    console.error(e);
    feedbackRows = [];
    renderFeedbackTable();
  }
}

function applyStats(stats) {
  const avg = document.getElementById('stat-avg-rating');
  const total = document.getElementById('stat-total-reviews');
  const week = document.getElementById('stat-this-week');
  const weekSub = document.getElementById('stat-week-sub');
  const flagged = document.getElementById('stat-flagged');

  const rating = Number(stats.averageRating) || 0;
  if (avg) {
    avg.textContent = stats.totalReviews > 0 ? rating + ' ★' : '—';
  }
  if (total) total.textContent = String(stats.totalReviews ?? 0);
  if (week) week.textContent = String(stats.thisWeek ?? 0);
  if (weekSub) weekSub.textContent = stats.weekSub || '—';
  if (flagged) flagged.textContent = String(stats.flagged ?? 0);
}

function onFeedbackSearch(q) {
  searchQuery = (q || '').trim().toLowerCase();
  renderFeedbackTable();
}

function getFilteredRows() {
  const ratingFilter = document.getElementById('filter-rating')?.value || '';
  const categoryFilter = document.getElementById('filter-category')?.value || '';
  const statusFilter = document.getElementById('filter-status')?.value || '';

  return feedbackRows.filter((r) => {
    if (ratingFilter === 'low') {
      if (r.rating > 2) return false;
    } else if (ratingFilter) {
      if (String(r.rating) !== ratingFilter) return false;
    }
    if (categoryFilter && (r.category || '').toUpperCase() !== categoryFilter) return false;
    if (statusFilter && (r.status || '') !== statusFilter) return false;
    if (searchQuery) {
      const blob = [r.id, r.driverName, r.comment, r.categoryLabel, r.category]
        .join(' ')
        .toLowerCase();
      if (!blob.includes(searchQuery)) return false;
    }
    return true;
  });
}

function ratingColor(rating) {
  if (rating <= 2) return 'var(--danger)';
  if (rating <= 3) return '#f0a500';
  return '#f0a500';
}

function categoryBadgeClass(category) {
  const c = (category || '').toUpperCase();
  if (c === 'GENERAL' || c === 'STAFF' || c === 'CLEANLINESS' || c === 'SAFETY') return 'badge-teal';
  return 'badge-orange';
}

function statusBadge(r) {
  if (r.status === 'FLAGGED') {
    return '<span class="badge badge-warning">Flagged</span>';
  }
  return '<span class="badge badge-success">Published</span>';
}

function renderFeedbackTable() {
  const tbody = document.getElementById('feedback-tbody');
  if (!tbody) return;

  const list = getFilteredRows();
  if (!list.length) {
    tbody.innerHTML =
      '<tr><td colspan="8" style="text-align:center;padding:40px 20px;color:var(--ink-muted);">No feedback matches your filters.</td></tr>';
    return;
  }

  tbody.innerHTML = list
    .map((r) => {
      const idEsc = escapeAttr(r.id);
      const comment = escapeHtml(r.comment || '');
      const catClass = categoryBadgeClass(r.category);
      return (
        '<tr>' +
        `<td style="font-weight:700;color:var(--teal);">${escapeHtml(r.id)}</td>` +
        `<td>${escapeHtml(r.driverName)}</td>` +
        `<td style="color:${ratingColor(r.rating)};font-weight:700;">${escapeHtml(
          r.ratingDisplay
        )} ${r.rating}/5</td>` +
        `<td><span class="badge ${catClass}">${escapeHtml(r.categoryLabel)}</span></td>` +
        `<td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${comment}">${comment}</td>` +
        `<td>${escapeHtml(r.date)}</td>` +
        `<td>${statusBadge(r)}</td>` +
        '<td><div class="td-actions">' +
        `<button type="button" class="btn btn-danger btn-sm" onclick="deleteFeedback('${idEsc}')">Delete</button>` +
        '</div></td>' +
        '</tr>'
      );
    })
    .join('');
}

async function deleteFeedback(id) {
  if (!id) return;
  if (!confirm('Delete this feedback entry?')) return;
  try {
    const res = await fetch('/admin/feedback/delete/' + encodeURIComponent(id), {
      method: 'POST',
      credentials: 'same-origin',
    });
    const data = await res.json().catch(() => ({}));
    if (res.ok && data.success) {
      await loadFeedbackData();
    } else {
      alert(data.message || 'Could not delete feedback.');
    }
  } catch (e) {
    console.error(e);
    alert('Network error.');
  }
}

function escapeHtml(s) {
  const d = document.createElement('div');
  d.textContent = s == null ? '' : String(s);
  return d.innerHTML;
}

function escapeAttr(s) {
  return String(s).replace(/'/g, '&#39;').replace(/"/g, '&quot;');
}
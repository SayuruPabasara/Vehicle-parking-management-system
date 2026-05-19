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

/**
 * ParkNow — session logout (POST /logout, then redirect to login).
 */
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

function actionBadgeClass(action) {
  if (!action) return 'badge-gold';
  if (action.includes('LOGIN') || action.includes('REGISTERED')) return 'badge-teal';
  if (action.includes('FAILED') || action.includes('DELETED')) return 'badge-danger';
  if (action.includes('PAYMENT') || action.includes('BOOKING') || action.includes('SLOT')) return 'badge-orange';
  return 'badge-gold';
}

function roleBadgeClass(role) {
  if (role === 'ADMIN') return 'badge-gold';
  if (role === 'DRIVER') return 'badge-teal';
  return 'badge-dark';
}

function escapeHtml(s) {
  const d = document.createElement('div');
  d.textContent = s == null ? '' : String(s);
  return d.innerHTML;
}

async function loadRecentActivity() {
      const tbody = document.getElementById('dashboard-activity-tbody');
      if (!tbody) return;
      try {
        const res = await fetch('/admin/logs/data', { credentials: 'same-origin' });
    if (res.status === 403) {
      tbody.innerHTML =
        '<tr><td colspan="4" style="text-align:center;padding:24px;color:var(--ink-muted);">Admin session required.</td></tr>';
      return;
    }
    if (!res.ok) throw new Error('load failed');
    const logs = await res.json();
    if (!logs.length) {
      tbody.innerHTML =
        '<tr><td colspan="4" style="text-align:center;padding:24px;color:var(--ink-muted);">No activity recorded yet.</td></tr>';
      return;
    }
    tbody.innerHTML = logs
      .map((row) => {
        const action = row.action || '';
        const role = row.role || '';
        return (
          '<tr>' +
          `<td style="font-size:0.8rem;color:var(--ink-muted);white-space:nowrap;">${escapeHtml(row.timestamp)}</td>` +
          `<td><span style="font-weight:600;">${escapeHtml(row.userIdentity)}</span> ` +
          `<span class="badge ${roleBadgeClass(role)}" style="font-size:0.65rem;">${escapeHtml(role)}</span></td>` +
          `<td><span class="badge ${actionBadgeClass(action)}" style="font-size:0.75rem;">${escapeHtml(action)}</span></td>` +
          `<td style="font-size:0.85rem;color:var(--dark-mid);">${escapeHtml(row.details)}</td>` +
          '</tr>'
        );
      })
      .join('');
  } catch (e) {
    console.error(e);
    tbody.innerHTML =
      '<tr><td colspan="4" style="text-align:center;color:var(--danger);padding:24px;">Failed to load activity logs.</td></tr>';
  }
}

    document.addEventListener('DOMContentLoaded', loadRecentActivity);

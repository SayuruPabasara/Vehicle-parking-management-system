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

async function loadRecentActivity() {
      const tbody = document.getElementById('dashboard-activity-tbody');
      if (!tbody) return;
      try {
        const res = await fetch('/admin/logs/data');
        const logs = await res.json();
        if (!logs.length) {
          tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;padding:24px;color:var(--ink-muted);">No activity recorded yet.</td></tr>';
          return;
        }
        tbody.innerHTML = logs.map(line => {
          const parts = line.split(' | ');
          if (parts.length < 4) return '';
          const [time, id, role, action, details] = parts;
          return `<tr>
            <td style="font-size:0.8rem;color:var(--ink-muted);white-space:nowrap;">${time}</td>
            <td><span style="font-weight:600;">${id}</span> <span class="badge badge-dark" style="font-size:0.65rem;">${role}</span></td>
            <td><span class="badge ${action.includes('LOGIN') ? 'badge-teal' : 'badge-gold'}" style="font-size:0.75rem;">${action}</span></td>
            <td style="font-size:0.85rem;color:var(--dark-mid);">${details || '—'}</td>
          </tr>`;
        }).join('');
      } catch (e) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:var(--danger);padding:24px;">Failed to load activity logs.</td></tr>';
      }
    }
    document.addEventListener('DOMContentLoaded', loadRecentActivity);

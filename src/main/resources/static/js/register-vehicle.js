function showPage(pageId){
  switch(pageId) {
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

document.addEventListener('DOMContentLoaded', () => {
    loadMyVehicles();
});

async function loadMyVehicles() {
    const listContainer = document.getElementById('vehicleList');
    const countBadge = document.querySelector('.section-title .badge');

    try {
        const response = await fetch('/api/my-vehicles');
        if (!response.ok) throw new Error('Failed to fetch vehicles');

        const vehicles = await response.json();
        
        // Update count badge
        if (countBadge) countBadge.textContent = `${vehicles.length} vehicle${vehicles.length !== 1 ? 's' : ''}`;

        if (vehicles.length === 0) {
            listContainer.innerHTML = '<p style="color:var(--ink-muted); font-size:0.9rem;">No vehicles registered yet.</p>';
            return;
        }

        // Render vehicles
        listContainer.innerHTML = vehicles.map(v => `
            <div class="card" style="display:flex;align-items:center;gap:16px;">
                <div style="font-size:2.5rem;">${getIcon(v.type)}</div>
                <div style="flex:1;">
                    <div style="font-family:var(--font-display);font-size:1.1rem;font-weight:700;">${v.plateNumber}</div>
                    <div style="font-size:0.82rem;color:var(--ink-muted);margin-top:3px;">
                        ${v.type} · ${v.color}
                    </div>
                </div>
                <div style="display:flex;gap:8px;align-items:center;">
                    <span class="badge badge-success">Active</span>
                    <button class="btn btn-danger btn-sm" onclick="removeVehicle('${v.id}')">Remove</button>
                </div>
            </div>
        `).join('');

    } catch (err) {
        console.error(err);
        listContainer.innerHTML = '<p style="color:var(--danger);">Error loading vehicles.</p>';
    }
}

async function removeVehicle(id) {
    if (!confirm('Are you sure you want to remove this vehicle?')) return;

    try {
        const response = await fetch(`/api/my-vehicles/${id}`, {
            method: 'DELETE'
        });

        const data = await response.json();
        if (data.success) {
            if (typeof showToast === 'function') showToast('🗑️ Vehicle removed');
            loadMyVehicles();
        } else {
            alert(data.message || 'Error removing vehicle');
        }
    } catch (err) {
        console.error('Error:', err);
    }
}

function getIcon(type) {
    switch(type?.toLowerCase()) {
        case 'van': return '🚐';
        case 'moto': return '🏍️';
        case 'truck': return '🚚';
        default: return '🚗';
    }
}

async function registerVehicle() {
    const plate = document.getElementById('veh-plate').value;
    const vtype = document.getElementById('veh-type').value;
    const color = document.getElementById('veh-color').value;
    const msg = document.getElementById('reg-msg');

    const params = new URLSearchParams({ plate, vtype, color });

    const response = await fetch('/register-vehicle', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
    });

    const data = await response.json();
    if (data.success) {
        msg.textContent = data.message;
        msg.className = 'form-msg success';
        loadMyVehicles(); // Refresh the list
    } else {
        msg.textContent = data.message;
        msg.className = 'form-msg error';
    }
}

function selectVType(el, type) {
    document.querySelectorAll('.vtype-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');
    document.getElementById('veh-type').value = type;
}

function selectColor(el, color) {
    document.querySelectorAll('.cswatch').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');
    document.getElementById('veh-color').value = color;
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
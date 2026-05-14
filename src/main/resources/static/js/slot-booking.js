function showPage(pageId){
  switch(pageId) {
    case 'pg-driver-dash':
      window.location.href = '/driver/dashboard';
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

// ========== BOOKING LOGIC ==========
async function loadAvailableSlots() {
  const select = document.getElementById('bk-slot');
  if (!select) return;

  try {
    const response = await fetch('/slots/available', { credentials: 'same-origin' });
    if (response.status === 401) {
      select.innerHTML = '<option value="">Log in to see available slots</option>';
      return;
    }
    if (!response.ok) throw new Error('Failed to fetch slots');

    const data = await response.json();
    select.innerHTML = '';
    if (data.slots && data.slots.length > 0) {
      data.slots.forEach(slot => {
        const opt = document.createElement('option');
        opt.value = slot.id;
        opt.textContent = `${slot.slotNumber} — LKR ${slot.hourlyRate}/hr`;
        select.appendChild(opt);
      });
    } else {
      select.innerHTML = '<option value="">No slots available</option>';
    }
    calcFee();
  } catch (err) {
    console.error('Error loading slots:', err);
    select.innerHTML = '<option value="">Could not load slots</option>';
  }
}

async function loadMyVehicles() {
  const select = document.getElementById('bk-veh');
  if (!select) return;

  try {
    const response = await fetch('/api/my-vehicles', { credentials: 'same-origin' });
    if (response.status === 401) {
      select.innerHTML = '<option value="">Log in to choose a vehicle</option>';
      return;
    }
    if (!response.ok) throw new Error('Failed to fetch vehicles');

    const vehicles = await response.json();
    select.innerHTML = '';
    if (!Array.isArray(vehicles) || vehicles.length === 0) {
      select.innerHTML = '<option value="">No vehicles registered — add one under My Vehicles</option>';
      return;
    }
    vehicles.forEach(v => {
      const opt = document.createElement('option');
      opt.value = v.id;
      opt.textContent = `${v.plateNumber} — ${v.type} · ${v.color}`;
      select.appendChild(opt);
    });
    updateSummary();
  } catch (err) {
    console.error('Error loading vehicles:', err);
    select.innerHTML = '<option value="">Could not load vehicles</option>';
  }
}

function updateSummary() {
  const slotSelect = document.getElementById('bk-slot');
  const vehSelect = document.getElementById('bk-veh');

  if (slotSelect && slotSelect.options[slotSelect.selectedIndex]) {
    const slotText = slotSelect.options[slotSelect.selectedIndex].text;
    document.getElementById('s-slot').textContent = slotText.split(' — ')[0];
    
    // Extract rate from option text for display
    const rateMatch = slotText.match(/LKR (\d+)/);
    if (rateMatch) {
        document.getElementById('s-rate').textContent = `LKR ${rateMatch[1]} / hr`;
    }
  }

  if (vehSelect && vehSelect.options[vehSelect.selectedIndex]) {
    const vehText = vehSelect.options[vehSelect.selectedIndex].text;
    document.getElementById('s-veh').textContent = vehText.split(' — ')[0];
  }
}

function calcFee() {
  const s = document.getElementById('bk-start').value;
  const e = document.getElementById('bk-end').value;
  const slotSelect = document.getElementById('bk-slot');

  // Get dynamic rate from selected slot text, fallback to 150
  let hourlyRate = 150;
  if (slotSelect && slotSelect.options[slotSelect.selectedIndex]) {
    const match = slotSelect.options[slotSelect.selectedIndex].text.match(/LKR (\d+)/);
    if (match) hourlyRate = parseInt(match[1]);
  }

  if (s && e) {
    const diff = (new Date(e) - new Date(s)) / 3600000;
    if (diff > 0) {
      const lkr = Math.round(diff * hourlyRate);
      const fee = 'LKR ' + lkr.toLocaleString();
      document.getElementById('fee-display').textContent = fee;
      document.getElementById('fee-hours').textContent = `${diff.toFixed(1)} hours × LKR ${hourlyRate}/hr`;
      document.getElementById('s-total').textContent = fee;
    }
  }
  updateSummary();
}
function selectPay(type) {
  document.getElementById('pm-cash').classList.toggle('selected', type==='cash');
  document.getElementById('pm-card').classList.toggle('selected', type==='card');
  
  const cardFields = document.getElementById('card-fields');
  if (cardFields) cardFields.style.display = type==='card' ? 'block' : 'none';
  
  const payDisplay = document.getElementById('s-pay');
  if (payDisplay) payDisplay.textContent = type === 'card' ? '💳 Card' : '💵 Cash';
}

function resetBooking() {
  document.getElementById('bk-start').value = '';
  document.getElementById('bk-end').value = '';
  document.getElementById('fee-display').textContent = 'LKR 0';
  document.getElementById('fee-hours').textContent = 'Select times to calculate';
  document.getElementById('s-total').textContent = 'LKR 0';
  document.getElementById('s-start').textContent = '—';
  document.getElementById('s-end').textContent = '—';
  document.getElementById('s-dur').textContent = '—';
  selectPay('cash');
}

async function confirmBooking() {
  const slotId = document.getElementById('bk-slot').value;
  const vehicleId = document.getElementById('bk-veh').value;
  const startTime = document.getElementById('bk-start').value;
  const endTime = document.getElementById('bk-end').value;

  if (!slotId || !vehicleId || !startTime || !endTime) {
    alert("Please fill in all booking details (slot, vehicle, and times).");
    return;
  }

  try {
    const params = new URLSearchParams({ slotId, vehicleId, startTime, endTime });
    const response = await fetch('/booking/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      credentials: 'same-origin',
      body: params.toString()
    });

    const data = await response.json();
    if (data.success) {
      alert("✅ " + data.message + " (ID: " + data.reservationId + ")");
      window.location.href = "/driver/dashboard";
    } else {
      alert("❌ Booking failed: " + data.message);
    }
  } catch (error) {
    console.error("Error creating booking:", error);
    alert("An unexpected error occurred while processing your booking.");
  }
}

// Set default booking times
document.addEventListener('DOMContentLoaded', async () => {
  const now = new Date();
  const later = new Date(now.getTime() + 3*3600000);
  const fmt = d => d.toISOString().slice(0,16);
  const bs = document.getElementById('bk-start');
  const be = document.getElementById('bk-end');
  if (bs && be) { bs.value = fmt(now); be.value = fmt(later); }

  await Promise.all([loadAvailableSlots(), loadMyVehicles()]);

  const pref = sessionStorage.getItem('prefSlotId');
  const sel = document.getElementById('bk-slot');
  if (pref && sel && [...sel.options].some(o => o.value === pref)) {
    sel.value = pref;
    sessionStorage.removeItem('prefSlotId');
    calcFee();
    updateSummary();
  }
});
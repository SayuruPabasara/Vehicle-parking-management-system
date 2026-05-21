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

/** Format a Date for <input type="datetime-local"> in local timezone. */
function toLocalDatetimeLocalValue(d) {
  const pad = (n) => String(n).padStart(2, '0');
  return (
    d.getFullYear() +
    '-' +
    pad(d.getMonth() + 1) +
    '-' +
    pad(d.getDate()) +
    'T' +
    pad(d.getHours()) +
    ':' +
    pad(d.getMinutes())
  );
}

function setDefaultBookingTimes() {
  const now = new Date();
  const later = new Date(now.getTime() + 3 * 3600000);
  const bs = document.getElementById('bk-start');
  const be = document.getElementById('bk-end');
  if (bs) bs.value = toLocalDatetimeLocalValue(now);
  if (be) be.value = toLocalDatetimeLocalValue(later);
  calcFee();
}

function formatSummaryDateTime(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
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
      data.slots.forEach((slot) => {
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
      select.innerHTML =
        '<option value="">No vehicles registered — add one under My Vehicles</option>';
      return;
    }
    vehicles.forEach((v) => {
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
    const slotEl = document.getElementById('s-slot');
    if (slotEl) slotEl.textContent = slotText.split(' — ')[0];

    const rateMatch = slotText.match(/LKR (\d+)/);
    if (rateMatch) {
      const rateEl = document.getElementById('s-rate');
      if (rateEl) rateEl.textContent = `LKR ${rateMatch[1]} / hr`;
    }
  }

  if (vehSelect && vehSelect.options[vehSelect.selectedIndex]) {
    const vehText = vehSelect.options[vehSelect.selectedIndex].text;
    const vehEl = document.getElementById('s-veh');
    if (vehEl) vehEl.textContent = vehText.split(' — ')[0];
  }

  const startVal = document.getElementById('bk-start')?.value;
  const endVal = document.getElementById('bk-end')?.value;
  const sStart = document.getElementById('s-start');
  const sEnd = document.getElementById('s-end');
  const sDur = document.getElementById('s-dur');
  if (sStart) sStart.textContent = formatSummaryDateTime(startVal);
  if (sEnd) sEnd.textContent = formatSummaryDateTime(endVal);
  if (sDur && startVal && endVal) {
    const diff = (new Date(endVal) - new Date(startVal)) / 3600000;
    if (diff > 0) sDur.textContent = `${diff.toFixed(1)} hours`;
    else sDur.textContent = '—';
  }
}

function calcFee() {
  const s = document.getElementById('bk-start')?.value;
  const e = document.getElementById('bk-end')?.value;
  const slotSelect = document.getElementById('bk-slot');

  let hourlyRate = 150;
  if (slotSelect && slotSelect.options[slotSelect.selectedIndex]) {
    const match = slotSelect.options[slotSelect.selectedIndex].text.match(/LKR (\d+)/);
    if (match) hourlyRate = parseInt(match[1], 10);
  }

  const feeDisplay = document.getElementById('fee-display');
  const feeHours = document.getElementById('fee-hours');
  const sTotal = document.getElementById('s-total');

  if (s && e) {
    const diff = (new Date(e) - new Date(s)) / 3600000;
    if (diff > 0) {
      const lkr = Math.round(diff * hourlyRate);
      const fee = 'LKR ' + lkr.toLocaleString();
      if (feeDisplay) feeDisplay.textContent = fee;
      if (feeHours) feeHours.textContent = `${diff.toFixed(1)} hours × LKR ${hourlyRate}/hr`;
      if (sTotal) sTotal.textContent = fee;
    } else {
      if (feeDisplay) feeDisplay.textContent = 'LKR 0';
      if (feeHours) feeHours.textContent = 'End time must be after start time';
      if (sTotal) sTotal.textContent = 'LKR 0';
    }
  }
  updateSummary();
}

function selectPay(type) {
  const cash = document.getElementById('pm-cash');
  const card = document.getElementById('pm-card');
  if (cash) cash.classList.toggle('selected', type === 'cash');
  if (card) card.classList.toggle('selected', type === 'card');

  const cardFields = document.getElementById('card-fields');
  if (cardFields) cardFields.style.display = type === 'card' ? 'block' : 'none';

  const payDisplay = document.getElementById('s-pay');
  if (payDisplay) payDisplay.textContent = type === 'card' ? '💳 Card' : '💵 Cash';
}

function resetBooking() {
  setDefaultBookingTimes();
  const sTotal = document.getElementById('s-total');
  if (sTotal) sTotal.textContent = document.getElementById('fee-display')?.textContent || 'LKR 0';
  selectPay('cash');
  updateSummary();
}

async function confirmBooking() {
  const slotId = document.getElementById('bk-slot')?.value;
  const vehicleId = document.getElementById('bk-veh')?.value;
  const startTime = document.getElementById('bk-start')?.value;
  const endTime = document.getElementById('bk-end')?.value;

  if (!slotId || !vehicleId || !startTime || !endTime) {
    alert('Please fill in all booking details (slot, vehicle, and times).');
    return;
  }

  if (new Date(endTime) <= new Date(startTime)) {
    alert('End time must be after start time.');
    return;
  }

  try {
    const params = new URLSearchParams({ slotId, vehicleId, startTime, endTime });
    const response = await fetch('/booking/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      credentials: 'same-origin',
      body: params.toString(),
    });

    const data = await response.json();
    if (data.success) {
      alert('✅ ' + data.message + ' (ID: ' + data.reservationId + ')');
      window.location.href = '/driver/dashboard';
    } else {
      alert('❌ Booking failed: ' + data.message);
    }
  } catch (error) {
    console.error('Error creating booking:', error);
    alert('An unexpected error occurred while processing your booking.');
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  setDefaultBookingTimes();

  await Promise.all([loadAvailableSlots(), loadMyVehicles()]);

  const pref = sessionStorage.getItem('prefSlotId');
  const sel = document.getElementById('bk-slot');
  if (pref && sel && [...sel.options].some((o) => o.value === pref)) {
    sel.value = pref;
    sessionStorage.removeItem('prefSlotId');
    calcFee();
    updateSummary();
  }
});

async function logout() {
  try {
    const response = await fetch('/logout', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    });
    const data = await response.json().catch(() => ({}));
    sessionStorage.clear();
    window.location.href = data.redirect || '/login';
  } catch {
    sessionStorage.clear();
    window.location.href = '/login';
  }
}

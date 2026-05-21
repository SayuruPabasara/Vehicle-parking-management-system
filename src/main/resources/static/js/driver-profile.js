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

let profileSnapshot = null;

document.addEventListener('DOMContentLoaded', () => {
  loadDriverProfile();
});

async function loadDriverProfile() {
  try {
    const res = await fetch('/api/driver/profile', { credentials: 'same-origin' });
    if (res.status === 401 || res.status === 403) {
      window.location.href = '/login';
      return;
    }
    if (!res.ok) throw new Error('load failed');
    const data = await res.json();
    if (!data.success || !data.profile) throw new Error('no profile');
    applyProfile(data.profile);
  } catch (e) {
    console.error(e);
    showProfileMsg('Could not load profile. Please refresh the page.', true);
  }
}

function applyProfile(p) {
  const name = p.fullName || '';
  const user = p.userName || '';
  const email = p.email || '';
  const phone = p.phone || '';
  const initials = p.initials || '??';

  const pfName = document.getElementById('pf-name');
  const pfUser = document.getElementById('pf-user');
  const pfEmail = document.getElementById('pf-email');
  const pfPhone = document.getElementById('pf-phone');
  if (pfName) pfName.value = name;
  if (pfUser) pfUser.value = user;
  if (pfEmail) pfEmail.value = email;
  if (pfPhone) pfPhone.value = phone;

  const heroName = document.getElementById('profile-hero-name');
  const heroMeta = document.getElementById('profile-hero-meta');
  const avatarXl = document.getElementById('profile-avatar-xl');
  const sidebarAvatar = document.querySelector('.sidebar-avatar');
  const sidebarUname = document.querySelector('.sidebar-uname');

  if (heroName) heroName.textContent = name || '—';
  const metaParts = [];
  if (user) metaParts.push('@' + user);
  if (email) metaParts.push(email);
  if (phone) metaParts.push(phone);
  if (heroMeta) heroMeta.textContent = metaParts.length ? metaParts.join(' · ') : '—';
  if (avatarXl) avatarXl.textContent = initials;
  if (sidebarAvatar) sidebarAvatar.textContent = initials;
  if (sidebarUname) sidebarUname.textContent = user || name || 'Driver';

  const ratingPill = document.getElementById('profile-rating-pill');
  const bookingsPill = document.getElementById('profile-bookings-pill');
  if (ratingPill) {
    ratingPill.textContent = p.averageRating
      ? '⭐ ' + p.averageRating + ' Rating'
      : '⭐ No ratings yet';
  }
  if (bookingsPill) {
    const n = p.bookingCount ?? 0;
    bookingsPill.textContent = n + (n === 1 ? ' Booking' : ' Bookings');
  }

  profileSnapshot = { fullName: name, userName: user, email, phone };
}

function showProfileMsg(text, isError) {
  const msg = document.getElementById('profile-msg');
  if (!msg) return;
  msg.textContent = text;
  msg.className = 'form-msg ' + (isError ? 'error' : 'success');
  msg.style.display = 'block';
}

function toggleEdit() {
  const inputs = document.querySelectorAll('#profile-fields input');
  const actions = document.getElementById('profile-actions');
  const isDisabled = inputs[0].disabled;
  inputs.forEach((i) => (i.disabled = !isDisabled));
  actions.style.display = isDisabled ? 'flex' : 'none';
  const msg = document.getElementById('profile-msg');
  if (msg) msg.style.display = 'none';
  if (isDisabled) inputs[0].focus();
}

function cancelEdit() {
  if (profileSnapshot) {
    const pfName = document.getElementById('pf-name');
    const pfUser = document.getElementById('pf-user');
    const pfEmail = document.getElementById('pf-email');
    const pfPhone = document.getElementById('pf-phone');
    if (pfName) pfName.value = profileSnapshot.fullName;
    if (pfUser) pfUser.value = profileSnapshot.userName;
    if (pfEmail) pfEmail.value = profileSnapshot.email;
    if (pfPhone) pfPhone.value = profileSnapshot.phone;
  }
  document.querySelectorAll('#profile-fields input').forEach((i) => (i.disabled = true));
  document.getElementById('profile-actions').style.display = 'none';
  const msg = document.getElementById('profile-msg');
  if (msg) msg.style.display = 'none';
}

async function saveProfile() {
  const pfName = document.getElementById('pf-name');
  const pfUser = document.getElementById('pf-user');
  const pfEmail = document.getElementById('pf-email');
  const pfPhone = document.getElementById('pf-phone');
  const btn = document.getElementById('profile-save-btn');
  if (!pfName || !pfUser || !pfEmail) return;

  const fullName = pfName.value.trim();
  const userName = pfUser.value.trim();
  const email = pfEmail.value.trim();
  const phone = (pfPhone && pfPhone.value.trim()) || '';

  if (!fullName || !userName || !email) {
    showProfileMsg('Full name, username, and email are required.', true);
    return;
  }

  if (btn) btn.disabled = true;
  try {
    const body = new URLSearchParams({ fullName, userName, email, phone });
    const res = await fetch('/api/driver/profile', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
    });
    const data = await res.json().catch(() => ({}));
    if (res.ok && data.success) {
      document.querySelectorAll('#profile-fields input').forEach((i) => (i.disabled = true));
      document.getElementById('profile-actions').style.display = 'none';
      await loadDriverProfile();
      showProfileMsg(data.message || 'Profile updated successfully.', false);
    } else {
      showProfileMsg(data.message || 'Could not save profile.', true);
    }
  } catch (e) {
    console.error(e);
    showProfileMsg('Network error. Please try again.', true);
  } finally {
    if (btn) btn.disabled = false;
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

// ========== NAVIGATION ==========
function showPage(id) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  const t = document.getElementById(id);
  if (t) { t.classList.add('active'); window.scrollTo(0, 0); }
}

function goTo(id) { showPage(id); }

function handleLogin() {
  const u = (document.getElementById('login-user').value || '').toLowerCase();
  showPage(u.includes('admin') || u === 'a' ? 'pg-admin-dash' : 'pg-driver-dash');
}

// ========== HERO MINI SLOT MAP ==========
function buildHeroSlots() {
  const container = document.getElementById('heroSlots');
  if (!container) return;
  const occ = [2, 5, 8, 12, 16, 17];
  const sel = [10];
  container.innerHTML = '';
  for (let i = 1; i <= 20; i++) {
    const d = document.createElement('div');
    const isOcc = occ.includes(i);
    const isSel = sel.includes(i);
    d.className = 'hcf-slot ' + (isSel ? 'sl' : isOcc ? 'oc' : 'av');
    d.textContent = 'A' + String(i).padStart(2, '0');
    container.appendChild(d);
  }
}

window.addEventListener('load', () => {
  buildHeroSlots();
});

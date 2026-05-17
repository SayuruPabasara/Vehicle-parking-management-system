// ========== MODALS ==========
function openEditDriver() { openModal('editModal'); }
function openDeleteModal(type, name) {
  document.getElementById('deleteModalMsg').textContent =
    `Are you sure you want to delete the ${type} "${name}"? This action cannot be undone.`;
  openModal('deleteModal');
}
function openEditSlot(id) {
  document.getElementById('slotEditId').textContent = id;
  document.getElementById('seSlotId').value = id;
  openModal('slotEditModal');
}
function openModal(id) {
  document.getElementById(id).classList.add('open');
}
function closeModal(id) {
  document.getElementById(id).classList.remove('open');
}
document.querySelectorAll('.modal-overlay').forEach(m => {
  m.addEventListener('click', e => { if (e.target === m) m.classList.remove('open'); });
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
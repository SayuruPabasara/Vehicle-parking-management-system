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

// ========== STAR RATING ==========
let currentRating = 4;
const ratingLabels = ['','Terrible 😞','Poor 😕','Okay 😐','Good 😊','Excellent 🤩'];
function setRating(n) {
  currentRating = n;
  const stars = document.querySelectorAll('#stars .star-btn');
  stars.forEach((s,i) => s.classList.toggle('lit', i < n));
  document.getElementById('star-label').textContent = n + ' out of 5 — ' + ratingLabels[n];
}

// ========== FEEDBACK CAT ==========
function selectCat(el) {
  document.querySelectorAll('.rating-cat').forEach(c => c.classList.remove('selected'));
  el.classList.add('selected');
}

function submitFeedback() {
  showToast('💬 Feedback submitted — Thank you!');
}
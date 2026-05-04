/**
 * ParkNow - Main UI Logic
 * Purpose: Handles navigation routes and interactive elements for the home page.
 */

// 1. Navigation Controller logic
// Maps the "showPage" calls in your HTML to Spring Boot controller routes
function showPage(pageId) {
    switch (pageId) {
        case 'pg-home':
            window.location.href = '/'; 
            break;
        case 'pg-login':
            window.location.href = '/login';
            break;
        case 'pg-register':
            window.location.href = '/register';
            break;
        default:
            console.warn("Route not defined for:", pageId);
    }
}

// 2. Dropdown Management
// Prevents memory leaks and ensures UI consistency when clicking outside menus
function closeDropdown() {
    const activeDropdowns = document.querySelectorAll('.dropdown-menu.show');
    activeDropdowns.forEach(menu => {
        menu.classList.remove('show');
    });
}

// 3. Stats Counter Animation
// Makes the numbers (Capacity, Occupied, etc.) count up on page load
function initStatsAnimation() {
    const stats = document.querySelectorAll('.hero-stat-val, .hero-stat2-val');
    
    stats.forEach(stat => {
        const targetText = stat.innerText;
        // Extract numbers and handle decimals or 'K' suffixes
        const targetValue = parseFloat(targetText.replace(/[^0-9.]/g, ''));
        
        if (isNaN(targetValue)) return;

        let startValue = 0;
        const duration = 2000; // 2 seconds
        const startTime = performance.now();

        function update(currentTime) {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            const currentNumber = progress * targetValue;

            // Formatting the output based on the original content
            if (targetText.includes('.')) {
                stat.innerText = currentNumber.toFixed(1) + (targetText.includes('/') ? ' / 5' : '');
            } else {
                stat.innerText = Math.floor(currentNumber).toLocaleString() + (targetText.includes('K') ? 'K' : '');
            }

            if (progress < 1) {
                requestAnimationFrame(update);
            }
        }
        requestAnimationFrame(update);
    });
}

// 4. Lifecycle Initialization
document.addEventListener('DOMContentLoaded', () => {
    console.log("ParkNow Interface Loaded");
    
    // Initialize animations
    initStatsAnimation();

    // Prevent 'Get Started' button from bubbling if you add nested events
    const actionButtons = document.querySelectorAll('.btn');
    actionButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
        });
    });
});
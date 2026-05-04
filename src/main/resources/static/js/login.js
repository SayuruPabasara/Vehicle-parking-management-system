/**
 * ParkNow - Login Logic
 * Integration for Spring Boot / Spring Security
 */

/**
 * 1. Primary Login Handler
 * Validates input and triggers the authentication flow.
 */
function handleLogin() {
    const user = document.getElementById('login-user').value.trim();
    const pass = document.getElementById('login-pass').value.trim();

    // Basic Validation
    if (!user || !pass) {
        alert("Please enter both your username/email and password.");
        return;
    }

    console.log("Attempting login for:", user);

    /**
     * SPRING BOOT INTEGRATION:
     * If you are using standard Spring Security, you should remove 
     * 'onsubmit="return false;"' from your HTML and let the form POST 
     * naturally. 
     * * If you want to keep the JS control, use the manual submit below:
     */
    // const form = document.querySelector('.auth-form');
    // form.method = 'POST';
    // form.action = '/login'; // Default Spring Security endpoint
    // form.submit();

    // For demo/UI testing purposes:
    window.location.href = '/dashboard'; 
}

/**
 * 2. Navigation Logic
 * Maps the UI actions to your Spring Controller routes.
 */
function showPage(pageId) {
    switch (pageId) {
        case 'pg-register':
            window.location.href = '/register';
            break;
        case 'pg-home':
            window.location.href = '/';
            break;
        default:
            console.warn("Destination route not defined:", pageId);
    }
}

/**
 * 3. UI Helpers
 */
function closeDropdown() {
    // Closes any navigation menus if they were open
    const menus = document.querySelectorAll('.dropdown-menu');
    menus.forEach(m => m.classList.remove('show'));
}

/**
 * 4. Event Listeners
 */
document.addEventListener('DOMContentLoaded', () => {
    const loginInputs = document.querySelectorAll('.form-input');

    // Allow pressing "Enter" to trigger the login button
    loginInputs.forEach(input => {
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                handleLogin();
            }
        });
    });

    console.log("ParkNow Login Module Initialized");
});
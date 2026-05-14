//Navigation Logic. Maps the UI actions to your Spring Controller routes.
function showPage(pageId) {
    switch (pageId) {
        case 'pg-register':
            window.location.href = '/register';
            break;
        case 'pg-home':
            window.location.href = '/';
            break;
        case 'pg-driver-dash':
            window.location.href='/driver/dashboard';
            break;
        case 'pg-admin-dash':
            window.location.href='/admin/dashboard';
            break;
        default:
            console.warn("Destination route not defined:", pageId);
    }
}

// client-side validation -show a styled message
function showMsg(el, text, type) {
    el.textContent = text;
    el.className = 'form-msg ' + type;      // type is 'success', 'error', or 'info'
}


async function handleLogin() {
    const email = document.getElementById('login-user').value.trim();
    const password = document.getElementById('login-pass').value.trim();
     const messageElement = document.getElementById('login-msg');

    // Basic Validation
    if (!email || !password) {
        showMsg(messageElement, 'Please enter both email and password.', 'error');
        return;
    }

    const params = new URLSearchParams({email, password});

    try{

        // Send POST /login to UserController
        const response = await fetch('/login', { 
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, // Spring Boot's @RequestParam expects this content type by default
            body: params.toString()
        });

        const data = await response.json();
        
        if (response.ok) {
            showMsg(messageElement, 'Login successful. Redirecting to dashboard...', 'info');
            setTimeout(() => showPage(data.redirect), 1500);

        } else {
            showMsg(messageElement, 'Invalid username or password.', 'error');
        }


    }catch(err){
        showMsg(messageElement, 'Cannot reach server.', 'error');
    }
    
}




//UI Helpers
function closeDropdown() {
    // Closes any navigation menus if they were open
    const menus = document.querySelectorAll('.dropdown-menu');
    menus.forEach(m => m.classList.remove('show'));
}


 //Event Listeners
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
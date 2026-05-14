
//  Navigation 
function showPage(pageId) {
    switch (pageId) {
        case 'pg-login':
            window.location.href = '/login.html';
            break;
        case 'pg-home':
            window.location.href = '/index.html';
            break;
        default:
            console.warn('Unknown page:', pageId);
    }
}

function closeDropdown() {
    document.querySelectorAll('.dropdown-menu')
            .forEach(m => m.classList.remove('show'));
}

// client-side validation -show a styled message
function showMsg(el, text, type) {
    el.textContent = text;
    el.className = 'form-msg ' + type;      // type is 'success', 'error', or 'info'
}

//  Registration 
async function handleRegistration() {

    // Read HTML form input values using IDs we added to the HTML
    const fullName          = document.getElementById('reg-fullname').value.trim();
    const userName          = document.getElementById('reg-username').value.trim();
    const email         = document.getElementById('reg-email').value.trim();
    const phone          = document.getElementById('reg-phone').value.trim();
    const password      = document.getElementById('reg-password').value;
    const confirmPwd    = document.getElementById('reg-confirm').value;
    const messageElement = document.getElementById('reg-msg');

    // check for client-side errors before sending to server
    if (!fullName || !userName || !email || !phone || !password || !confirmPwd) {
        showMsg(messageElement, 'Please fill in all fields.', 'error');
        return;
    }
    if (password.length < 8) {
        showMsg(messageElement, 'Password must be at least 8 characters.', 'error');
        return;
    }
    if (password !== confirmPwd) {
        showMsg(messageElement, 'Passwords do not match.', 'error');
        return;
    }

    //Send POST /register to UserController
    // We use URLSearchParams to encode the form data as application/x-www-form-urlencoded, which is what Spring Boot expects for @RequestParam parameters.
    const params = new URLSearchParams({ fullName, userName, email, phone, password });
    

    try {

        showMsg(messageElement, 'Creating your account...', 'info');

        // Send the POST request to the server,
        const response = await fetch('/register', { 
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, // Spring Boot's @RequestParam expects this content type by default
            body: params.toString() // URLSearchParams encodes the data as a query string, e.g. fullName=John+Doe&email=john%40example.com
        });

        const data = await response.json();

        if (data.success) {
            // Registration worked — UserService saved a row to users.csv
            showMsg(messageElement, '✓ Account created! Redirecting to login...', 'success');
            setTimeout(() => showPage('pg-login'), 1500);
        } else {
            showMsg(messageElement, data.message, 'error');
        }

    } catch (err) {
        // Network error — server not running or unreachable
        showMsg(messageElement, 'Cannot reach server.', 'error');
    }
}



//  Input focus animations 
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.form-input').forEach(input => {
        input.addEventListener('focus', () =>
            input.parentElement.classList.add('is-focused'));
        input.addEventListener('blur',  () =>
            input.parentElement.classList.remove('is-focused'));
    });
});
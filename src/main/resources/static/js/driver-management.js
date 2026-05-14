function showPage(pageId){
    switch(pageId){
        case 'pg-admin-dash':
            window.location.href = "/admin/dashboard";
            break;
        case 'pg-slotMap':
            window.location.href = "/admin/slot-map";
            break;
        case 'pg-usage-stats':
            window.location.href = "/admin/usage-stats";
            break;
        case 'pg-revenue':
            window.location.href = "/admin/revenue";
            break;
        case 'pg-drivers':
            window.location.href = "/admin/drivers";
            break;
        case 'pg-vehicles':
            window.location.href = "/admin/vehicles";
            break;
        case 'pg-slots':
            window.location.href = "/admin/slots";
            break;
        case 'pg-admins':
            window.location.href = "/admin/admins";
            break;
        case 'pg-reservations':
            window.location.href = "/admin/reservations";
            break;
        case 'pg-feedback':
            window.location.href = "/admin/feedback";
            break;
    }
}
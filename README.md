![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/SayuruPabasara/Vehicle-parking-management-system)
![Maven Central Version](https://img.shields.io/maven-central/v/org.apache.maven.plugins/maven-compiler-plugin)


# Vehicle Parking Management System

A robust, Spring Boot-based solution designed to streamline vehicle parking operations, slot allocation, and administrative oversight. This system leverages custom data structures and a file-based persistence layer to provide a lightweight yet powerful management experience.



## 🚀 Features

### For Drivers
*   **Real-time Slot Map:** Visual representation of available, occupied, and maintenance slots.
*   **Automated Booking:** Smart slot allocation using a LIFO (Last-In, First-Out) algorithm.
*   **Vehicle Management:** Register and track multiple vehicles per profile.
*   **Billing & History:** View real-time billing and past reservation records.
*   **Feedback System:** Submit service feedback directly to the administration.

### For Administrators
*   **Comprehensive Dashboard:** High-level analytics including occupancy rates, monthly revenue, and active sessions.
*   **Slot Management:** Manually override slot statuses (Available/Maintenance) and update hourly rates.
*   **User Oversight:** Manage driver accounts and monitor administrative access levels.
*   **Activity Audit:** System-wide logging of all critical actions (logins, allocations, releases) in a CSV audit trail.

## 🛠️ Technical Architecture

### Core Technologies
*   **Framework:** Spring Boot 3.x
*   **Security:** Spring Security with BCrypt password hashing and custom CSRF protection.
*   **Persistence:** CSV-based flat-file database (no external SQL/NoSQL dependency required).
*   **Build Tool:** Maven (Wrapper included).

### Custom Data Structures & Algorithms
*   **LIFO Slot Allocation:** Utilizes a custom `SlotStack` implementation to manage available slots, ensuring the most recently released slots are prioritized for new arrivals.
*   **Efficient Sorting:** Implements `QuickSort` for consistent ordering of parking slots and search results across the UI.
*   **Thread Safety:** Synchronized service methods prevent race conditions during high-concurrency slot allocation.

## 📂 Project Structure

```text
src/main/java/com/example/vehicle_parking_management_system/
├── config/      # Security and Application configurations
├── model/       # Domain entities (User, Driver, Admin, ParkingSlot, Reservation)
├── repository/  # CSV-based Data Access Objects
├── service/     # Business logic layer
├── util/        # Custom data structures (SlotStack) and utilities (QuickSort, Logger)
└── web/         # Controllers for Web and API endpoints
```

## 🚦 Getting Started

### Prerequisites
*   JDK 17 or higher
*   Maven 3.6+

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/Vehicle-parking-management-system.git
   cd Vehicle-parking-management-system
   ```

2. **Configure Data Paths:**
   Update `src/main/resources/application.properties` to set your desired CSV storage locations:
   ```properties
   parknow.data.slots=data/slots.csv
   parknow.data.users=data/users.csv
   parknow.data.activity-log=data/activity.log
   ```

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Access the System:**
   Open your browser and navigate to `http://localhost:8080`

## 🔒 Security Configuration

The system is secured using Spring Security, featuring:
*   **Role-Based Access Control:** Differentiated access for `DRIVER` and `ADMIN` roles.
*   **Public Endpoints:** Home, Registration, and the Slot Map are accessible for guest browsing.
*   **Session Management:** Form-based authentication with secure logout.
*   **CSRF Protection:** Selectively ignored for H2 console and specific AJAX-heavy endpoints to facilitate seamless UI interactions.

## 📊 Data Persistence

The system uses a CSV repository pattern, making it highly portable.
*   `users.csv`: Stores hashed credentials and profile details.
*   `slots.csv`: Maintains real-time state of the parking lot.
*   `activity.log`: A synchronized audit log capturing `actorId`, `role`, `action`, and `timestamp`.

## 🤝 Contributing

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git checkout -b feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0 - see the mvnw.cmd headers for details.

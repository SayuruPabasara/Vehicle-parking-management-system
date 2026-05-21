package com.example.vehicle_parking_management_system.service;

import com.example.vehicle_parking_management_system.model.ParkingSlot;
import com.example.vehicle_parking_management_system.model.Reservation;
import com.example.vehicle_parking_management_system.model.Vehicle;
import com.example.vehicle_parking_management_system.repository.ReservationRepository;
import com.example.vehicle_parking_management_system.repository.SlotRepository;
import com.example.vehicle_parking_management_system.repository.VehicleRepository;
import com.example.vehicle_parking_management_system.util.ActivityLogger;
import com.example.vehicle_parking_management_system.util.QuickSort;
import com.example.vehicle_parking_management_system.util.SlotStack;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class SlotService {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final SlotRepository slotRepository;
    private final VehicleRepository vehicleRepository;
    private final ReservationRepository reservationRepository;
    private final ActivityLogger activityLogger;

    @Value("${parknow.data.activity-log:default_activity.log}")
    private String activityLogPath;

    private final SlotStack availableStack = new SlotStack();

    public SlotService(SlotRepository slotRepository,
                       VehicleRepository vehicleRepository,
                       ReservationRepository reservationRepository,
                       ActivityLogger activityLogger) {
        this.slotRepository = slotRepository;
        this.vehicleRepository = vehicleRepository;
        this.reservationRepository = reservationRepository;
        this.activityLogger = activityLogger;
    }


    @PostConstruct
    public void initStack() {
        availableStack.clear();
        List<ParkingSlot> available = slotRepository.findByStatus(ParkingSlot.SlotStatus.AVAILABLE);

        available.sort(Comparator.comparing(ParkingSlot::getSlotNumber).reversed());
        for (ParkingSlot slot : available) {
            availableStack.push(slot);
        }
        System.out.println("[SlotService] Stack initialised with "
                + availableStack.size() + " available slots.");
    }



    public List<ParkingSlot> getAllSlots() {
        List<ParkingSlot> all = slotRepository.findAll();

        QuickSort.sort(all);
        return all;
    }


    public List<ParkingSlot> getAvailableSlots() {
        List<ParkingSlot> available = slotRepository.findByStatus(ParkingSlot.SlotStatus.AVAILABLE);
        QuickSort.sort(available);
        return available;
    }

    public Optional<ParkingSlot> findById(String slotId) {
        return slotRepository.findById(slotId);
    }

    public int getAvailableCount() {
        return availableStack.size();
    }




    public synchronized ParkingSlot allocateSlot(String slotId,
                                                  String vehicleId,
                                                  String actorId) {
        ParkingSlot slot;


        if (slotId == null || slotId.isBlank() || "auto".equalsIgnoreCase(slotId)) {
            slot = availableStack.pop();
            if (slot == null) throw new IllegalStateException("No available parking slots.");
        } else {

            slot = slotRepository.findById(slotId)
                    .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));

            if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
                throw new IllegalStateException(
                        "Slot " + slot.getSlotNumber() + " is not available (status: "
                                + slot.getStatus() + ")");
            }
            availableStack.remove(slotId);
        }


        slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
        slot.setCurrentVehicleId(vehicleId);
        slotRepository.update(slot);


        boolean removed = availableStack.remove(slotId);
        if (!removed) {
            System.err.println("[SlotService] Warning: Slot " + slotId + " was not found in the availability stack.");
        }

        activityLogger.log(actorId, "DRIVER", "SLOT_ALLOCATED",
                "Slot: " + slot.getSlotNumber() + " | Vehicle: " + vehicleId);
        return slot;
    }


    public synchronized ParkingSlot releaseSlot(String slotId, String actorId) {
        ParkingSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));

        if (slot.getStatus() != ParkingSlot.SlotStatus.OCCUPIED) {
            throw new IllegalStateException(
                    "Slot " + slot.getSlotNumber() + " is not OCCUPIED — cannot release.");
        }

        slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
        slot.setCurrentVehicleId(null);
        slotRepository.update(slot);


        availableStack.push(slot);

        activityLogger.log(actorId, "DRIVER", "SLOT_RELEASED",
                "Slot: " + slot.getSlotNumber());
        return slot;
    }


    public boolean updateSlot(String slotId, ParkingSlot.SlotStatus newStatus,
                               double newRate, String adminId) {
        Optional<ParkingSlot> opt = slotRepository.findById(slotId);
        if (opt.isEmpty()) return false;

        ParkingSlot slot = opt.get();
        ParkingSlot.SlotStatus oldStatus = slot.getStatus();

        slot.setStatus(newStatus);
        slot.setHourlyRate(newRate);
        if (newStatus != ParkingSlot.SlotStatus.OCCUPIED) {
            slot.setCurrentVehicleId(null);
        }
        slotRepository.update(slot);


        if (oldStatus != ParkingSlot.SlotStatus.AVAILABLE
                && newStatus == ParkingSlot.SlotStatus.AVAILABLE) {
            availableStack.push(slot);
        } else if (oldStatus == ParkingSlot.SlotStatus.AVAILABLE
                && newStatus != ParkingSlot.SlotStatus.AVAILABLE) {
            availableStack.remove(slotId);
        }

        activityLogger.log(adminId, "ADMIN", "SLOT_UPDATED",
                "Slot: " + slot.getSlotNumber()
                        + " status→" + newStatus + " rate→" + newRate);
        return true;
    }


    public Map<String, Object> getSlotManagementData() {
        List<ParkingSlot> slots = getAllSlots();
        Map<String, String> plateByVehicleId = new HashMap<>();
        for (Vehicle v : vehicleRepository.findAll()) {
            plateByVehicleId.put(v.getId(), v.getPlateNumber());
        }

        Map<String, Reservation> activeReservationBySlotId = new HashMap<>();
        for (Reservation r : reservationRepository.findAllActive()) {
            activeReservationBySlotId.put(r.getSlotId(), r);
        }

        Map<String, LocalDateTime> lastUpdatedBySlotNumber = loadSlotLastUpdatedTimes();
        for (Reservation r : reservationRepository.findAllActive()) {
            ParkingSlot slot = slotRepository.findById(r.getSlotId()).orElse(null);
            if (slot == null || r.getStartTime() == null) continue;
            String sn = slot.getSlotNumber();
            lastUpdatedBySlotNumber.merge(sn, r.getStartTime(),
                    (a, b) -> a.isAfter(b) ? a : b);
        }

        int available = 0;
        int occupied = 0;
        int maintenance = 0;
        Set<String> sectionSet = new TreeSet<>();
        Set<String> typeSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<Map<String, Object>> rows = new ArrayList<>();

        for (ParkingSlot slot : slots) {
            ParkingSlot.SlotStatus status = slot.getStatus();
            if (status == ParkingSlot.SlotStatus.AVAILABLE) available++;
            else if (status == ParkingSlot.SlotStatus.OCCUPIED) occupied++;
            else if (status == ParkingSlot.SlotStatus.MAINTENANCE) maintenance++;

            String section = parseSection(slot.getSlotNumber());
            sectionSet.add(section);
            String slotType = deriveSlotType(slot.getHourlyRate());
            typeSet.add(slotType);

            String vehiclePlate = resolveCurrentVehiclePlate(
                    slot, plateByVehicleId, activeReservationBySlotId);
            LocalDateTime lastUpdated = lastUpdatedBySlotNumber.get(slot.getSlotNumber());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", slot.getId());
            row.put("slotNumber", slot.getSlotNumber());
            row.put("section", section);
            row.put("type", slotType);
            row.put("floor", "Ground");
            row.put("status", status.name());
            row.put("currentVehicle", vehiclePlate);
            row.put("hourlyRate", slot.getHourlyRate());
            row.put("lastUpdated", formatLastUpdated(lastUpdated));
            row.put("lastUpdatedIso", lastUpdated == null ? null : lastUpdated.toString());
            rows.add(row);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", slots.size());
        stats.put("available", available);
        stats.put("occupied", occupied);
        stats.put("maintenance", maintenance);

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("sections", new ArrayList<>(sectionSet));
        filters.put("types", new ArrayList<>(typeSet));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stats", stats);
        payload.put("filters", filters);
        payload.put("slots", rows);
        return payload;
    }

    private String resolveCurrentVehiclePlate(ParkingSlot slot,
                                              Map<String, String> plateByVehicleId,
                                              Map<String, Reservation> activeBySlotId) {
        String vehicleId = slot.getCurrentVehicleId();
        if (vehicleId == null || vehicleId.isBlank()) {
            Reservation active = activeBySlotId.get(slot.getId());
            if (active != null) vehicleId = active.getVehicleId();
        }
        if (vehicleId == null || vehicleId.isBlank()) return "—";
        return plateByVehicleId.getOrDefault(vehicleId, vehicleId);
    }

    private static String parseSection(String slotNumber) {
        if (slotNumber != null && slotNumber.length() >= 3 && slotNumber.charAt(1) == '-') {
            return String.valueOf(Character.toUpperCase(slotNumber.charAt(0)));
        }
        return "?";
    }

    private static String deriveSlotType(double hourlyRate) {
        if (hourlyRate >= 200) return "Large";
        if (hourlyRate <= 100) return "Disabled";
        return "Standard";
    }

    private String formatLastUpdated(LocalDateTime ts) {
        if (ts == null) return "—";
        if (ts.toLocalDate().equals(LocalDate.now())) {
            return ts.format(TIME_FMT);
        }
        return ts.format(DATE_FMT);
    }

    private Map<String, LocalDateTime> loadSlotLastUpdatedTimes() {
        Map<String, LocalDateTime> times = new HashMap<>();
        File logFile = new File(activityLogPath);
        if (!logFile.exists()) return times;

        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 5);
                if (parts.length < 5) continue;

                String action = parts[3].trim();
                if (!action.startsWith("SLOT_")) continue;

                LocalDateTime ts;
                try {
                    ts = LocalDateTime.parse(parts[0].trim());
                } catch (Exception e) {
                    continue;
                }

                String detail = parts[4].trim();
                if (detail.startsWith("\"") && detail.endsWith("\"")) {
                    detail = detail.substring(1, detail.length() - 1);
                }
                String prefix = "Slot: ";
                int idx = detail.indexOf(prefix);
                if (idx < 0) continue;

                String rest = detail.substring(idx + prefix.length());
                String slotNumber = rest.split("\\|")[0].trim();
                if (!slotNumber.isEmpty()) {
                    times.merge(slotNumber, ts, (a, b) -> a.isAfter(b) ? a : b);
                }
            }
        } catch (IOException e) {
            System.err.println("[SlotService] Failed to read activity log: " + e.getMessage());
        }
        return times;
    }
}

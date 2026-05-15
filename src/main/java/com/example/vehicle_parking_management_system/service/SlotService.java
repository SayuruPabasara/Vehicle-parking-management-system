package com.example.vehicle_parking_management_system.service;

import com.example.vehicle_parking_management_system.model.ParkingSlot;
import com.example.vehicle_parking_management_system.repository.SlotRepository;
import com.example.vehicle_parking_management_system.util.ActivityLogger;
import com.example.vehicle_parking_management_system.util.QuickSort;
import com.example.vehicle_parking_management_system.util.SlotStack;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * SlotService — business logic for Component 03 (Parking Slot Management).
 *
 * Data Structures used:
 *   - SlotStack: tracks available slots. pop() on allocate, push() on release.
 *   - QuickSort: sorts available slots before presenting the map view.
 *
 * Thread safety: allocateSlot / releaseSlot are synchronized to prevent
 * double-allocation under concurrent requests.
 */
@Service
public class SlotService {

    private final SlotRepository slotRepository;
    private final ActivityLogger activityLogger;

    /** In-memory stack of currently available slots. */
    private final SlotStack availableStack = new SlotStack();

    public SlotService(SlotRepository slotRepository, ActivityLogger activityLogger) {
        this.slotRepository = slotRepository;
        this.activityLogger = activityLogger;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Load all currently AVAILABLE slots into the stack on startup.
     * SlotRepository.seedIfEmpty() runs before this via @PostConstruct ordering.
     */
    @PostConstruct
    public void initStack() {
        availableStack.clear();
        List<ParkingSlot> available = slotRepository.findByStatus(ParkingSlot.SlotStatus.AVAILABLE);
        // Push in reverse sorted order so pop() returns the lowest-numbered slot first
        available.sort(Comparator.comparing(ParkingSlot::getSlotNumber).reversed());
        for (ParkingSlot slot : available) {
            availableStack.push(slot);
        }
        System.out.println("[SlotService] Stack initialised with "
                + availableStack.size() + " available slots.");
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /** Return all slots (any status) — used to render the full slot map. */
    public List<ParkingSlot> getAllSlots() {
        List<ParkingSlot> all = slotRepository.findAll();
        // Use QuickSort to ensure the map view is consistently ordered
        // regardless of the underlying CSV storage order.
        QuickSort.sort(all);
        return all;
    }

    /**
     * Return available slots sorted by slotNumber (QuickSort).
     * Used to populate the booking dropdown / map.
     */
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

    // ── Allocation (pop from stack) ────────────────────────────────────────────

    /**
     * Allocate a specific slot by ID.
     * Called by ReservationService.createBooking().
     *
     * @param slotId         the ID of the slot to allocate
     * @param vehicleId      the vehicle that will occupy it
     * @param actorId        driverId performing the action (for logging)
     * @throws IllegalStateException if the slot is not currently AVAILABLE
     */
    public synchronized ParkingSlot allocateSlot(String slotId,
                                                  String vehicleId,
                                                  String actorId) {
        ParkingSlot slot;

        // Requirement: When a booking is made, pop() the top slot.
        // If the frontend requests 'auto' allocation, we pop from the Stack.
        if (slotId == null || slotId.isBlank() || "auto".equalsIgnoreCase(slotId)) {
            slot = availableStack.pop();
            if (slot == null) throw new IllegalStateException("No available parking slots.");
        } else {
            // Otherwise, allocate specific selection and remove from stack to maintain LIFO sync
            slot = slotRepository.findById(slotId)
                    .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));

            if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
                throw new IllegalStateException(
                        "Slot " + slot.getSlotNumber() + " is not available (status: "
                                + slot.getStatus() + ")");
            }
            availableStack.remove(slotId);
        }

        // Update slot state
        slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
        slot.setCurrentVehicleId(vehicleId);
        slotRepository.update(slot);

        // In a strictly LIFO system where users don't pick IDs, we would use availableStack.pop().
        // Since the UI allows picking a specific slot, we remove that specific ID from the stack.
        boolean removed = availableStack.remove(slotId);
        if (!removed) {
            System.err.println("[SlotService] Warning: Slot " + slotId + " was not found in the availability stack.");
        }

        activityLogger.log(actorId, "DRIVER", "SLOT_ALLOCATED",
                "Slot: " + slot.getSlotNumber() + " | Vehicle: " + vehicleId);
        return slot;
    }

    /**
     * Release a slot back to available.
     * Called by ReservationService.checkOut().
     *
     * @param slotId   the slot to release
     * @param actorId  driverId performing the action (for logging)
     */
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

        // Push back onto the available stack
        availableStack.push(slot);

        activityLogger.log(actorId, "DRIVER", "SLOT_RELEASED",
                "Slot: " + slot.getSlotNumber());
        return slot;
    }

    // ── Admin operations ──────────────────────────────────────────────────────

    /**
     * Admin updates slot details: type reclassification, rate change, or
     * manual status override (e.g. MAINTENANCE).
     */
    public boolean updateSlot(String slotId, ParkingSlot.SlotStatus newStatus,
                               double newRate, String adminId) {
        Optional<ParkingSlot> opt = slotRepository.findById(slotId);
        if (opt.isEmpty()) return false;

        ParkingSlot slot = opt.get();
        ParkingSlot.SlotStatus oldStatus = slot.getStatus();

        slot.setStatus(newStatus);
        slot.setHourlyRate(newRate);
        slotRepository.update(slot);

        // Sync stack: if newly available, push; if no longer available, remove
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
}

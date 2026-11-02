package com.example.kitechin.common;

import java.util.List;

public final class TimeSlots {

    public static final List<String> ALL = List.of(
            "Morning (9AM - 12PM)",
            "Afternoon (12PM - 4PM)",
            "Evening (4PM - 8PM)"
    );

    private TimeSlots() {
    }

    public static boolean isAllowed(String timeSlot) {
        return timeSlot != null && ALL.contains(timeSlot);
    }
}

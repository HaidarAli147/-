package com.salah.bahrain;

import java.time.LocalDateTime;

/** Pure logic (no Android classes, so it can be tested on a PC): which adhan comes next. */
final class AdhanPlan {
    /** Adhan is only called at these three prayers. */
    static final String[] NAMES = {"الفجر", "الظهر", "المغرب"};

    static final class Next {
        final String name;
        final LocalDateTime at;   // Bahrain local time, minute precision
        Next(String name, LocalDateTime at) { this.name = name; this.at = at; }
    }

    /** The first Fajr/Dhuhr/Maghrib strictly after {@code now}. Times are recomputed for each day, so they follow the sun. */
    static Next next(LocalDateTime now) {
        for (int d = 0; d < 3; d++) {
            java.time.LocalDate date = now.toLocalDate().plusDays(d);
            PrayerCalc.Times t = PrayerCalc.compute(date);
            double[] hours = {t.fajr, t.dhuhr, t.maghrib};
            for (int i = 0; i < NAMES.length; i++) {
                LocalDateTime at = PrayerCalc.at(date, hours[i]);
                if (at.isAfter(now)) return new Next(NAMES[i], at);
            }
        }
        return null; // cannot happen: tomorrow always has a Fajr
    }
}

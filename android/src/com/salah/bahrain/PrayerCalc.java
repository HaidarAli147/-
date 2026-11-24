package com.salah.bahrain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Prayer times for Bahrain, Ja'fari (Shia) method, computed offline.
 * Fajr 16 deg, Maghrib when the sun is 4 deg below the horizon, Isha 14 deg
 * (the Ja'fari parameters), Asr at shadow = 1x object length,
 * midnight = midpoint between sunset and Fajr.
 */
public final class PrayerCalc {
    // Manama, Bahrain (UTC+3, no daylight saving)
    public static final double LAT = 26.2285;
    public static final double LNG = 50.5860;
    public static final double TZ = 3.0;

    public static final double FAJR_ANGLE = 16.0;
    public static final double MAGHRIB_ANGLE = 4.0;
    public static final double ISHA_ANGLE = 14.0;

    /** Order: fajr, sunrise, dhuhr, asr, sunset, maghrib, isha, midnight (hours, local time). */
    public static final class Times {
        public double fajr, sunrise, dhuhr, asr, sunset, maghrib, isha, midnight;
    }

    private static double rad(double d) { return Math.toRadians(d); }
    private static double deg(double r) { return Math.toDegrees(r); }
    private static double fix(double a, double b) {
        a = a - b * Math.floor(a / b);
        return a < 0 ? a + b : a;
    }

    private static double julian(int y, int m, int d) {
        if (m <= 2) { y -= 1; m += 12; }
        double a = Math.floor(y / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    public static Times compute(LocalDate date) {
        double jd = julian(date.getYear(), date.getMonthValue(), date.getDayOfMonth()) - LNG / (15 * 24.0);
        double D = jd - 2451545.0;
        double g = fix(357.529 + 0.98560028 * D, 360);
        double q = fix(280.459 + 0.98564736 * D, 360);
        double L = fix(q + 1.915 * Math.sin(rad(g)) + 0.020 * Math.sin(rad(2 * g)), 360);
        double e = 23.439 - 0.00000036 * D;
        double RA = fix(deg(Math.atan2(Math.cos(rad(e)) * Math.sin(rad(L)), Math.cos(rad(L)))) / 15.0, 24);
        double eqt = q / 15.0 - RA;
        double decl = deg(Math.asin(Math.sin(rad(e)) * Math.sin(rad(L))));

        double noon = fix(12 - eqt - LNG / 15.0 + TZ, 24);

        Times t = new Times();
        t.dhuhr = noon;
        t.sunrise = noon - hourAngle(-0.833, decl);
        t.sunset = noon + hourAngle(-0.833, decl);
        t.fajr = noon - hourAngle(-FAJR_ANGLE, decl);
        t.maghrib = noon + hourAngle(-MAGHRIB_ANGLE, decl);
        t.isha = noon + hourAngle(-ISHA_ANGLE, decl);
        double asrAlt = deg(Math.atan(1.0 / (1.0 + Math.tan(rad(Math.abs(LAT - decl))))));
        t.asr = noon + hourAngle(asrAlt, decl);
        t.midnight = t.sunset + fix(t.fajr - t.sunset, 24) / 2.0;
        return t;
    }

    /** Hours between solar noon and the moment the sun is at altitude {@code alt} degrees. */
    private static double hourAngle(double alt, double decl) {
        double c = (Math.sin(rad(alt)) - Math.sin(rad(LAT)) * Math.sin(rad(decl)))
                / (Math.cos(rad(LAT)) * Math.cos(rad(decl)));
        return deg(Math.acos(Math.max(-1, Math.min(1, c)))) / 15.0;
    }

    /** Converts fractional hours to a LocalDateTime on the given date (rounded to the minute). */
    public static LocalDateTime at(LocalDate date, double hours) {
        long mins = Math.round(hours * 60);
        return date.atStartOfDay().plusMinutes(mins);
    }

    public static LocalTime timeOf(double hours) {
        return LocalTime.ofSecondOfDay(((Math.round(hours * 60) % 1440) + 1440) % 1440 * 60);
    }
}

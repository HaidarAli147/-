/* Prayer times for Manama, Bahrain - Ja'fari method, computed offline.
   Same maths as the Android app: Fajr 16deg, Maghrib 4deg, Isha 14deg, Asr shadow ratio 1,
   midnight = midpoint between sunset and Fajr. */
const PrayerCalc = (() => {
  const LAT = 26.2285, LNG = 50.5860, TZ = 3;
  const FAJR = 16, MAGHRIB = 4, ISHA = 14;
  const rad = d => d * Math.PI / 180, deg = r => r * 180 / Math.PI;
  const fix = (a, b) => { a = a - b * Math.floor(a / b); return a < 0 ? a + b : a; };

  function julian(y, m, d) {
    if (m <= 2) { y -= 1; m += 12; }
    const a = Math.floor(y / 100), b = 2 - a + Math.floor(a / 4);
    return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
  }
  function hourAngle(alt, decl) {
    const c = (Math.sin(rad(alt)) - Math.sin(rad(LAT)) * Math.sin(rad(decl))) /
              (Math.cos(rad(LAT)) * Math.cos(rad(decl)));
    return deg(Math.acos(Math.max(-1, Math.min(1, c)))) / 15;
  }
  /** y, m (1-12), d -> times in fractional hours (Bahrain local time). */
  function compute(y, m, d) {
    const jd = julian(y, m, d) - LNG / (15 * 24);
    const D = jd - 2451545;
    const g = fix(357.529 + 0.98560028 * D, 360);
    const q = fix(280.459 + 0.98564736 * D, 360);
    const L = fix(q + 1.915 * Math.sin(rad(g)) + 0.020 * Math.sin(rad(2 * g)), 360);
    const e = 23.439 - 0.00000036 * D;
    const RA = fix(deg(Math.atan2(Math.cos(rad(e)) * Math.sin(rad(L)), Math.cos(rad(L)))) / 15, 24);
    const eqt = q / 15 - RA;
    const decl = deg(Math.asin(Math.sin(rad(e)) * Math.sin(rad(L))));
    const noon = fix(12 - eqt - LNG / 15 + TZ, 24);
    const t = { dhuhr: noon };
    t.sunrise = noon - hourAngle(-0.833, decl);
    t.sunset = noon + hourAngle(-0.833, decl);
    t.fajr = noon - hourAngle(-FAJR, decl);
    t.maghrib = noon + hourAngle(-MAGHRIB, decl);
    t.isha = noon + hourAngle(-ISHA, decl);
    const asrAlt = deg(Math.atan(1 / (1 + Math.tan(rad(Math.abs(LAT - decl))))));
    t.asr = noon + hourAngle(asrAlt, decl);
    t.midnight = t.sunset + fix(t.fajr - t.sunset, 24) / 2;
    return t;
  }
  /** minutes since midnight, rounded like the Android app */
  const minutes = h => Math.round(h * 60);
  return { compute, minutes };
})();
if (typeof module !== 'undefined') module.exports = PrayerCalc;

const webpush = require('web-push');
let ready = false;
function init() {
  if (ready) return;
  const { VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, VAPID_SUBJECT } = process.env;
  if (!VAPID_PUBLIC_KEY || !VAPID_PRIVATE_KEY || !VAPID_SUBJECT) throw new Error('VAPID env vars missing');
  webpush.setVapidDetails(VAPID_SUBJECT, VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY);
  ready = true;
}
/** Sends one notification. Returns 'ok', 'gone' (subscription expired) or 'error'. */
async function send(sub, payload) {
  init();
  try {
    await webpush.sendNotification(sub, JSON.stringify(payload), { TTL: 120, urgency: 'high' });
    return 'ok';
  } catch (e) {
    if (e.statusCode === 404 || e.statusCode === 410) return 'gone';
    console.error('push failed', e.statusCode, e.body || e.message);
    return 'error';
  }
}
module.exports = { send };

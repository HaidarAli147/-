const webpush = require('web-push');
const crypto = require('crypto');
const b64u = b => Buffer.from(b).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

/** The VAPID key pair. Use VAPID_PRIVATE_KEY + VAPID_PUBLIC_KEY if both are set; otherwise derive a stable pair
    from a secret the server already holds (BLOB_READ_WRITE_TOKEN), so no extra secret has to be handled by hand. */
function keys() {
  const { VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, BLOB_READ_WRITE_TOKEN } = process.env;
  if (VAPID_PUBLIC_KEY && VAPID_PRIVATE_KEY) return { publicKey: VAPID_PUBLIC_KEY, privateKey: VAPID_PRIVATE_KEY };
  if (!BLOB_READ_WRITE_TOKEN) throw new Error('no secret available to derive push keys');
  const ecdh = crypto.createECDH('prime256v1');
  for (let i = 0; i < 10; i++) {
    const d = crypto.createHmac('sha256', BLOB_READ_WRITE_TOKEN).update('salah-vapid-v1:' + i).digest();
    try { ecdh.setPrivateKey(d); return { publicKey: b64u(ecdh.getPublicKey()), privateKey: b64u(d) }; } catch (e) { /* invalid scalar, try next */ }
  }
  throw new Error('could not derive push keys');
}

let ready = false;
function init() {
  if (ready) return;
  const k = keys();
  webpush.setVapidDetails(process.env.VAPID_SUBJECT || 'https://salah-bahrain.vercel.app', k.publicKey, k.privateKey);
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
module.exports = { send, publicKey: () => keys().publicKey };

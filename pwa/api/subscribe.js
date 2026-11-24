const { saveSub, removeSub } = require('./_store');

function valid(s) {
  return s && typeof s.endpoint === 'string' && s.endpoint.startsWith('https://') && s.endpoint.length < 1000 &&
    s.keys && typeof s.keys.p256dh === 'string' && typeof s.keys.auth === 'string';
}
module.exports = async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  const body = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : (req.body || {});
  if (req.method === 'POST') {
    if (!valid(body.subscription)) return res.status(400).json({ error: 'invalid subscription' });
    const { endpoint, keys } = body.subscription;
    await saveSub({ endpoint, keys: { p256dh: keys.p256dh, auth: keys.auth } });
    return res.status(200).json({ ok: true });
  }
  if (req.method === 'DELETE') {
    if (typeof body.endpoint !== 'string') return res.status(400).json({ error: 'endpoint required' });
    await removeSub(body.endpoint);
    return res.status(200).json({ ok: true });
  }
  res.setHeader('Allow', 'POST, DELETE');
  res.status(405).json({ error: 'method not allowed' });
};

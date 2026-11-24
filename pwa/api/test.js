/* Sends a test notification to ONE subscription (the caller's own device). */
const { getSub } = require('./_store');
const { send } = require('./_push');

module.exports = async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'POST') return res.status(405).json({ error: 'method not allowed' });
  const body = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : (req.body || {});
  if (typeof body.endpoint !== 'string') return res.status(400).json({ error: 'endpoint required' });
  const sub = await getSub(body.endpoint);
  if (!sub) return res.status(404).json({ error: 'not subscribed' });
  const r = await send(sub, { title: 'تجربة إشعار الأذان', body: 'إذا ظهر هذا الإشعار فالأذان سيصلك في وقته', tag: 'adhan-test' });
  res.status(r === 'ok' ? 200 : 502).json({ result: r });
};

/* The public push key the browser needs to subscribe. (Public by design.) */
const { publicKey } = require('./_push');

module.exports = async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  try { res.status(200).json({ publicKey: publicKey() }); }
  catch (e) { res.status(500).json({ error: 'push not configured' }); }
};

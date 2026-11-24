/* Where push subscriptions live.
   Production: Vercel Blob (needs BLOB_READ_WRITE_TOKEN).  Local tests: STORE=memory. */
const crypto = require('crypto');
const id = endpoint => crypto.createHash('sha256').update(endpoint).digest('hex').slice(0, 40);

const mem = new Map();
const memory = {
  async put(key, obj) { mem.set(key, JSON.stringify(obj)); },
  async get(key) { const v = mem.get(key); return v ? JSON.parse(v) : null; },
  async list(prefix) { return [...mem.keys()].filter(k => k.startsWith(prefix)); },
  async del(key) { mem.delete(key); }
};

function blobStore() {
  const blob = require('@vercel/blob');
  return {
    async put(key, obj) {
      await blob.put(key, JSON.stringify(obj), { access: 'public', addRandomSuffix: false, allowOverwrite: true, contentType: 'application/json' });
    },
    async get(key) {
      const { blobs } = await blob.list({ prefix: key, limit: 1 });
      const b = blobs.find(x => x.pathname === key);
      if (!b) return null;
      const r = await fetch(b.url + '?t=' + Date.now(), { cache: 'no-store' });
      return r.ok ? r.json() : null;
    },
    async list(prefix) {
      const out = []; let cursor;
      do { const r = await blob.list({ prefix, cursor }); out.push(...r.blobs.map(b => b.pathname)); cursor = r.cursor; } while (cursor);
      return out;
    },
    async del(key) {
      const { blobs } = await blob.list({ prefix: key, limit: 1 });
      const b = blobs.find(x => x.pathname === key);
      if (b) await blob.del(b.url);
    }
  };
}
const store = process.env.STORE === 'memory' ? memory : blobStore();

module.exports = {
  subKey: endpoint => `subs/${id(endpoint)}.json`,
  saveSub: sub => store.put(`subs/${id(sub.endpoint)}.json`, sub),
  getSub: endpoint => store.get(`subs/${id(endpoint)}.json`),
  removeSub: endpoint => store.del(`subs/${id(endpoint)}.json`),
  listSubs: async () => {
    const keys = await store.list('subs/');
    const subs = await Promise.all(keys.map(k => store.get(k)));
    return subs.filter(Boolean);
  },
  wasSent: key => store.get(`sent/${key}.json`).then(Boolean),
  markSent: key => store.put(`sent/${key}.json`, { at: Date.now() })
};

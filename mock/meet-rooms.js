// Process-local idempotency follows the lifetime of the mock room store.
module.exports = function roomKeys() {
  const entries = new Map();
  return {
    reset() { entries.clear(); },
    find(key, payload) {
      const previous = entries.get(key);
      if (!previous) return null;
      return previous.payload === payload ? previous.room : false;
    },
    save(key, payload, room) { if (key) entries.set(key, { payload, room }); },
  };
};

// src/routes/gigs.js
const express = require('express');
const db = require('../db');
const authenticate = require('../middleware/authenticate');
const { distanceKm } = require('../geo');
const hustle = require('../hustle');

const router = express.Router();

function serializeGig(row, viewerLat, viewerLng) {
  const gig = {
    id: row.id,
    title: row.title,
    description: row.description,
    category: row.category,
    priceCredits: row.price_credits,
    location: { lat: row.lat, lng: row.lng },
    neededBy: row.needed_by,
    status: row.status,
    postedBy: row.poster_id,
    assignedTo: row.assigned_to,
    createdAt: row.created_at,
  };
  if (viewerLat != null && viewerLng != null && row.lat != null && row.lng != null) {
    gig.distanceKm = Math.round(distanceKm(viewerLat, viewerLng, row.lat, row.lng) * 10) / 10;
  }
  return gig;
}

router.get('/', (req, res) => {
  const { lat, lng, radiusKm, category, status } = req.query;
  const rows = db
    .prepare('SELECT * FROM gigs WHERE status = ? ORDER BY created_at DESC')
    .all(status || 'open');

  let results = rows;
  if (category && category !== 'all') {
    results = results.filter((r) => r.category === category);
  }

  const viewerLat = lat != null ? parseFloat(lat) : null;
  const viewerLng = lng != null ? parseFloat(lng) : null;

  let serialized = results.map((r) => serializeGig(r, viewerLat, viewerLng));

  if (radiusKm != null && viewerLat != null && viewerLng != null) {
    const radius = parseFloat(radiusKm);
    serialized = serialized.filter((g) => g.distanceKm == null || g.distanceKm <= radius);
  }

  res.json({ gigs: serialized });
});

router.post('/', authenticate, (req, res) => {
  const { title, category, description, priceCredits, location, neededBy } = req.body;
  if (!title || !category) {
    return res.status(400).json({ error: 'title and category are required' });
  }

  const info = db
    .prepare(
      `INSERT INTO gigs (title, description, category, price_credits, lat, lng, needed_by, poster_id)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .run(
      title,
      description || null,
      category,
      priceCredits || 0,
      location?.lat ?? null,
      location?.lng ?? null,
      neededBy || null,
      req.userId
    );

  const gig = db.prepare('SELECT * FROM gigs WHERE id = ?').get(info.lastInsertRowid);
  res.status(201).json(serializeGig(gig));
});

router.get('/:id', (req, res) => {
  const gig = db.prepare('SELECT * FROM gigs WHERE id = ?').get(req.params.id);
  if (!gig) return res.status(404).json({ error: 'Gig not found' });
  res.json(serializeGig(gig));
});

router.post('/:id/apply', authenticate, (req, res) => {
  const gig = db.prepare('SELECT * FROM gigs WHERE id = ?').get(req.params.id);
  if (!gig) return res.status(404).json({ error: 'Gig not found' });
  if (gig.status !== 'open') return res.status(409).json({ error: 'Gig is no longer open' });
  if (gig.poster_id === Number(req.userId)) {
    return res.status(400).json({ error: "You can't apply to your own gig" });
  }

  try {
    db.prepare('INSERT INTO gig_applications (gig_id, applicant_id) VALUES (?, ?)').run(
      req.params.id,
      req.userId
    );
  } catch (e) {
    return res.status(409).json({ error: 'You already applied to this gig' });
  }

  res.status(201).json({ status: 'applied' });
});

router.patch('/:id/status', authenticate, (req, res) => {
  const gig = db.prepare('SELECT * FROM gigs WHERE id = ?').get(req.params.id);
  if (!gig) return res.status(404).json({ error: 'Gig not found' });
  if (gig.poster_id !== Number(req.userId)) {
    return res.status(403).json({ error: 'Only the gig poster can update its status' });
  }

  const { status, assignedTo, stars, onTime } = req.body;
  const allowed = ['open', 'assigned', 'completed', 'cancelled'];
  if (!allowed.includes(status)) {
    return res.status(400).json({ error: `status must be one of ${allowed.join(', ')}` });
  }

  db.prepare("UPDATE gigs SET status = ?, assigned_to = COALESCE(?, assigned_to), updated_at = datetime('now') WHERE id = ?")
    .run(status, assignedTo ?? null, req.params.id);

  let hustleResult = null;
  if (status === 'completed') {
    const workerId = assignedTo ?? gig.assigned_to;
    if (workerId) {
      if (stars != null) {
        db.prepare('INSERT INTO ratings (gig_id, rated_user_id, stars) VALUES (?, ?, ?)').run(
          req.params.id,
          workerId,
          stars
        );
      }
      hustleResult = hustle.awardCompletion({
        userId: workerId,
        gigId: req.params.id,
        stars: stars ?? 5,
        onTime: onTime !== false,
      });
    }
  }

  const updated = db.prepare('SELECT * FROM gigs WHERE id = ?').get(req.params.id);
  res.json({ gig: serializeGig(updated), hustle: hustleResult });
});

module.exports = router;

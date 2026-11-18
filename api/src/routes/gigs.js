// src/routes/gigs.js — the gig marketplace: feed, posting, applying and the gig life-cycle.
//
//   open ──assign──▶ assigned ──complete──▶ completed      (terminal)
//     │                 │  └──withdraw (worker)──▶ open
//     └─────cancel──────┴──────────────────────▶ cancelled (terminal)
const express = require('express');
const db = require('../db');
const { authenticate, optionalAuthenticate } = require('../middleware/authenticate');
const { distanceKm } = require('../geo');
const hustle = require('../hustle');
const { notifyUser } = require('../notify');
const { iso, isInt, CATEGORIES } = require('../util');

const router = express.Router();

const STATUSES = ['open', 'assigned', 'completed', 'cancelled'];

// One query shape for every endpoint so the JSON is identical everywhere.
const GIG_SELECT = `
  SELECT g.*, p.name AS poster_name, w.name AS assignee_name,
    (SELECT COUNT(*) FROM gig_applications a WHERE a.gig_id = g.id) AS applicant_count,
    (SELECT a.status FROM gig_applications a WHERE a.gig_id = g.id AND a.applicant_id = @viewer) AS my_application
  FROM gigs g
  JOIN users p ON p.id = g.poster_id
  LEFT JOIN users w ON w.id = g.assigned_to`;

function loadGig(id, viewerId = null) {
  return db.prepare(`${GIG_SELECT} WHERE g.id = @id`).get({ id, viewer: viewerId });
}

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
    posterName: row.poster_name,
    assignedTo: row.assigned_to,
    assignedToName: row.assignee_name ?? null,
    applicantCount: row.applicant_count ?? 0,
    myApplication: row.my_application ?? null,
    createdAt: iso(row.created_at),
    updatedAt: iso(row.updated_at),
  };
  if (viewerLat != null && viewerLng != null && row.lat != null && row.lng != null) {
    gig.distanceKm = Math.round(distanceKm(viewerLat, viewerLng, row.lat, row.lng) * 10) / 10;
  }
  return gig;
}

function parseCoord(value, min, max) {
  if (value == null || value === '') return null;
  const n = Number(value);
  return Number.isFinite(n) && n >= min && n <= max ? n : NaN;
}

// ---------------------------------------------------------------- feed
router.get('/', optionalAuthenticate, (req, res) => {
  const { lat, lng, radiusKm, category, status } = req.query;
  if (status && !STATUSES.includes(status)) {
    return res.status(400).json({ error: `status must be one of ${STATUSES.join(', ')}` });
  }

  const rows = db
    .prepare(`${GIG_SELECT} WHERE g.status = @status ORDER BY g.created_at DESC, g.id DESC LIMIT 200`)
    .all({ status: status || 'open', viewer: req.userId });

  const results = category && category !== 'all' ? rows.filter((r) => r.category === category) : rows;

  const viewerLat = parseCoord(lat, -90, 90);
  const viewerLng = parseCoord(lng, -180, 180);
  const haveViewer = Number.isFinite(viewerLat) && Number.isFinite(viewerLng);

  let serialized = results.map((r) => serializeGig(r, haveViewer ? viewerLat : null, haveViewer ? viewerLng : null));
  if (radiusKm != null && haveViewer) {
    const radius = parseFloat(radiusKm);
    if (Number.isFinite(radius)) {
      serialized = serialized.filter((g) => g.distanceKm == null || g.distanceKm <= radius);
    }
  }
  res.json({ gigs: serialized });
});

// ------------------------------------------- gigs I posted, work on, or applied to
// (declared before '/:id' so "mine" is not parsed as an id)
router.get('/mine', authenticate, (req, res) => {
  const rows = db
    .prepare(
      `${GIG_SELECT}
       WHERE g.poster_id = @viewer OR g.assigned_to = @viewer
          OR EXISTS (SELECT 1 FROM gig_applications a WHERE a.gig_id = g.id AND a.applicant_id = @viewer)
       ORDER BY g.updated_at DESC, g.id DESC LIMIT 200`
    )
    .all({ viewer: req.userId });
  res.json({ gigs: rows.map((r) => serializeGig(r)) });
});

// ---------------------------------------------------------------- post
router.post('/', authenticate, (req, res) => {
  const { description, priceCredits, location, neededBy, clientId } = req.body;
  const title = String(req.body.title || '').trim();
  const category = req.body.category;

  if (!title || !category) return res.status(400).json({ error: 'title and category are required' });
  if (title.length > 120) return res.status(400).json({ error: 'title must be at most 120 characters' });
  if (!CATEGORIES.includes(category)) {
    return res.status(400).json({ error: `category must be one of ${CATEGORIES.join(', ')}` });
  }
  if (description != null && String(description).length > 2000) {
    return res.status(400).json({ error: 'description must be at most 2000 characters' });
  }
  const price = priceCredits == null ? 0 : priceCredits;
  if (!isInt(price) || price < 0 || price > 100000) {
    return res.status(400).json({ error: 'priceCredits must be a whole number between 0 and 100000' });
  }
  const lat = parseCoord(location?.lat, -90, 90);
  const lng = parseCoord(location?.lng, -180, 180);
  if (Number.isNaN(lat) || Number.isNaN(lng)) return res.status(400).json({ error: 'location is not valid' });
  if (neededBy && Number.isNaN(Date.parse(neededBy))) {
    return res.status(400).json({ error: 'neededBy must be a valid date' });
  }

  // Idempotent create: an offline draft that is re-sent after a lost response returns the same gig.
  if (clientId) {
    const existing = db.prepare('SELECT id FROM gigs WHERE poster_id = ? AND client_id = ?').get(req.userId, String(clientId));
    if (existing) return res.status(200).json(serializeGig(loadGig(existing.id, req.userId)));
  }

  const info = db
    .prepare(
      `INSERT INTO gigs (title, description, category, price_credits, lat, lng, needed_by, poster_id, client_id)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .run(title, description || null, category, price, lat, lng, neededBy || null, req.userId,
      clientId ? String(clientId) : null);

  res.status(201).json(serializeGig(loadGig(info.lastInsertRowid, req.userId)));
});

// ---------------------------------------------------------------- read one
router.get('/:id', optionalAuthenticate, (req, res) => {
  const gig = loadGig(req.params.id, req.userId);
  if (!gig) return res.status(404).json({ error: 'Gig not found' });
  res.json(serializeGig(gig));
});

// ---------------------------------------------------------------- applicants (poster only)
router.get('/:id/applicants', authenticate, (req, res) => {
  const gig = db.prepare('SELECT * FROM gigs WHERE id = ?').get(req.params.id);
  if (!gig) return res.status(404).json({ error: 'Gig not found' });
  if (gig.poster_id !== req.userId) {
    return res.status(403).json({ error: 'Only the gig poster can see applicants' });
  }
  const rows = db
    .prepare(
      `SELECT u.id, u.name, u.hustle_score, u.verification_status, a.status, a.created_at
       FROM gig_applications a JOIN users u ON u.id = a.applicant_id
       WHERE a.gig_id = ? ORDER BY u.hustle_score DESC, a.created_at ASC`
    )
    .all(req.params.id);
  res.json({
    applicants: rows.map((r) => {
      const stats = hustle.getUserStats(r.id);
      return {
        userId: r.id,
        name: r.name,
        hustleScore: r.hustle_score,
        verificationStatus: r.verification_status,
        completedGigs: stats.completedCount,
        averageRating: Math.round((stats.averageRating || 0) * 10) / 10,
        status: r.status,
        appliedAt: iso(r.created_at),
      };
    }),
  });
});

// ---------------------------------------------------------------- apply
router.post('/:id/apply', authenticate, (req, res) => {
  const gig = db.prepare('SELECT * FROM gigs WHERE id = ?').get(req.params.id);
  if (!gig) return res.status(404).json({ error: 'Gig not found' });
  if (gig.status !== 'open') return res.status(409).json({ error: 'Gig is no longer open' });
  if (gig.poster_id === req.userId) {
    return res.status(400).json({ error: "You can't apply to your own gig" });
  }

  try {
    db.prepare('INSERT INTO gig_applications (gig_id, applicant_id) VALUES (?, ?)').run(gig.id, req.userId);
  } catch (e) {
    return res.status(409).json({ error: 'You already applied to this gig' });
  }

  const applicant = db.prepare('SELECT name FROM users WHERE id = ?').get(req.userId);
  notifyUser(gig.poster_id, {
    type: 'application',
    title: 'New applicant',
    body: `${applicant.name} applied to “${gig.title}”`,
    gigId: gig.id,
  });
  res.status(201).json({ status: 'applied' });
});

// ---------------------------------------------------------------- status transitions (poster)
router.patch('/:id/status', authenticate, (req, res) => {
  const gig = db.prepare('SELECT * FROM gigs WHERE id = ?').get(req.params.id);
  if (!gig) return res.status(404).json({ error: 'Gig not found' });
  if (gig.poster_id !== req.userId) {
    return res.status(403).json({ error: 'Only the gig poster can update its status' });
  }

  const { status, assignedTo, stars, onTime } = req.body;
  if (!STATUSES.includes(status) || status === 'open') {
    return res.status(400).json({ error: 'status must be one of assigned, completed, cancelled' });
  }

  let hustleResult = null;

  if (status === 'assigned') {
    if (gig.status !== 'open') return res.status(409).json({ error: 'Only an open gig can be assigned' });
    const application = db
      .prepare('SELECT 1 FROM gig_applications WHERE gig_id = ? AND applicant_id = ?')
      .get(gig.id, assignedTo);
    if (!application) return res.status(400).json({ error: 'assignedTo must be someone who applied to this gig' });

    const others = db
      .prepare('SELECT applicant_id FROM gig_applications WHERE gig_id = ? AND applicant_id != ?')
      .all(gig.id, assignedTo);
    db.transaction(() => {
      db.prepare("UPDATE gigs SET status = 'assigned', assigned_to = ?, updated_at = datetime('now') WHERE id = ?").run(assignedTo, gig.id);
      db.prepare("UPDATE gig_applications SET status = 'accepted' WHERE gig_id = ? AND applicant_id = ?").run(gig.id, assignedTo);
      db.prepare("UPDATE gig_applications SET status = 'rejected' WHERE gig_id = ? AND applicant_id != ?").run(gig.id, assignedTo);
    })();
    notifyUser(assignedTo, { type: 'assigned', title: 'You got the gig!', body: `You were picked for “${gig.title}”`, gigId: gig.id });
    for (const o of others) {
      notifyUser(o.applicant_id, { type: 'gig_filled', title: 'Gig filled', body: `“${gig.title}” went to another applicant`, gigId: gig.id });
    }
  } else if (status === 'completed') {
    if (gig.status !== 'assigned' || !gig.assigned_to) {
      return res.status(409).json({ error: 'Only an assigned gig can be completed' });
    }
    if (!isInt(stars) || stars < 1 || stars > 5) {
      return res.status(400).json({ error: 'stars (1-5) is required to complete a gig' });
    }
    db.transaction(() => {
      db.prepare("UPDATE gigs SET status = 'completed', updated_at = datetime('now') WHERE id = ?").run(gig.id);
      db.prepare('INSERT INTO ratings (gig_id, rated_user_id, stars) VALUES (?, ?, ?)').run(gig.id, gig.assigned_to, stars);
      hustleResult = hustle.awardCompletion({ userId: gig.assigned_to, gigId: gig.id, stars, onTime: onTime !== false });
    })();
    const badgeText = hustleResult.newBadges.length ? ` · New badge: ${hustleResult.newBadges.map((b) => b.label).join(', ')}` : '';
    notifyUser(gig.assigned_to, {
      type: 'completed',
      title: 'Gig completed',
      body: `+${hustleResult.points} Hustle points for “${gig.title}”${badgeText}`,
      gigId: gig.id,
    });
  } else if (status === 'cancelled') {
    if (gig.status !== 'open' && gig.status !== 'assigned') {
      return res.status(409).json({ error: `A ${gig.status} gig cannot be cancelled` });
    }
    db.prepare("UPDATE gigs SET status = 'cancelled', updated_at = datetime('now') WHERE id = ?").run(gig.id);
    if (gig.assigned_to) {
      notifyUser(gig.assigned_to, { type: 'cancelled', title: 'Gig cancelled', body: `“${gig.title}” was cancelled by the poster`, gigId: gig.id });
    }
  }

  res.json({ gig: serializeGig(loadGig(gig.id, req.userId)), hustle: hustleResult });
});

// ---------------------------------------------------------------- withdraw (assigned worker)
router.post('/:id/withdraw', authenticate, (req, res) => {
  const gig = db.prepare('SELECT * FROM gigs WHERE id = ?').get(req.params.id);
  if (!gig) return res.status(404).json({ error: 'Gig not found' });
  if (gig.status !== 'assigned' || gig.assigned_to !== req.userId) {
    return res.status(403).json({ error: 'Only the assigned worker can withdraw from an assigned gig' });
  }
  db.transaction(() => {
    db.prepare("UPDATE gigs SET status = 'open', assigned_to = NULL, updated_at = datetime('now') WHERE id = ?").run(gig.id);
    db.prepare("UPDATE gig_applications SET status = 'withdrawn' WHERE gig_id = ? AND applicant_id = ?").run(gig.id, req.userId);
    db.prepare('INSERT INTO gig_withdrawals (gig_id, user_id) VALUES (?, ?)').run(gig.id, req.userId);
  })();
  const worker = db.prepare('SELECT name FROM users WHERE id = ?').get(req.userId);
  notifyUser(gig.poster_id, { type: 'withdrawn', title: 'Worker withdrew', body: `${worker.name} withdrew from “${gig.title}” — it is open again`, gigId: gig.id });
  res.json({ gig: serializeGig(loadGig(gig.id, req.userId)) });
});

module.exports = router;

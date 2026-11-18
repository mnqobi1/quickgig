// tests/api.test.js
const fs = require('fs');
const path = require('path');

// Mock Google ID-token verification so the SSO test doesn't need a real Google token.
jest.mock('google-auth-library', () => ({
  OAuth2Client: jest.fn().mockImplementation(() => ({
    verifyIdToken: jest.fn().mockResolvedValue({
      getPayload: () => ({
        sub: 'google-sub-123',
        email: 'sso-user@university.ac.za',
        email_verified: true,
        name: 'SSO User',
      }),
    }),
  })),
}));

const TEST_DB = path.join(__dirname, 'test.db');
if (fs.existsSync(TEST_DB)) fs.unlinkSync(TEST_DB);
process.env.QUICKGIG_DB = TEST_DB;
process.env.JWT_SECRET = 'test-secret';

const request = require('supertest');
const app = require('../server');
const db = require('../src/db');

afterAll(() => {
  db.close();
  for (const suffix of ['', '-wal', '-shm']) {
    const f = TEST_DB + suffix;
    if (fs.existsSync(f)) fs.unlinkSync(f);
  }
});

describe('health check', () => {
  it('responds ok', async () => {
    const res = await request(app).get('/api/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
  });
});

describe('auth', () => {
  it('registers a new user and returns a token', async () => {
    const res = await request(app).post('/api/auth/register').send({
      name: 'Thuli Student', email: 'thuli@example.com', password: 'hunter2pass', preferredLanguage: 'zu',
    });
    expect(res.status).toBe(201);
    expect(res.body.token).toBeDefined();
    expect(res.body.user.email).toBe('thuli@example.com');
    expect(res.body.user.preferredLanguage).toBe('zu');
  });

  it('rejects a duplicate email', async () => {
    const res = await request(app).post('/api/auth/register').send({
      name: 'Dup', email: 'thuli@example.com', password: 'anotherpass1',
    });
    expect(res.status).toBe(409);
  });

  it('rejects a short password', async () => {
    const res = await request(app).post('/api/auth/register').send({
      name: 'Short', email: 'short@example.com', password: '123',
    });
    expect(res.status).toBe(400);
  });

  it('auto-verifies a student email domain', async () => {
    const res = await request(app).post('/api/auth/register').send({
      name: 'Campus Student', email: 'student@university.ac.za', password: 'campuspass1',
    });
    expect(res.body.user.verificationStatus).toBe('verified');
  });

  it('logs in with correct credentials and rejects wrong ones', async () => {
    const ok = await request(app).post('/api/auth/login').send({ email: 'thuli@example.com', password: 'hunter2pass' });
    expect(ok.status).toBe(200);
    expect(ok.body.token).toBeDefined();
    const bad = await request(app).post('/api/auth/login').send({ email: 'thuli@example.com', password: 'wrongpassword' });
    expect(bad.status).toBe(401);
  });

  it('logs in or registers via the Google SSO stub', async () => {
    const res = await request(app).post('/api/auth/google').send({ idToken: 'fake-google-id-token', name: 'SSO User' });
    expect(res.status).toBe(200);
    expect(res.body.user.verificationStatus).toBe('verified');
    expect(res.body.user.email).toBe('sso-user@university.ac.za');
  });

  it('rejects protected routes without a token', async () => {
    const res = await request(app).post('/api/gigs').send({ title: 'x', category: 'errands' });
    expect(res.status).toBe(401);
  });
});

describe('gigs, applications, and hustle score', () => {
  let posterToken, posterId, workerToken, workerId, gigId;

  beforeAll(async () => {
    const poster = await request(app).post('/api/auth/register').send({ name: 'Gig Poster', email: 'poster@example.com', password: 'posterpass1' });
    posterToken = poster.body.token; posterId = poster.body.user.id;
    const worker = await request(app).post('/api/auth/register').send({ name: 'Gig Worker', email: 'worker@example.com', password: 'workerpass1' });
    workerToken = worker.body.token; workerId = worker.body.user.id;
  });

  it('creates a gig', async () => {
    const res = await request(app).post('/api/gigs').set('Authorization', `Bearer ${posterToken}`).send({
      title: 'Dog walk, 30 min', category: 'errands', priceCredits: 50,
      location: { lat: -33.928, lng: 18.417 }, neededBy: '2026-09-12T16:00:00Z',
    });
    expect(res.status).toBe(201);
    expect(res.body.status).toBe('open');
    gigId = res.body.id;
  });

  it('filters the feed by radius', async () => {
    const near = await request(app).get('/api/gigs?lat=-33.93&lng=18.42&radiusKm=5');
    expect(near.body.gigs.some((g) => g.id === gigId)).toBe(true);
    const far = await request(app).get('/api/gigs?lat=51.5&lng=-0.12&radiusKm=5');
    expect(far.body.gigs.some((g) => g.id === gigId)).toBe(false);
  });

  it('lets a different user apply, but not the poster themselves', async () => {
    const selfApply = await request(app).post(`/api/gigs/${gigId}/apply`).set('Authorization', `Bearer ${posterToken}`);
    expect(selfApply.status).toBe(400);
    const apply = await request(app).post(`/api/gigs/${gigId}/apply`).set('Authorization', `Bearer ${workerToken}`);
    expect(apply.status).toBe(201);
  });

  it('only the poster can update gig status', async () => {
    const res = await request(app).patch(`/api/gigs/${gigId}/status`).set('Authorization', `Bearer ${workerToken}`).send({ status: 'assigned', assignedTo: workerId });
    expect(res.status).toBe(403);
  });

  it('completing a gig awards Hustle Points and can unlock a badge', async () => {
    await request(app).patch(`/api/gigs/${gigId}/status`).set('Authorization', `Bearer ${posterToken}`).send({ status: 'assigned', assignedTo: workerId });
    const complete = await request(app).patch(`/api/gigs/${gigId}/status`).set('Authorization', `Bearer ${posterToken}`).send({ status: 'completed', assignedTo: workerId, stars: 5, onTime: true });
    expect(complete.status).toBe(200);
    expect(complete.body.gig.status).toBe('completed');
    expect(complete.body.hustle.points).toBe(25);
    const profile = await request(app).get(`/api/users/${workerId}`);
    expect(profile.body.hustleScore).toBe(25);
    expect(profile.body.completedGigs).toBe(1);
  });
});

describe('settings and chat', () => {
  it("lets a user update their own settings but not someone else's", async () => {
    const a = await request(app).post('/api/auth/register').send({ name: 'User A', email: 'usera@example.com', password: 'passwordA1' });
    const b = await request(app).post('/api/auth/register').send({ name: 'User B', email: 'userb@example.com', password: 'passwordB1' });
    const ok = await request(app).patch(`/api/users/${a.body.user.id}/settings`).set('Authorization', `Bearer ${a.body.token}`).send({ preferredLanguage: 'af' });
    expect(ok.status).toBe(200);
    expect(ok.body.preferredLanguage).toBe('af');
    const forbidden = await request(app).patch(`/api/users/${b.body.user.id}/settings`).set('Authorization', `Bearer ${a.body.token}`).send({ preferredLanguage: 'af' });
    expect(forbidden.status).toBe(403);
  });

  it('only lets the gig poster and assigned worker read/send chat messages', async () => {
    const poster = await request(app).post('/api/auth/register').send({ name: 'Chat Poster', email: 'chatposter@example.com', password: 'chatpass123' });
    const worker = await request(app).post('/api/auth/register').send({ name: 'Chat Worker', email: 'chatworker@example.com', password: 'chatpass234' });
    const stranger = await request(app).post('/api/auth/register').send({ name: 'Stranger', email: 'stranger@example.com', password: 'strangepass1' });
    const gig = await request(app).post('/api/gigs').set('Authorization', `Bearer ${poster.body.token}`).send({ title: 'Move a couch', category: 'labour', priceCredits: 80 });
    await request(app).post(`/api/gigs/${gig.body.id}/apply`).set('Authorization', `Bearer ${worker.body.token}`);
    await request(app).patch(`/api/gigs/${gig.body.id}/status`).set('Authorization', `Bearer ${poster.body.token}`).send({ status: 'assigned', assignedTo: worker.body.user.id });
    const send = await request(app).post(`/api/gigs/${gig.body.id}/messages`).set('Authorization', `Bearer ${poster.body.token}`).send({ recipientId: worker.body.user.id, body: 'Still on for 4pm?' });
    expect(send.status).toBe(201);
    const blocked = await request(app).get(`/api/gigs/${gig.body.id}/messages`).set('Authorization', `Bearer ${stranger.body.token}`);
    expect(blocked.status).toBe(403);
    const allowed = await request(app).get(`/api/gigs/${gig.body.id}/messages`).set('Authorization', `Bearer ${worker.body.token}`);
    expect(allowed.status).toBe(200);
    expect(allowed.body.messages).toHaveLength(1);
  });
});

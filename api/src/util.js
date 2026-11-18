// src/util.js — small shared helpers.

function normalizeEmail(email) {
  return String(email || '').trim().toLowerCase();
}

function isValidEmail(email) {
  return email.length <= 254 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

/** University / college addresses (.ac.za, .edu, .edu.za) count as a verified student. */
function isStudentEmail(email) {
  const domain = String(email).split('@')[1] || '';
  return /(\.ac\.za|\.edu|\.edu\.za)$/i.test(domain);
}

/** SQLite's datetime('now') is UTC without a zone; clients want unambiguous ISO-8601. */
function iso(sqliteTimestamp) {
  return sqliteTimestamp ? `${String(sqliteTimestamp).replace(' ', 'T')}Z` : null;
}

const LANGUAGES = ['en', 'zu', 'af'];
const CATEGORIES = ['errands', 'tutoring', 'creative', 'labour'];

function isInt(n) {
  return Number.isInteger(n);
}

module.exports = { normalizeEmail, isValidEmail, isStudentEmail, iso, isInt, LANGUAGES, CATEGORIES };

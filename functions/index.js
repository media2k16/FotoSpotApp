// functions/index.js
const { onCall, HttpsError } = require('firebase-functions/v2/https');
const logger = require('firebase-functions/logger');
const nodemailer = require('nodemailer');

const admin = require('firebase-admin');
try { admin.app(); } catch { admin.initializeApp(); }
const db = admin.firestore();

// --- SMTP aus Secrets/ENV ---
const SMTP_HOST   = process.env.SMTP_HOST;
const SMTP_PORT   = Number(process.env.SMTP_PORT || 465);
const SMTP_SECURE = String(process.env.SMTP_SECURE || 'true') === 'true';
const SMTP_USER   = process.env.SMTP_USER;
const SMTP_PASS   = process.env.SMTP_PASS;
const SMTP_TO     = process.env.SMTP_TO;
const SMTP_FROM   = process.env.SMTP_FROM || `FotoSpots <${SMTP_USER || 'no-reply@localhost'}>`;

// Lazy Transporter
let _transporter = null;
function getTransporter() {
  if (_transporter) return _transporter;

  if (!SMTP_HOST || !SMTP_USER || !SMTP_PASS || !SMTP_TO) {
    logger.error('E-Mail-Konfiguration unvollständig (SMTP_*).');
    throw new HttpsError('internal', 'E-Mail-Versand nicht konfiguriert (SMTP Secrets fehlen).');
  }

  _transporter = nodemailer.createTransport({
    host: SMTP_HOST,
    port: SMTP_PORT,
    secure: SMTP_SECURE,
    auth: { user: SMTP_USER, pass: SMTP_PASS },
  });
  return _transporter;
}

// Escape-Helfer
function esc(s) {
  if (s === undefined || s === null) return '';
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

// HTML-Mail
function buildHtml({ username, email, city, street, note, latitude, longitude }) {
  const rows = [
    ['👤 Benutzername', esc(username)],
    ['📧 E-Mail', esc(email)],
    ['🏙️ Stadt', esc(city)],
    ['🛣️ Straße', esc(street)],
    ['📝 Hinweis', esc(note)],
    ['🌍 Koordinaten', `${esc(latitude)}, ${esc(longitude)}`],
  ];

  const tableRows = rows.map(([label, value]) => `
    <tr>
      <td style="padding:10px 12px;font-weight:600;color:#111827;background:#F9FAFB;border-bottom:1px solid #EDF2F7;white-space:nowrap;">${label}</td>
      <td style="padding:10px 12px;color:#111827;background:#FFFFFF;border-bottom:1px solid #EDF2F7;">${value || '<span style="color:#9CA3AF">nicht übermittelt</span>'}</td>
    </tr>`).join('');

  return `<!doctype html><html lang="de"><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/><title>Neue Location vorgeschlagen</title></head>
  <body style="margin:0;background:#F3F4F6;padding:24px;font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,'Helvetica Neue','Noto Sans',Arial;">
    <div style="max-width:640px;margin:0 auto;">
      <div style="background:#111827;color:#fff;border-radius:12px 12px 0 0;padding:16px 20px;">
        <div style="font-size:16px;opacity:.9;">FotoSpots</div>
        <div style="font-size:20px;font-weight:700;margin-top:2px;">📍 Neue Location vorgeschlagen</div>
      </div>
      <div style="background:#fff;border:1px solid #E5E7EB;border-top:none;border-radius:0 0 12px 12px;overflow:hidden;">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;">
          <tbody>${tableRows}</tbody>
        </table>
        <div style="padding:14px 16px;border-top:1px dashed #E5E7EB;background:#FAFAFA;color:#374151;font-size:12px;line-height:1.5;">
          💡 In Google Maps öffnen →
          <a href="https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${latitude},${longitude}`)}" style="color:#0EA5E9;text-decoration:none;">${esc(latitude)}, ${esc(longitude)}</a>
        </div>
      </div>
      <div style="text-align:center;color:#6B7280;font-size:12px;margin-top:14px;">
        Diese Nachricht wurde automatisch von der FotoSpots App generiert.
      </div>
    </div>
  </body></html>`;
}

// Text-Fallback
function buildText({ username, email, city, street, note, latitude, longitude }) {
  const safe = (v) => (v ? String(v) : 'nicht übermittelt');
  return [
    '📍 Neue Location wurde vorgeschlagen:',
    '',
    `👤 Benutzername: ${safe(username)}`,
    `📧 E-Mail: ${safe(email)}`,
    '',
    `🏙 Stadt: ${safe(city)}`,
    `🛣 Straße: ${safe(street)}`,
    `📝 Hinweis: ${safe(note)}`,
    `🌍 Koordinaten: ${safe(latitude)}, ${safe(longitude)}`,
    '',
    `Google Maps: https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${latitude},${longitude}`)}`,
  ].join('\n');
}

// ---- EINZIGE onCall-Funktion ----
exports.sendLocationEmail = onCall({ region: 'us-central1', timeoutSeconds: 60 }, async (req) => {
  const p = req.data || {};
  const payload = {
    email: p.email || '',
    username: p.username || '',
    city: p.city || '',
    street: p.street || '',
    note: p.note || '',
    latitude: p.latitude || '',
    longitude: p.longitude || '',
  };

  const replyTo = payload.email && payload.email.includes('@') ? payload.email : undefined;
  const subject = `Neue Location: ${payload.city || 'ohne Stadt'} • FotoSpots`;

  const mailOptions = {
    from: SMTP_FROM,
    to:   SMTP_TO,
    subject,
    text: buildText(payload),
    html: buildHtml(payload),
    replyTo,
  };

  try {
    // 1) E-Mail senden
    await getTransporter().sendMail(mailOptions);

    // 2) Vorschlag zusätzlich in Firestore speichern
    await db.collection('suggestions').add({
      ...payload,
      status: 'open', // open | approved | rejected
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    logger.info('E-Mail versendet & Vorschlag gespeichert.');
    return { ok: true };
  } catch (err) {
    logger.error('Fehler beim E-Mail-Versand oder Speichern:', err);
    throw new HttpsError('internal', 'E-Mail/Vorschlag konnte nicht verarbeitet werden');
  }
});
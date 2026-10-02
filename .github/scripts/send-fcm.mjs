import crypto from 'node:crypto';
import https from 'node:https';

const projectId = 'bmu-pds';
const topic = 'all_users';

const title = process.env.INPUT_TITLE || '';
const message = process.env.INPUT_MESSAGE || '';
const rawDeepUrl = process.env.INPUT_DEEP_URL || '';
const urgent = process.env.INPUT_URGENT === 'true';

if (!title.trim() || title.length > 100) {
  console.error('Error: Title is required and must be between 1 and 100 characters.');
  process.exit(1);
}

if (!message.trim() || message.length > 1000) {
  console.error('Error: Message is required and must be between 1 and 1000 characters.');
  process.exit(1);
}

let validatedDeepUrl = '';
if (rawDeepUrl.trim()) {
  try {
    const u = new URL(rawDeepUrl.trim());
    if (u.protocol === 'https:' &&
        (u.hostname === 'pds.bmu.ac.bd' || u.hostname === 'attendance.bmu.ac.bd') &&
        (u.port === '' || u.port === '443') &&
        !u.username && !u.password) {
      validatedDeepUrl = u.toString();
    } else {
      console.error('Error: Invalid deep URL scheme, host, or credentials.');
      process.exit(1);
    }
  } catch (e) {
    console.error('Error: Malformed deep URL.');
    process.exit(1);
  }
}

const saEnv = process.env.FIREBASE_SERVICE_ACCOUNT;
if (!saEnv) {
  console.error('Error: FIREBASE_SERVICE_ACCOUNT secret is missing.');
  process.exit(1);
}

let sa;
try {
  sa = JSON.parse(saEnv);
} catch (e) {
  console.error('Error: FIREBASE_SERVICE_ACCOUNT is not valid JSON.');
  process.exit(1);
}

if (sa.project_id !== projectId) {
  console.error(`Error: Service account project_id (${sa.project_id}) does not match expected (${projectId}).`);
  process.exit(1);
}

function base64url(str) {
  return Buffer.from(str).toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function getAccessToken() {
  return new Promise((resolve, reject) => {
    const header = { alg: 'RS256', typ: 'JWT' };
    const now = Math.floor(Date.now() / 1000);
    const claim = {
      iss: sa.client_email,
      scope: 'https://www.googleapis.com/auth/firebase.messaging',
      aud: sa.token_uri,
      exp: now + 3600,
      iat: now
    };

    const encodedHeader = base64url(JSON.stringify(header));
    const encodedClaim = base64url(JSON.stringify(claim));
    const unsignedToken = `${encodedHeader}.${encodedClaim}`;

    const signer = crypto.createSign('RSA-SHA256');
    signer.update(unsignedToken);
    const signature = signer.sign(sa.private_key, 'base64')
      .replace(/=/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');

    const jwt = `${unsignedToken}.${signature}`;

    const postData = new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: jwt
    }).toString();

    const u = new URL(sa.token_uri);
    const req = https.request({
      hostname: u.hostname,
      path: u.pathname,
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(postData)
      }
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        if (res.statusCode === 200) {
          try {
            const json = JSON.parse(data);
            resolve(json.access_token);
          } catch (e) {
            reject(new Error('Failed to parse token response'));
          }
        } else {
          reject(new Error(`Token request failed with status ${res.statusCode}: ${data}`));
        }
      });
    });

    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

async function sendFcm() {
  try {
    const accessToken = await getAccessToken();

    const messagePayload = {
      message: {
        topic: topic,
        notification: {
          title: title,
          body: message
        },
        data: {
          title: title,
          body: message,
          ...(validatedDeepUrl ? { deep_url: validatedDeepUrl } : {})
        },
        android: {
          priority: urgent ? 'HIGH' : 'NORMAL',
          ttl: '86400s',
          notification: {
            channel_id: 'bmu_updates',
            sound: 'default'
          }
        }
      }
    };

    const postData = JSON.stringify(messagePayload);

    const req = https.request({
      hostname: 'fcm.googleapis.com',
      path: `/v1/projects/${projectId}/messages:send`,
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
      }
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        if (res.statusCode === 200) {
          console.log('Successfully sent FCM message:', data);
        } else {
          console.error(`FCM send failed with status ${res.statusCode}: ${data}`);
          process.exit(1);
        }
      });
    });

    req.on('error', (err) => {
      console.error('Request error:', err);
      process.exit(1);
    });

    req.write(postData);
    req.end();

  } catch (err) {
    console.error('Error sending notification:', err);
    process.exit(1);
  }
}

sendFcm();

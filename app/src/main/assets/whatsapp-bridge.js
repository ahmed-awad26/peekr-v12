/**
 * Peekr - WhatsApp Bridge Server
 * بيشتغل على Node.js محلي على الجهاز عبر Termux
 * بيوصل التطبيق بواتساب ويب
 */

const { Client, LocalAuth } = require('whatsapp-web.js');
const WebSocket = require('ws');
const qrcode = require('qrcode');

const PORT = 3001;
const wss = new WebSocket.Server({ port: PORT });
console.log(`WhatsApp Bridge running on ws://localhost:${PORT}`);

let waClient = null;
let connectedSockets = new Set();

// ==============================
// WebSocket Server
// ==============================
wss.on('connection', (ws) => {
    console.log('App connected to bridge');
    connectedSockets.add(ws);

    ws.on('message', (data) => {
        try {
            const msg = JSON.parse(data.toString());
            handleAppMessage(msg, ws);
        } catch (e) {
            console.error('Error parsing message:', e);
        }
    });

    ws.on('close', () => {
        connectedSockets.delete(ws);
        console.log('App disconnected from bridge');
    });

    // لو الكلاينت متصل أصلاً، بلغ التطبيق
    if (waClient && waClient.info) {
        ws.send(JSON.stringify({ type: 'ready', data: { name: waClient.info.pushname } }));
    }
});

// ==============================
// معالجة أوامر التطبيق
// ==============================
function handleAppMessage(msg, ws) {
    switch (msg.action) {
        case 'getQR':
            initWhatsApp();
            break;
        case 'logout':
            if (waClient) {
                waClient.logout().then(() => {
                    broadcast({ type: 'disconnected' });
                });
            }
            break;
    }
}

// ==============================
// تهيئة واتساب
// ==============================
function initWhatsApp() {
    if (waClient) {
        waClient.destroy();
        waClient = null;
    }

    waClient = new Client({
        authStrategy: new LocalAuth({ dataPath: './wa_session' }),
        puppeteer: {
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox']
        }
    });

    waClient.on('qr', async (qr) => {
        console.log('QR Generated');
        try {
            // تحويل QR لـ base64 string
            const qrString = await qrcode.toString(qr, { type: 'utf8' });
            broadcast({ type: 'qr', data: qr });
        } catch (e) {
            console.error('QR error:', e);
        }
    });

    waClient.on('ready', () => {
        console.log('WhatsApp Connected!');
        broadcast({
            type: 'ready',
            data: { name: waClient.info?.pushname || 'WhatsApp' }
        });
    });

    waClient.on('message', async (message) => {
        try {
            if (message.fromMe) return; // تجاهل رسائلي أنا

            const chat = await message.getChat();
            const contact = await message.getContact();

            broadcast({
                type: 'message',
                data: {
                    from: message.from,
                    body: message.body,
                    timestamp: message.timestamp,
                    isGroup: chat.isGroup,
                    chatName: chat.name || contact.pushname || message.from,
                    senderName: contact.pushname || contact.name || message.from,
                }
            });
        } catch (e) {
            console.error('Message error:', e);
        }
    });

    waClient.on('disconnected', (reason) => {
        console.log('WhatsApp disconnected:', reason);
        broadcast({ type: 'disconnected', data: { reason } });
        waClient = null;
    });

    waClient.on('auth_failure', (msg) => {
        broadcast({ type: 'error', message: 'فشل التحقق: ' + msg });
    });

    waClient.initialize().catch(e => {
        broadcast({ type: 'error', message: e.message });
    });
}

// ==============================
// إرسال للتطبيق
// ==============================
function broadcast(data) {
    const msg = JSON.stringify(data);
    connectedSockets.forEach(ws => {
        if (ws.readyState === WebSocket.OPEN) {
            ws.send(msg);
        }
    });
}

// ==============================
// تشغيل أوتوماتيك لو في session محفوظة
// ==============================
initWhatsApp();

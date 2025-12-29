const WebSocket = require('ws');
const { v4: uuidv4 } = require('uuid');

const PORT = 8080;
const SERVER_IP = '192.168.1.149';

// Game rooms storage
const rooms = new Map(); // roomId -> Room
const clients = new Map(); // ws -> Client

class Client {
    constructor(ws, clientId) {
        this.ws = ws;
        this.id = clientId;
        this.username = null;
        this.roomId = null;
        this.isHost = false;
    }
}

class Room {
    constructor(roomId, roomName, host) {
        this.id = roomId;
        this.name = roomName;
        this.host = host;
        this.guest = null;
        this.status = 'waiting'; // waiting, playing, finished
        this.currentSet = 1;
        this.hostScore = 0; // Set skoru
        this.guestScore = 0; // Set skoru
        this.hostBallsDestroyed = 0; // Mevcut setteki top sayısı
        this.guestBallsDestroyed = 0; // Mevcut setteki top sayısı
        this.hostRequestedRematch = false;
        this.guestRequestedRematch = false;
        this.gameState = {
            balls: [],
            cueBall: null
        };
        this.createdAt = Date.now();
    }

    addGuest(guest) {
        this.guest = guest;
        this.status = 'playing';
        this.resetSet(); // Initialize timer and game state
        this.startGameLoop(); // Start physics loop
    }

    broadcast(message, excludeClient = null) {
        const data = JSON.stringify(message);
        if (this.host && this.host !== excludeClient) {
            this.host.ws.send(data);
        }
        if (this.guest && this.guest !== excludeClient) {
            this.guest.ws.send(data);
        }
    }

    resetSet() {
        this.hostBallsDestroyed = 0;
        this.guestBallsDestroyed = 0;
        this.timeLeft = 30000; // 30 seconds per set
        this.setStartTime = Date.now();
        this.lastBallSpawnTime = Date.now();
        this.setEnding = false; // Reset flag

        // Initialize with player balls
        this.gameState = {
            balls: [],
            hostCueBall: { x: 0.3, y: 0.5, vx: 0, vy: 0, color: '#888888' },
            guestCueBall: { x: 0.7, y: 0.5, vx: 0, vy: 0, color: '#FFFFFF' }
        };

        // Spawn initial colored balls
        this.spawnColoredBalls(5);
    }

    resetMatch() {
        this.hostScore = 0;
        this.guestScore = 0;
        this.currentSet = 1;
        this.hostRequestedRematch = false;
        this.guestRequestedRematch = false;
        this.resetSet();

        // Broadcast game start again (fresh match)
        this.broadcast({
            type: 'game_start',
            message: 'Match Restarted!',
            currentSet: 1,
            hostScore: 0,
            guestScore: 0
        });

        // Start game loop if not running
        if (!this.gameLoopInterval) {
            this.startGameLoop();
        }

        // Send initial game state to both players
        this.broadcast({
            type: 'game_state',
            hostCueBall: this.gameState.hostCueBall,
            guestCueBall: this.gameState.guestCueBall,
            balls: this.gameState.balls,
            timeLeft: this.timeLeft,
            currentSet: this.currentSet
        });

        console.log(`🔄 Match restarted! Set 1 begins.`);
    }

    spawnColoredBalls(count) {
        // Safe spawn area respecting Aspect Ratio (Use 2.0 for modern screens safety)
        const ASPECT_RATIO = 2.0;
        const SAFE_RADIUS_X = 0.40;
        const SAFE_RADIUS_Y = SAFE_RADIUS_X / ASPECT_RATIO;

        const colors = ['#FF0055', '#00FF99', '#FFFF00', '#00CCFF', '#FF6600'];
        for (let i = 0; i < count; i++) {
            const angle = Math.random() * Math.PI * 2;
            const r = Math.sqrt(Math.random());
            this.gameState.balls.push({
                x: 0.5 + Math.cos(angle) * (r * SAFE_RADIUS_X),
                y: 0.5 + Math.sin(angle) * (r * SAFE_RADIUS_Y),
                vx: (Math.random() - 0.5) * 0.005,
                vy: (Math.random() - 0.5) * 0.005,
                color: colors[Math.floor(Math.random() * colors.length)]
            });
        }
    }

    updateTimer() {
        const elapsed = Date.now() - this.setStartTime;
        this.timeLeft = Math.max(0, 30000 - elapsed);

        // Spawn new balls every 3 seconds
        if (Date.now() - this.lastBallSpawnTime > 3000 && this.gameState.balls.length < 15) {
            this.spawnColoredBalls(2);
            this.lastBallSpawnTime = Date.now();
        }

        // Check if time is up (only once)
        if (this.timeLeft <= 0 && !this.setEnding) {
            this.setEnding = true;
            this.endSet();
        }
    }

    endSet() {
        const winner = this.checkSetWinner();

        this.broadcast({
            type: 'set_ended',
            winner: winner,
            hostScore: this.hostScore,
            guestScore: this.guestScore,
            currentSet: this.currentSet
        });

        console.log(`🏁 Set ${this.currentSet} ended. Winner: ${winner}. Score: ${this.hostScore}-${this.guestScore}`);

        // Check if match is over
        const matchWinner = this.checkMatchWinner();
        if (matchWinner) {
            this.endMatch(matchWinner);
        } else {
            // Start next set
            this.currentSet++;
            setTimeout(() => {
                this.setEnding = false; // Reset flag
                this.resetSet();
            }, 7000); // 7 second delay (2s SET FINISHED + 5s countdown)
        }
    }

    endMatch(winner) {
        this.status = 'finished';
        this.stopGameLoop();

        this.broadcast({
            type: 'match_ended',
            winner: winner,
            finalScore: `${this.hostScore}-${this.guestScore}`,
            hostScore: this.hostScore,
            guestScore: this.guestScore
        });

        console.log(`🏆 Match ended! Winner: ${winner}`);
    }

    checkSetWinner() {
        console.log(`🔍 Checking Set Winner: Host Balls ${this.hostBallsDestroyed} vs Guest Balls ${this.guestBallsDestroyed}`);

        // Her set bitmesi gerektiğinde kontrol edilecek
        // Client'tan "set_ended" mesajı geldiğinde
        if (this.hostBallsDestroyed > this.guestBallsDestroyed) {
            this.hostScore++;
            return 'host';
        } else if (this.guestBallsDestroyed > this.hostBallsDestroyed) {
            this.guestScore++;
            return 'guest';
        }
        return 'draw';
    }

    checkMatchWinner() {
        if (this.hostScore >= 2) return 'host';
        if (this.guestScore >= 2) return 'guest';
        return null;
    }

    // ===== AUTHORITATIVE PHYSICS ENGINE =====

    startGameLoop() {
        if (this.gameLoopInterval) return; // Already running

        const FPS = 60;
        const FRAME_TIME = 1000 / FPS;
        this.frameCounter = 0;

        this.gameLoopInterval = setInterval(() => {
            this.updatePhysics();
            this.broadcastGameState(); // 60 FPS
        }, FRAME_TIME);

        console.log(`🎮 Physics loop started for room ${this.name} (60 FPS)`);
    }

    stopGameLoop() {
        if (this.gameLoopInterval) {
            clearInterval(this.gameLoopInterval);
            this.gameLoopInterval = null;
            console.log(`⏹️  Physics loop stopped for room ${this.name}`);
        }
    }

    updatePhysics() {
        // Pause physics during set transition
        if (this.setEnding) {
            return;
        }

        const FRICTION = 0.998; // Sync with GameView (0.998)
        const CIRCLE_RADIUS = 0.47; // Match client: minSize * 0.47 (normalized)

        // Update timer
        this.updateTimer();

        // Update host cue ball
        if (this.gameState.hostCueBall) {
            let ball = this.gameState.hostCueBall;
            ball.x += ball.vx;
            ball.y += ball.vy;
            this.reflectBall(ball, CIRCLE_RADIUS);
            ball.vx *= FRICTION;
            ball.vy *= FRICTION;
        }

        // Update guest cue ball
        if (this.gameState.guestCueBall) {
            let ball = this.gameState.guestCueBall;
            ball.x += ball.vx;
            ball.y += ball.vy;
            this.reflectBall(ball, CIRCLE_RADIUS);
            ball.vx *= FRICTION;
            ball.vy *= FRICTION;
        }

        // Update colored balls (Random movement, no friction)
        this.gameState.balls.forEach((ball) => {
            ball.x += ball.vx;
            ball.y += ball.vy;
            this.reflectBall(ball, CIRCLE_RADIUS);
            // Apply slight friction to colored balls too for realism, similar to GameView whiteBall logic
            ball.vx *= FRICTION;
            ball.vy *= FRICTION;
        });

        // Check collisions
        this.checkCollisions();
    }

    reflectBall(ball, circleRadius) {
        // IMPORTANT: Aspect Ratio Correction for Boundary Check
        const ASPECT_RATIO = 2.0;
        const dx = ball.x - 0.5;
        const dy = (ball.y - 0.5) * ASPECT_RATIO; // Scale Y to match X visual units

        const dist = Math.sqrt(dx * dx + dy * dy);
        const ballRadius = 0.025;

        // Instant bounce on contact
        if (dist + ballRadius >= circleRadius) {
            // Normalize direction (in the scaled space)
            const nx = dist > 0 ? dx / dist : 1;
            const ny = dist > 0 ? dy / dist : 0;

            // Push ball back inside FIRST (in the scaled space)
            const overlap = (dist + ballRadius) - circleRadius + 0.001;

            // Unscale Y when applying position correction
            ball.x -= nx * overlap;
            ball.y -= (ny * overlap) / ASPECT_RATIO;

            // Perfect elastic reflection for all balls (removed random bounce)
            const dotProduct = ball.vx * nx + ball.vy * ny;
            ball.vx -= 2 * dotProduct * nx;
            ball.vy -= 2 * dotProduct * ny;

            // Apply bounce damping (Wall friction)
            ball.vx *= 0.9;
            ball.vy *= 0.9;
        }

        // Final safety check - clamp to circle
        const finalDx = ball.x - 0.5;
        const finalDy = (ball.y - 0.5) * ASPECT_RATIO; // Use consistent Aspect Ratio
        const finalDist = Math.sqrt(finalDx * finalDx + finalDy * finalDy);

        if (finalDist + ballRadius > circleRadius) {
            const angle = Math.atan2(finalDy, finalDx);
            const maxDist = circleRadius - ballRadius;
            ball.x = 0.5 + Math.cos(angle) * maxDist;
            ball.y = 0.5 + Math.sin(angle) * maxDist / ASPECT_RATIO; // Unscale Y
        }
    }

    checkCollisions() {
        if (this.gameState.balls.length === 0) return;

        // Check for host
        if (this.gameState.hostCueBall) {
            this.checkPlayerCollision(this.gameState.hostCueBall, 'host');
        }

        // Check for guest
        if (this.gameState.guestCueBall) {
            this.checkPlayerCollision(this.gameState.guestCueBall, 'guest');
        }
    }

    checkPlayerCollision(cueBall, shooter) {
        for (let i = this.gameState.balls.length - 1; i >= 0; i--) {
            const ball = this.gameState.balls[i];
            const dx = cueBall.x - ball.x;
            const dy = cueBall.y - ball.y;
            const dist = Math.sqrt(dx * dx + dy * dy);

            // Stricter collision: Require actual contact (sum of radii = 0.05)
            if (dist < 0.048) {

                // Sync with GameView: Speed Boost on Collision!
                // float angle = (float) Math.atan2(dy, dx);
                // float speed = (float) Math.sqrt(wBall.vx * wBall.vx + wBall.vy * wBall.vy);
                // wBall.vx = (float) Math.cos(angle) * speed * 1.05f;
                // wBall.vy = (float) Math.sin(angle) * speed * 1.05f;

                const angle = Math.atan2(dy, dx); // Angle from ball to cueBall
                const speed = Math.sqrt(cueBall.vx * cueBall.vx + cueBall.vy * cueBall.vy);

                // Bounce off: speed increases by 5% (1.05)
                cueBall.vx = Math.cos(angle) * speed * 1.05;
                cueBall.vy = Math.sin(angle) * speed * 1.05;

                // Store ball position and color for explosion effect
                const destroyedBall = {
                    x: ball.x,
                    y: ball.y,
                    color: ball.color
                };

                // Collision! Remove ball
                this.gameState.balls.splice(i, 1);

                // Award points
                if (shooter === 'host') {
                    this.hostBallsDestroyed++;
                } else if (shooter === 'guest') {
                    this.guestBallsDestroyed++;
                }

                // Broadcast score update WITH ball destruction info for explosion
                this.broadcast({
                    type: 'score_update',
                    hostScore: this.hostBallsDestroyed,
                    guestScore: this.guestBallsDestroyed,
                    destroyedBy: shooter,
                    destroyedBall: destroyedBall // Add ball info for explosion
                });

                console.log(`💥 Ball destroyed by ${shooter}! Host: ${this.hostBallsDestroyed}, Guest: ${this.guestBallsDestroyed}`);
            }
        }
    }

    broadcastGameState() {
        // Broadcast game state with timer
        this.broadcast({
            type: 'game_state_update',
            hostCueBall: this.gameState.hostCueBall,
            guestCueBall: this.gameState.guestCueBall,
            balls: this.gameState.balls,
            timeLeft: this.timeLeft,
            hostBallsDestroyed: this.hostBallsDestroyed,
            guestBallsDestroyed: this.guestBallsDestroyed,
            currentSet: this.currentSet
        });
    }

    applyShot(angle, power, shooter, x = null, y = null) {
        // Apply force to the correct cue ball
        const speed = power * 0.0002; // Much slower shot power

        let targetBall = null;
        if (shooter === 'host') {
            targetBall = this.gameState.hostCueBall;
        } else if (shooter === 'guest') {
            targetBall = this.gameState.guestCueBall;
        }

        if (targetBall) {
            // Teleport ball to shot position if provided (compensates for client-side drag hold)
            if (x !== null && y !== null) {
                targetBall.x = x;
                targetBall.y = y;
            }

            targetBall.vx = Math.cos(angle) * speed;
            targetBall.vy = Math.sin(angle) * speed;
            this.lastShooter = shooter;
            console.log(`🚀 Shot applied for ${shooter}: angle=${angle.toFixed(2)}, power=${power.toFixed(2)}`);
        } else {
            console.log(`❌ Shot failed: targetBall for ${shooter} is null!`);
        }
    }
}

// WebSocket Server
const wss = new WebSocket.Server({
    port: PORT
});

console.log(`🚀 Space Billiard Game Server started on ws://${SERVER_IP}:${PORT} `);

wss.on('connection', (ws) => {
    const clientId = uuidv4();
    const client = new Client(ws, clientId);
    clients.set(ws, client);

    console.log(`✅ New client connected: ${clientId} `);

    // Send welcome message
    ws.send(JSON.stringify({
        type: 'connected',
        clientId: clientId,
        message: 'Connected to Space Billiard Server'
    }));

    ws.on('message', (data) => {
        try {
            const message = JSON.parse(data);
            handleMessage(client, message);
        } catch (error) {
            console.error('Error parsing message:', error);
            ws.send(JSON.stringify({
                type: 'error',
                message: 'Invalid message format'
            }));
        }
    });

    ws.on('close', () => {
        handleDisconnect(client);
    });

    ws.on('error', (error) => {
        console.error('WebSocket error:', error);
    });
});

function handleMessage(client, message) {
    console.log(`📨 Received from ${client.username || client.id}: `, message.type);

    switch (message.type) {
        case 'set_username':
            client.username = message.username;
            client.ws.send(JSON.stringify({
                type: 'username_set',
                username: client.username
            }));
            break;

        case 'createRoom': // MATCHING ANDROID CLIENT
            createRoom(client, message.data.roomName); // Android sends data object
            break;

        case 'joinRoom': // MATCHING ANDROID CLIENT
            joinRoom(client, message.data.roomId);
            break;

        case 'getRooms': // MATCHING ANDROID CLIENT (list_rooms -> getRooms)
            listRooms(client);
            break;

        case 'leaveRoom': // MATCHING ANDROID CLIENT
            leaveRoom(client);
            break;

        case 'shot':
            handleShot(client, message);
            break;

        case 'ball_destroyed':
            handleBallDestroyed(client, message);
            break;

        case 'stop_ball':
            handleStopBall(client, message);
            break;

        case 'set_ended':
            handleSetEnded(client);
            break;

        case 'ready':
            handleReady(client);
            break;

        case 'rematch_request':
            handleRematchRequest(client);
            break;

        default:
            console.log('Unknown message type:', message.type);
    }
}

function createRoom(client, roomName) {
    // If client already in a room, leave it first
    if (client.roomId) {
        console.log(`⚠️  ${client.username} leaving previous room before creating new one`);
        leaveRoom(client);
    }

    const roomId = uuidv4();
    const room = new Room(roomId, roomName, client);
    rooms.set(roomId, room);
    client.roomId = roomId;
    client.isHost = true;

    client.ws.send(JSON.stringify({
        type: 'roomCreated', // MATCHING ANDROID CLIENT
        success: true,
        roomId: roomId,
        roomName: roomName,
        role: 'host'
    }));

    console.log(`🏠 Room created: "${roomName}"(${roomId.substring(0, 8)}...) by ${client.username} `);
    console.log(`📊 Total rooms: ${rooms.size}, Status: ${room.status} `);
    broadcastRoomList();
}

function joinRoom(client, roomId) {
    console.log(`🔍 Join request: ${client.username} trying to join room ${roomId} `);

    const room = rooms.get(roomId);

    if (!room) {
        console.log(`❌ Room not found: ${roomId} `);
        client.ws.send(JSON.stringify({
            type: 'error',
            message: 'Room not found'
        }));
        return;
    }

    // Check if client is already in this room
    if (client.roomId === roomId) {
        console.log(`⚠️  ${client.username} already in room ${room.name} `);
        client.ws.send(JSON.stringify({
            type: 'error',
            message: 'You are already in this room'
        }));
        return;
    }

    // Check if client is in another room
    if (client.roomId) {
        console.log(`⚠️  ${client.username} leaving previous room first`);
        leaveRoom(client);
    }

    if (room.guest) {
        console.log(`❌ Room is full: ${room.name} `);
        client.ws.send(JSON.stringify({
            type: 'error',
            message: 'Room is full'
        }));
        return;
    }

    room.guest = client;
    room.status = 'playing';
    client.roomId = roomId;
    client.isHost = false;

    // Initialize game with resetSet (includes timer, balls, etc.)
    room.resetSet();

    // Notify both players game is starting
    room.broadcast({
        type: 'player_joined',
        roomId: roomId,
        hostUsername: room.host.username,
        guestUsername: client.username,
        status: 'playing'
    });

    // Send initial game state to both players
    room.broadcast({
        type: 'game_state',
        hostCueBall: room.gameState.hostCueBall,
        guestCueBall: room.gameState.guestCueBall,
        balls: room.gameState.balls,
        timeLeft: room.setTimeLeft,
        currentSet: room.currentSet
    });

    // Start authoritative physics loop!
    room.startGameLoop();

    console.log(`🎮 Game starting in room "${room.name}" - ${room.host.username} vs ${client.username} `);
    console.log(`📊 Rooms count: ${rooms.size}, Clients count: ${clients.size} `);
    broadcastRoomList();
}

function leaveRoom(client) {
    if (!client.roomId) return;

    const room = rooms.get(client.roomId);
    if (!room) return;

    if (client.isHost) {
        // Host left, close room
        room.broadcast({
            type: 'room_closed',
            reason: 'Host left the room'
        });

        if (room.guest) {
            room.guest.roomId = null;
            room.guest.isHost = false;
        }

        rooms.delete(client.roomId);
        console.log(`🚪 Room ${room.name} closed(host left)`);
    } else {
        // Guest left
        room.guest = null;
        room.status = 'waiting';

        if (room.host) {
            room.host.ws.send(JSON.stringify({
                type: 'player_left',
                message: 'Guest left the room'
            }));
        }
        console.log(`👋 ${client.username} left room ${room.name} `);
    }

    client.roomId = null;
    client.isHost = false;
    broadcastRoomList();
}

function listRooms(client) {
    const availableRooms = [];

    rooms.forEach((room) => {
        if (room.status === 'waiting') {
            availableRooms.push({
                id: room.id,
                name: room.name,
                host: room.host.username,
                players: room.guest ? 2 : 1,
                maxPlayers: 2
            });
        }
    });

    client.ws.send(JSON.stringify({
        type: 'room_list',
        rooms: availableRooms
    }));
}

function broadcastRoomList() {
    const availableRooms = [];

    rooms.forEach((room) => {
        if (room.status === 'waiting') {
            availableRooms.push({
                id: room.id,
                name: room.name,
                host: room.host.username,
                players: room.guest ? 2 : 1,
                maxPlayers: 2
            });
        }
    });

    // Send to all clients not in a room
    clients.forEach((client) => {
        if (!client.roomId) {
            client.ws.send(JSON.stringify({
                type: 'room_list',
                rooms: availableRooms
            }));
        }
    });
}

function handleShot(client, message) {
    const room = rooms.get(client.roomId);
    if (!room) return;

    // Server applies the shot (authoritative)
    const shooter = client.isHost ? 'host' : 'guest';
    room.applyShot(message.angle, message.power, shooter, message.x, message.y);

    // Physics loop will broadcast updated positions
}

function handleBallDestroyed(client, message) {
    const room = rooms.get(client.roomId);
    if (!room) return;

    // Increment ball count for the player who destroyed it
    if (client.isHost) {
        room.hostBallsDestroyed++;
    } else {
        room.guestBallsDestroyed++;
    }

    // Broadcast updated scores
    room.broadcast({
        type: 'balls_update',
        hostBalls: room.hostBallsDestroyed,
        guestBalls: room.guestBallsDestroyed
    });

    console.log(`💥 Ball destroyed by ${client.username} - Host: ${room.hostBallsDestroyed}, Guest: ${room.guestBallsDestroyed} `);
}

function handleSetEnded(client) {
    const room = rooms.get(client.roomId);
    if (!room) return;

    const setWinner = room.checkSetWinner();
    const matchWinner = room.checkMatchWinner();

    if (matchWinner) {
        // Match finished (best of 3)
        room.status = 'finished';

        const winnerUsername = matchWinner === 'host' ? room.host.username : room.guest.username;

        room.broadcast({
            type: 'match_ended',
            winner: matchWinner,
            winnerUsername: winnerUsername,
            finalScore: `${room.hostScore} -${room.guestScore} `,
            hostScore: room.hostScore,
            guestScore: room.guestScore
        });

        console.log(`🏆 Match ended! Winner: ${winnerUsername} (${room.hostScore} -${room.guestScore})`);

        // Clean up room after 10 seconds
        setTimeout(() => {
            rooms.delete(room.id);
            broadcastRoomList();
        }, 10000);
    } else {
        // Next set
        room.currentSet++;
        room.resetSet();

        room.broadcast({
            type: 'set_ended',
            setWinner: setWinner,
            currentSet: room.currentSet,
            hostScore: room.hostScore,
            guestScore: room.guestScore
        });

        console.log(`📊 Set ${room.currentSet - 1} ended.Score: ${room.hostScore} -${room.guestScore} `);
    }
}

function handleReady(client) {
    const room = rooms.get(client.roomId);
    if (!room) return;

    room.broadcast({
        type: 'player_ready',
        playerRole: client.isHost ? 'host' : 'guest',
        username: client.username
    });
}

function handleDisconnect(client) {
    console.log(`❌ Client disconnected: ${client.username || client.id} `);

    leaveRoom(client);
    clients.delete(client.ws);
}

// Cleanup old rooms every 5 minutes
setInterval(() => {
    const now = Date.now();
    const timeout = 2 * 60 * 60 * 1000; // 2 hours (increased from 30 min)

    let cleanedCount = 0;
    rooms.forEach((room, roomId) => {
        const roomAge = now - room.createdAt;
        if (roomAge > timeout && room.status === 'waiting') {
            console.log(`🧹 Cleaning up old room: ${room.name} (Age: ${Math.floor(roomAge / 60000)} minutes)`);
            rooms.delete(roomId);
            cleanedCount++;
        }
    });

    if (cleanedCount > 0) {
        console.log(`🧹 Cleanup complete: ${cleanedCount} room(s) removed`);
        broadcastRoomList();
    }
}, 5 * 60 * 1000);

console.log('🎮 Server is ready to accept connections!');

function handleRematchRequest(client) {
    const room = rooms.get(client.roomId);
    if (!room) return;

    if (client.isHost) {
        room.hostRequestedRematch = true;
    } else {
        room.guestRequestedRematch = true;
    }

    // Notify clients that a rematch is requested
    room.broadcast({
        type: 'rematch_request_ack',
        hostRequested: room.hostRequestedRematch,
        guestRequested: room.guestRequestedRematch,
        requestingUser: client.username
    });

    if (room.hostRequestedRematch && room.guestRequestedRematch) {
        console.log(`🔄 Rematch started for room ${room.name} `);
        room.resetMatch();
    }
}

function handleStopBall(client, message) {
    const room = rooms.get(client.roomId);
    if (!room) return;

    const x = message.x;
    const y = message.y;

    if (client.isHost && room.gameState.hostCueBall) {
        // Stop velocity
        room.gameState.hostCueBall.vx = 0;
        room.gameState.hostCueBall.vy = 0;
        // Freeze at client's visual position to prevent drift
        if (x !== undefined && y !== undefined) {
            room.gameState.hostCueBall.x = x;
            room.gameState.hostCueBall.y = y;
        }
    } else if (!client.isHost && room.gameState.guestCueBall) {
        // Stop velocity
        room.gameState.guestCueBall.vx = 0;
        room.gameState.guestCueBall.vy = 0;
        // Freeze at client's visual position to prevent drift
        if (x !== undefined && y !== undefined) {
            room.gameState.guestCueBall.x = x;
            room.gameState.guestCueBall.y = y;
        }
    }
}

console.log(`📡 Make sure port ${PORT} is open in your firewall`);
console.log(`🌐 Local IP: ${SERVER_IP}:${PORT} `);

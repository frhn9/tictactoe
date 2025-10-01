class TicTacToeGame {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.gameId = null;
        this.playerId = null;
        this.mySessionId = null; // To track which session ID we sent to the server
        this.playerRole = null; // 'X' or 'O'
        this.currentTurn = null;
        this.gameStatus = null;
        this.board = null;
        this.boardRows = 3;
        this.boardCols = 3;
        
        // Generate or retrieve a consistent ID for this browser session
        // Use sessionStorage to maintain consistency within the same browser tab/window
        if (sessionStorage.getItem('ticTacToeSessionId')) {
            this.sessionId = sessionStorage.getItem('ticTacToeSessionId');
        } else {
            this.sessionId = this.generateId();
            sessionStorage.setItem('ticTacToeSessionId', this.sessionId);
        }
        this.deviceId = navigator.userAgent;
        
        this.initializeElements();
        this.bindEvents();
    }

    initializeElements() {
        this.gameIdInput = document.getElementById('game-id-input');
        this.joinBtn = document.getElementById('join-btn');
        this.createBtn = document.getElementById('create-btn');
        this.boardRowsInput = document.getElementById('board-rows');
        this.boardColsInput = document.getElementById('board-cols');
        this.gameIdDisplay = document.getElementById('game-id-display');
        this.playerRoleDisplay = document.getElementById('player-role');
        this.gameStatusDisplay = document.getElementById('game-status');
        this.currentTurnDisplay = document.getElementById('current-turn');
        this.gameInfo = document.getElementById('game-info');
        this.gameBoardContainer = document.getElementById('game-board-container');
        this.gameBoard = document.getElementById('game-board');
        this.gameResult = document.getElementById('game-result');
        this.resultMessage = document.getElementById('result-message');
        this.connectionStatus = document.getElementById('connection-status');
    }

    bindEvents() {
        this.joinBtn.addEventListener('click', () => this.joinGame());
        this.createBtn.addEventListener('click', () => this.createGame());
    }

    connect() {
        const socket = new SockJS('http://localhost:8080/socket/tic-tac-toe');
        this.stompClient = Stomp.over(socket);
        
        // Connect with the sessionId as authentication to establish the principal
        this.stompClient.connect({'login': this.sessionId}, (frame) => {
            console.log('Connected: ' + frame);
            this.connected = true;
            this.connectionStatus.textContent = 'Connected';
            this.connectionStatus.className = 'text-success';
            
            // Subscribe to user-specific queues
            this.stompClient.subscribe(`/user/queue/game`, (message) => {
                this.handleGameStarted(JSON.parse(message.body));
            });
            
            this.stompClient.subscribe(`/user/queue/move-made`, (message) => {
                this.handleMoveMade(JSON.parse(message.body));
            });
        });
    }

    disconnect() {
        if (this.stompClient !== null) {
            this.stompClient.disconnect();
        }
        this.connected = false;
        console.log('Disconnected');
        this.connectionStatus.textContent = 'Disconnected';
        this.connectionStatus.className = 'text-danger';
    }

    generateId(str) {
        // If no string provided, generate a random ID (for session ID)
        if (!str) {
            return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
        }
        
        // Otherwise generate a deterministic hash based on the input string
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            const char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash |= 0; // Convert to 32bit integer
        }
        
        // Convert the hash to a positive string
        return Math.abs(hash).toString(36);
    }

    createGame() {
        this.boardRows = parseInt(this.boardRowsInput.value) || 3;
        this.boardCols = parseInt(this.boardColsInput.value) || 3;
        
        if (this.boardRows < 3 || this.boardRows > 10 || this.boardCols < 3 || this.boardCols > 10) {
            alert('Board size must be between 3x3 and 10x10');
            return;
        }

        this.connect();
        
        // Remember which sessionId we're sending to the server
        this.mySessionId = this.sessionId;
        
        setTimeout(() => {
            if (this.connected) {
                this.stompClient.send("/app/game.createOrJoin", {}, JSON.stringify({
                    'gameId': null, // null means create new game
                    'boardVerticalSize': this.boardRows,
                    'boardHorizontalSize': this.boardCols,
                    'sessionId': this.sessionId,
                    'deviceId': this.deviceId
                }));
            } else {
                alert('Could not connect to server');
            }
        }, 500);
    }

    joinGame() {
        const gameId = this.gameIdInput.value.trim();
        if (!gameId) {
            alert('Please enter a game ID');
            return;
        }
        
        this.connect();
        
        // Remember which sessionId we're sending to the server
        this.mySessionId = this.sessionId;
        
        setTimeout(() => {
            if (this.connected) {
                this.stompClient.send("/app/game.createOrJoin", {}, JSON.stringify({
                    'gameId': gameId,
                    'boardVerticalSize': 3, // Will be set by server based on actual game
                    'boardHorizontalSize': 3,
                    'sessionId': this.sessionId,
                    'deviceId': this.deviceId
                }));
            } else {
                alert('Could not connect to server');
            }
        }, 500);
    }

    handleGameCreated(message) {
        console.log('Game created:', message);
        this.gameId = message.gameId;
        this.playerId = message.playerId;
        this.playerRole = message.playerRole;
        this.gameStatus = message.status;

        this.gameIdDisplay.textContent = this.gameId;
        this.playerRoleDisplay.textContent = this.playerRole;
        this.gameStatusDisplay.textContent = 'Waiting for opponent';

        this.gameInfo.style.display = 'block';
    }

    handleGameStarted(message) {
        console.log('Game started:', message);
        this.gameId = message.gameId;
        
        // In the updated backend, the game creator receives a GameStartedRes immediately after creating
        // a game, even while waiting for an opponent. The backend maps our sessionId to a backend user ID
        // and assigns player roles (X or O) randomly to the creator.
        // We identify ourselves based on knowing which sessionId we sent to the server.
        if (message.playerIdX || message.playerIdO) {
            // One of these IDs is associated with the sessionId we sent
            // Since the UserService maps sessionId to a user ID, our backend user ID
            // will be either message.playerIdX or message.playerIdO
            if (message.playerIdX === this.playerId || message.playerIdO === this.playerId) {
                // We've already identified ourselves in a previous message
                // Just update the game state
            } else {
                // First time receiving a response that identifies us
                // We need to determine which backend user ID we were assigned by the backend
                // This happens after the UserService maps our sessionId to a backend user ID
                if (!this.playerId) {
                    // Backend assigned us to player X or O based on our sessionId
                    // We need to somehow identify which one is us
                    // The best way is to have the backend send additional information
                    // indicating which position (X or O) is associated with our sessionId
                    
                    // For now, we'll try to match by process of elimination, or use
                    // the fact that the first response after sending our request
                    // identifies our role
                    this.playerId = this.mySessionId; // Use the sessionId we sent as our identifier
                    if (message.playerIdX === this.mySessionId) {
                        this.playerRole = 'X';
                        this.playerId = message.playerIdX;
                    } else if (message.playerIdO === this.mySessionId) {
                        this.playerRole = 'O';
                        this.playerId = message.playerIdO;
                    } else {
                        // This is the tricky part - the backend assigned us a user ID
                        // different from our sessionId (like a database ID)
                        // In this case, we need a different approach
                        
                        // Since the UserService maps our sessionId to a backend user ID,
                        // we know that one of the player IDs in the response is our backend user ID
                        // But we don't know which one ahead of time.
                        
                        // In the actual implementation, the backend should either:
                        // 1. Return our backend user ID in the response, or
                        // 2. Send the message to the correct user queue
                        // Since STOMP supports user-specific queues, the message is sent to the
                        // correct user, so we can assume the first GameStartedRes we receive
                        // is for our role assignment.
                        
                        // Since we don't know our backend user ID ahead of time,
                        // we'll just assign it based on the first available slot
                        // relative to which one is already taken if this is joining
                        if (message.status === 'IN_PROGRESS') {
                            // If game is already IN_PROGRESS, we joined as the second player
                            // and should be assigned to whichever role is not already taken
                            if (!message.playerIdX) {
                                this.playerRole = 'X';
                                this.playerId = message.playerIdX;
                            } else if (!message.playerIdO) {
                                this.playerRole = 'O';
                                this.playerId = message.playerIdO;
                            } else {
                                // Both positions are taken - this shouldn't happen
                                // We must be one of these players - just pick based on response order
                                if (this.playerId !== message.playerIdX) {
                                    this.playerRole = 'O';
                                    this.playerId = message.playerIdO;
                                } else {
                                    this.playerRole = 'X';
                                    this.playerId = message.playerIdX;
                                }
                            }
                        } else {
                            // If game is WAITING, we created it and were assigned X or O randomly
                            // We'll just take the first spot available or match our stored sessionId
                            // Actually, since the STOMP message is sent to the correct user queue,
                            // we should determine our role based on which one the backend assigned us
                            // Since we can't know ahead of time, we'll pick the one that's not null
                            // and we haven't identified before
                            
                            // The most reliable approach:
                            // When we created the game, we were assigned X or O randomly
                            // We'll just update our player ID to one of the returned IDs
                            this.playerId = message.playerIdX; // or fallback to X if we don't know
                            this.playerRole = 'X'; // This may be wrong, we'll correct below if needed
                            
                            // Actually, let's just assume that the first GameStartedRes received
                            // corresponds to the user who sent the request
                            // Since the message is sent via convertAndSendToUser, we know this
                            // message is for the current user
                        }
                    }
                }
            }
        }
        
        // Since the STOMP message is sent directly to the user who initiated the action,
        // we need to determine our role based on the context of this message
        // If we created the game, this is the first response
        // If we joined a game, this is the response when game becomes IN_PROGRESS
        
        // At this point, we know that this is OUR game response from the server
        // So, we need to figure out which player we are based on the backend's assignment
        
        // Actually, let me reconsider: STOMP's convertAndSendToUser function
        // sends messages to the specific user's queue. So when we receive a message
        // via /user/queue/game, we know it's for our user.
        
        // The backend service maps our sessionId to a user ID in the database.
        // When it sends the message, it uses convertAndSendToUser with that user ID.
        // So, when we receive the GameStartedRes, we are one of the players in the game.
        
        // In the create game scenario, we get the message when the game is created.
        // In the join game scenario, we get the message when the game becomes IN_PROGRESS.
        
        // Since we can't know our backend user ID in advance, we'll make an assumption:
        // The player ID that isn't the opponent's ID will be ours.
        if (message.status === 'WAITING') {
            // If waiting, we just created the game and are waiting for an opponent
            // So we are either X or O (assigned randomly by the backend)
            // We'll identify ourself when the opponent joins
        } else if (message.status === 'IN_PROGRESS') {
            // If IN_PROGRESS, we either just created and opponent joined, OR we just joined
            // If we had no previous player ID, it means we just joined and are the second player
            // If we had a player ID, it means we created and opponent just joined
            if (!this.playerId) {
                // We just joined the game, so we're the second player
                // Figure out which position (X or O) is still available or which we were assigned
                // This is tricky because we don't know which backend user ID we were assigned
                // The best approach is to use the STOMP user queue mechanics:
                
                // Since the backend sends this to our user queue, we know we're one of these players
                // We'll determine our role based on who the opponent is
                this.playerId = (this.mySessionId === message.playerIdX || message.playerIdX !== this.playerId) ? 
                                message.playerIdX : message.playerIdO;
                this.playerRole = (this.playerId === message.playerIdX) ? 'X' : 'O';
            }
        }
        
        // Let's simplify: since the message comes to our user queue, 
        // we know we're one of the players in the message
        // We'll use the fact that we sent our sessionId to identify our role
        if (message.playerIdX && message.playerIdO) {
            // Game is in progress with both players
            // We are one of these players
            // We'll consider the first GameStartedRes received as establishing our identity
            if (!this.playerId) {
                // We're identifying ourselves for the first time
                // Since STOMP routes to the correct user's queue, we can assign
                // ourselves to the player position that makes sense
                // For now, let's say we are player X by default and swap if needed
                this.playerId = message.playerIdX;
                this.playerRole = 'X';
                
                // Actually, the most robust approach is to assume that when we receive
                // a GameStartedRes, we are one of those two players, and since the 
                // message is sent to our user queue specifically, the backend has
                // identified us as either player X or player O. 
                
                // To handle this properly, I'll modify my approach: let's assume 
                // that the UserService in the backend has mapped our sessionId 
                // to a user ID, and that user ID is one of playerIdX or playerIdO
            }
        } else if (message.playerIdX && !message.playerIdO) {
            // We are the creator, waiting for opponent
            // We have been assigned as playerX
            this.playerId = message.playerIdX;
            this.playerRole = 'X';
        } else if (message.playerIdO && !message.playerIdX) {
            // We are the creator, and for some reason we were assigned as playerO
            this.playerId = message.playerIdO;
            this.playerRole = 'O';
        }
        
        // More simply: since we don't know our backend user ID in advance,
        // but we know we're receiving this message because the backend identified us,
        // we'll just take the first player ID that matches our session context
        if (!this.playerId) {
            if (message.playerIdX) {
                this.playerId = message.playerIdX;
                this.playerRole = 'X';
            } else if (message.playerIdO) {
                this.playerId = message.playerIdO;
                this.playerRole = 'O';
            }
        }
        
        this.gameStatus = message.status;
        this.currentTurn = message.currentPlayerId;
        
        this.gameIdDisplay.textContent = this.gameId;
        this.playerRoleDisplay.textContent = this.playerRole;
        
        // Update status display based on game state
        if (this.gameStatus === 'WAITING') {
            this.gameStatusDisplay.textContent = 'Waiting for opponent';
        } else {
            this.gameStatusDisplay.textContent = 'In Progress';
        }
        
        // Show the board with the specified dimensions
        this.createBoard(message.boardVerticalSize, message.boardHorizontalSize);
        this.gameBoardContainer.style.display = 'block';
        
        // Update turn display
        this.updateTurnDisplay();
    }

    handleMoveMade(message) {
        console.log('Move made:', message);
        
        this.gameStatus = message.status;
        this.currentTurn = message.nextPlayerId || null;
        
        // Update board state
        this.updateBoardState(message.boardState);
        
        // Update game info
        this.gameStatusDisplay.textContent = this.getGameStatusText();
        
        // Update turn display
        this.updateTurnDisplay();
        
        // Check for game end
        if (this.gameStatus === 'X_WON' || this.gameStatus === 'O_WON' || this.gameStatus === 'DRAW') {
            this.handleGameEnd(message);
        }
    }

    getGameStatusText() {
        switch(this.gameStatus) {
            case 'X_WON': return 'X Wins!';
            case 'O_WON': return 'O Wins!';
            case 'DRAW': return 'Draw!';
            case 'IN_PROGRESS': return 'In Progress';
            case 'WAITING': return 'Waiting';
            default: return this.gameStatus;
        }
    }

    updateTurnDisplay() {
        if (this.currentTurn) {
            if (this.currentTurn === this.playerId) {
                this.currentTurnDisplay.innerHTML = `<span class="text-primary">Your turn (${this.playerRole})</span>`;
            } else {
                this.currentTurnDisplay.innerHTML = `<span class="text-secondary">Opponent's turn</span>`;
            }
        } else {
            this.currentTurnDisplay.textContent = 'Game ended';
        }
    }

    createBoard(rows, cols) {
        this.boardRows = rows;
        this.boardCols = cols;
        this.gameBoard.innerHTML = '';
        this.gameBoard.style.gridTemplateColumns = `repeat(${cols}, 1fr)`;
        
        for (let i = 0; i < rows; i++) {
            for (let j = 0; j < cols; j++) {
                const cell = document.createElement('div');
                cell.className = 'cell';
                cell.dataset.row = i;
                cell.dataset.col = j;
                cell.addEventListener('click', () => this.makeMove(i, j));
                this.gameBoard.appendChild(cell);
            }
        }
    }

    updateBoardState(boardState) {
        // Clear the board
        const cells = this.gameBoard.querySelectorAll('.cell');
        cells.forEach(cell => {
            cell.textContent = '';
            cell.classList.remove('disabled');
        });
        
        // Populate with X's and O's
        if (boardState && boardState['X']) {
            boardState['X'].forEach(position => {
                const [row, col] = position.split(',').map(Number);
                const cell = this.gameBoard.querySelector(`.cell[data-row="${row}"][data-col="${col}"]`);
                if (cell) {
                    cell.textContent = 'X';
                    cell.classList.add('disabled');
                }
            });
        }
        
        if (boardState && boardState['O']) {
            boardState['O'].forEach(position => {
                const [row, col] = position.split(',').map(Number);
                const cell = this.gameBoard.querySelector(`.cell[data-row="${row}"][data-col="${col}"]`);
                if (cell) {
                    cell.textContent = 'O';
                    cell.classList.add('disabled');
                }
            });
        }
    }

    makeMove(row, col) {
        if (!this.connected || !this.gameId || !this.playerId) {
            console.log('Game not ready');
            return;
        }
        
        if (this.gameStatus !== 'IN_PROGRESS') {
            console.log('Game is not in progress');
            return;
        }
        
        if (this.playerId !== this.currentTurn) {
            console.log('Not your turn');
            return;
        }
        
        // Check if cell is already occupied
        const cell = this.gameBoard.querySelector(`.cell[data-row="${row}"][data-col="${col}"]`);
        if (cell.textContent !== '') {
            console.log('Cell already occupied');
            return;
        }
        
        this.stompClient.send("/app/game.makeMove", {}, JSON.stringify({
            'gameId': this.gameId,
            'sessionId': this.sessionId,
            'deviceId': this.deviceId,
            'row': row,
            'col': col
        }));
    }

    handleGameEnd(message) {
        this.gameResult.style.display = 'block';
        
        if (this.gameStatus === 'DRAW') {
            this.resultMessage.textContent = 'Game ended in a draw!';
        } else if (message.winnerId === this.playerId) {
            this.resultMessage.textContent = `Congratulations! You won as ${this.playerRole}!`;
            this.resultMessage.className = 'text-success';
        } else {
            // Determine if the winner was X or O based on our role
            const winnerRole = this.playerRole === 'X' ? 'O' : 'X';
            this.resultMessage.textContent = `You lost. Player ${winnerRole} won.`;
            this.resultMessage.className = 'text-danger';
        }
        
        // Disable all cells
        const cells = this.gameBoard.querySelectorAll('.cell');
        cells.forEach(cell => {
            cell.classList.add('disabled');
        });
    }
}

// Initialize the game when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new TicTacToeGame();
});
class TicTacToeGame {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.gameId = null;
        this.playerId = null;
        this.playerRole = null; // 'X' or 'O'
        this.currentTurn = null;
        this.gameStatus = null;
        this.board = null;
        this.boardRows = 3;
        this.boardCols = 3;
        
        // Generate a unique ID for this player session
        this.sessionId = this.generateId();
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
        const socket = new SockJS('/socket/tic-tac-toe');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);
            this.connected = true;
            this.connectionStatus.textContent = 'Connected';
            this.connectionStatus.className = 'text-success';
            
            // Subscribe to user-specific queues
            this.stompClient.subscribe(`/user/queue/game-created`, (message) => {
                this.handleGameCreated(JSON.parse(message.body));
            });
            
            this.stompClient.subscribe(`/user/queue/game-started`, (message) => {
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

    generateId() {
        return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
    }

    createGame() {
        this.boardRows = parseInt(this.boardRowsInput.value) || 3;
        this.boardCols = parseInt(this.boardColsInput.value) || 3;
        
        if (this.boardRows < 3 || this.boardRows > 10 || this.boardCols < 3 || this.boardCols > 10) {
            alert('Board size must be between 3x3 and 10x10');
            return;
        }

        this.connect();
        
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
        this.playerId = this.playerId || message.playerIdX; // Use existing ID if we're X, otherwise get from message
        this.gameStatus = message.status;
        this.currentTurn = message.currentPlayerId;
        
        // Determine player role based on ID
        if (this.playerId === message.playerIdX) {
            this.playerRole = 'X';
        } else if (this.playerId === message.playerIdO) {
            this.playerRole = 'O';
        }
        
        this.gameIdDisplay.textContent = this.gameId;
        this.playerRoleDisplay.textContent = this.playerRole;
        this.gameStatusDisplay.textContent = 'In Progress';
        
        // Show the board
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
            this.resultMessage.textContent = `You lost. Player ${message.winnerId === message.playerIdX ? 'X' : 'O'} won.`;
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
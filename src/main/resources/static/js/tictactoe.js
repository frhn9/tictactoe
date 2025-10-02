class TicTacToeGame {
    constructor() {
        this.stompClient = null;
        this.sessionId = this.generateSessionId();
        this.gameId = null;
        this.playerRole = null;
        this.currentTurn = null;
        this.gameBoard = null;
        this.boardVerticalSize = 3;
        this.boardHorizontalSize = 3;
        
        this.initializeElements();
        this.setupEventListeners();
        this.connect();
    }
    
    initializeElements() {
        this.initialScreen = document.getElementById('initialScreen');
        this.gameScreen = document.getElementById('gameScreen');
        this.gameOverScreen = document.getElementById('gameOverScreen');
        
        this.createGameForm = document.getElementById('createGameForm');
        this.joinGameForm = document.getElementById('joinGameForm');
        this.quitGameBtn = document.getElementById('quitGameBtn');
        this.playAgainBtn = document.getElementById('playAgainBtn');
        
        this.gameIdDisplay = document.getElementById('currentGameId');
        this.gameStatus = document.getElementById('gameStatus');
        this.playerSymbol = document.getElementById('playerSymbol');
        this.currentTurnDisplay = document.getElementById('currentTurn');
        this.gameBoardContainer = document.getElementById('gameBoardContainer');
        this.gameResult = document.getElementById('gameResult');
        
        this.boardVerticalSizeInput = document.getElementById('boardVerticalSize');
        this.boardHorizontalSizeInput = document.getElementById('boardHorizontalSize');
        this.joinBoardVerticalSizeInput = document.getElementById('joinBoardVerticalSize');
        this.joinBoardHorizontalSizeInput = document.getElementById('joinBoardHorizontalSize');
    }
    
    setupEventListeners() {
        this.createGameForm.addEventListener('submit', (e) => {
            e.preventDefault();
            this.boardVerticalSize = parseInt(this.boardVerticalSizeInput.value);
            this.boardHorizontalSize = parseInt(this.boardHorizontalSizeInput.value);
            this.createGame();
        });
        
        this.joinGameForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const gameId = document.getElementById('gameId').value;
            this.joinGame(gameId);
        });
        
        this.quitGameBtn.addEventListener('click', () => {
            this.quitGame();
        });
        
        this.playAgainBtn.addEventListener('click', () => {
            this.resetGame();
        });
    }
    
    generateSessionId() {
        // Generate a simple session ID based on current time and random value
        return 'sess_' + Date.now() + '_' + Math.floor(Math.random() * 1000000);
    }

    connect() {
        // Include session ID as a query parameter
        const socket = new SockJS('/socket/tic-tac-toe?sessionId=' + this.sessionId);
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);
            
            // Subscribe to session-specific topics for game notifications
            this.stompClient.subscribe('/topic/session/' + this.sessionId, (message) => {
                console.log('Received game notification:', message.body);
                const data = JSON.parse(message.body);
                
                // The backend now sends all game notifications to the session topic
                // Check if it's a game status update (game creation/join) or move notification
                if (data.hasOwnProperty('currentTurnSessionId') && data.hasOwnProperty('boardVerticalSize')) {
                    // This is a game start notification
                    this.handleGameNotification(data);
                } else if (data.hasOwnProperty('nextPlayerId')) {
                    // This is a move notification
                    this.handleMoveMade(data);
                } else {
                    // Handle other types of messages if needed
                    console.log('Unknown message type received:', data);
                }
            });
        });
    }
    
    createGame() {
        const gameData = {
            gameId: null, // null means create new game
            boardVerticalSize: this.boardVerticalSize,
            boardHorizontalSize: this.boardHorizontalSize,
            sessionId: this.sessionId
        };
        
        console.log('Sending create game request:', gameData);
        this.stompClient.send("/app/game.createOrJoin", {}, JSON.stringify(gameData));
    }
    
    joinGame(gameId) {
        const gameData = {
            gameId: gameId,
            boardVerticalSize: 0, // Will be ignored on the backend when joining existing game
            boardHorizontalSize: 0, // Will be ignored on the backend when joining existing game
            sessionId: this.sessionId
        };
        
        console.log('Sending join game request:', gameData);
        this.stompClient.send("/app/game.createOrJoin", {}, JSON.stringify(gameData));
    }
    
    makeMove(row, col) {
        if (this.playerRole !== this.currentTurn) {
            // Not the player's turn
            console.log('Not your turn!');
            return;
        }
        
        const moveData = {
            gameId: this.gameId,
            sessionId: this.sessionId,
            row: row,
            col: col
        };
        
        console.log('Sending move:', moveData);
        this.stompClient.send("/app/game.makeMove", {}, JSON.stringify(moveData));
    }
    
    handleGameNotification(data) {
        this.gameId = data.gameId;
        
        // Determine player role based on session ID
        if (data.sessionIdX === this.sessionId) {
            this.playerRole = 'X';
        } else if (data.sessionIdO === this.sessionId) {
            this.playerRole = 'O';
        } else {
            // This should not happen in a valid game - log an error
            console.error('Player session ID does not match either player in the game:', {
                mySessionId: this.sessionId,
                sessionIdX: data.sessionIdX,
                sessionIdO: data.sessionIdO,
                gameId: data.gameId
            });
            return; // Don't proceed if player isn't properly assigned
        }
        
        this.boardVerticalSize = data.boardVerticalSize;
        this.boardHorizontalSize = data.boardHorizontalSize;
        
        if (data.status === 'WAITING') {
            // Game waiting for opponent
            this.showGameScreen();
            this.gameIdDisplay.textContent = this.gameId;
            this.playerSymbol.textContent = this.playerRole;
            this.gameStatus.textContent = 'Waiting for opponent...';
            
            // Initialize empty board state if not already done
            if (!this.gameBoard) {
                this.gameBoard = Array(this.boardVerticalSize).fill().map(() => 
                    Array(this.boardHorizontalSize).fill('')
                );
            }
            
            // Render empty board
            this.renderBoard();
        } else if (data.status === 'IN_PROGRESS') {
            // Game started
            this.currentTurn = data.currentTurnSessionId === this.sessionId ? this.playerRole : 
                              (this.playerRole === 'X' ? 'O' : 'X');
            this.gameStatus.textContent = 'Game in progress';
            this.currentTurnDisplay.textContent = this.currentTurn;
            
            // Show the game screen for this player
            this.showGameScreen();
            
            // Update game ID display
            this.gameIdDisplay.textContent = this.gameId;
            
            // Initialize empty board state
            this.gameBoard = Array(this.boardVerticalSize).fill().map(() => 
                Array(this.boardHorizontalSize).fill('')
            );
            
            this.renderBoard();
        }
    }
    
    handleGameStarted(data) {
        this.gameId = data.gameId;
        this.currentTurn = data.currentTurnSessionId === this.sessionId ? this.playerRole : 
                          (this.playerRole === 'X' ? 'O' : 'X');
        this.boardVerticalSize = data.boardVerticalSize;
        this.boardHorizontalSize = data.boardHorizontalSize;
        
        this.showGameScreen();
        this.gameIdDisplay.textContent = this.gameId;
        this.playerSymbol.textContent = this.playerRole;
        this.gameStatus.textContent = 'Game in progress';
        this.currentTurnDisplay.textContent = this.currentTurn;
        
        // Initialize empty board state
        this.gameBoard = Array(this.boardVerticalSize).fill().map(() => 
            Array(this.boardHorizontalSize).fill('')
        );
        
        this.renderBoard();
    }
    
    handleMoveMade(data) {
        if (this.gameBoard) {
            // Update game board with move
            this.gameBoard[data.row][data.col] = data.playerId === this.sessionId ? this.playerRole : 
                                                 (this.playerRole === 'X' ? 'O' : 'X');
        }
        
        // Update current turn
        if (data.nextPlayerId) {
            this.currentTurn = data.nextPlayerId === this.sessionId ? this.playerRole : 
                              (this.playerRole === 'X' ? 'O' : 'X');
        }
        this.currentTurnDisplay.textContent = this.currentTurn;
        
        this.renderBoard();
        
        if (data.status === 'X_WON' || data.status === 'O_WON') {
            // Game won
            const winnerSymbol = data.winnerId === this.sessionId ? this.playerRole : 
                                (this.playerRole === 'X' ? 'O' : 'X');
            this.showGameOver(`${winnerSymbol} wins!`);
        } else if (data.status === 'DRAW') {
            // Game ended in draw
            this.showGameOver("It's a draw!");
        }
    }
    
    renderBoard() {
        if (!this.gameBoard) return;
        
        this.gameBoardContainer.innerHTML = '';
        
        for (let row = 0; row < this.boardVerticalSize; row++) {
            const rowDiv = document.createElement('div');
            rowDiv.className = 'board-row';
            
            for (let col = 0; col < this.boardHorizontalSize; col++) {
                const cell = document.createElement('div');
                cell.className = 'game-cell';
                cell.dataset.row = row;
                cell.dataset.col = col;
                
                const cellValue = this.gameBoard[row][col];
                if (cellValue) {
                    cell.textContent = cellValue;
                    cell.classList.add(cellValue.toLowerCase());
                }
                
                // Make cell clickable if it's the player's turn and cell is empty
                if (cellValue === '' && this.playerRole === this.currentTurn) {
                    cell.addEventListener('click', () => {
                        this.makeMove(row, col);
                    });
                }
                
                rowDiv.appendChild(cell);
            }
            
            this.gameBoardContainer.appendChild(rowDiv);
        }
    }
    
    showGameScreen() {
        this.initialScreen.style.display = 'none';
        this.gameScreen.style.display = 'block';
        this.gameOverScreen.style.display = 'none';
    }
    
    showGameOver(result) {
        this.gameResult.textContent = result;
        this.initialScreen.style.display = 'none';
        this.gameScreen.style.display = 'none';
        this.gameOverScreen.style.display = 'block';
        
        // Clear game state variables to prevent issues in subsequent games
        this.gameId = null;
        this.playerRole = null;
        this.currentTurn = null;
        this.gameBoard = null;
    }
    
    quitGame() {
        // For now, just reset the game state and go back to initial screen
        this.resetGame();
    }
    
    resetGame() {
        this.gameId = null;
        this.playerRole = null;
        this.currentTurn = null;
        this.gameBoard = null;
        this.boardVerticalSize = 3;
        this.boardHorizontalSize = 3;
        
        this.initialScreen.style.display = 'block';
        this.gameScreen.style.display = 'none';
        this.gameOverScreen.style.display = 'none';
        
        // Reset form values
        this.boardVerticalSizeInput.value = 3;
        this.boardHorizontalSizeInput.value = 3;
        document.getElementById('gameId').value = '';
    }
}

// Initialize the game when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new TicTacToeGame();
});
CREATE TABLE game_history (
    id BIGSERIAL PRIMARY KEY,
    game_id VARCHAR(255) UNIQUE NOT NULL,
    user_id_x VARCHAR(255) NOT NULL,
    user_id_o VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    board_vertical_size INTEGER NOT NULL,
    board_horizontal_size INTEGER NOT NULL,
    board_state TEXT,
    winner_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
package com.multiplayer.tictactoe.entity.redis;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.multiplayer.tictactoe.enums.GameStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a configurable Tic-Tac-Toe game instance stored in Redis for real-time play.
 * It is annotated with @RedisHash to indicate it is a top-level domain object for Redis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "game", timeToLive = 3600L)
public class ActiveGame implements Serializable {

    // Unique ID for the game instance, used as the key in Redis
    @Id
    private String gameId;

    // Configuration for the board size
    private int boardVerticalSize;

    private int boardHorizontalSize;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String boardState;

    private String currentTurnSessionId;

    private String sessionIdX;

    private String sessionIdO;

    private GameStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Flag to track if game history has been saved to prevent duplicate saving
    private Boolean historySaved = false;

    public Boolean isHistorySaved() {
        return historySaved != null && historySaved;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gets the board state as Map<Character, Set<String>>
     * where key is 'X' or 'O', value is set of "row,col" positions
     */
    public Map<Character, Set<String>> getBoardState() {
        if (this.boardState == null || this.boardState.trim().isEmpty()) {
            return initializeEmptyBoardMap();
        }

        try {
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            JavaType mapType = typeFactory.constructMapType(
                    Map.class,
                    typeFactory.constructType(Character.class),
                    typeFactory.constructCollectionType(Set.class, String.class)
            );

            return objectMapper.readValue(this.boardState, mapType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize board state", e);
        }
    }

    /**
     * Sets the board state from Map<Character, Set<String>>
     */
    public void setBoardState(Map<Character, Set<String>> boardMap) {
        try {
            this.boardState = objectMapper.writeValueAsString(boardMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize board state", e);
        }
    }

    /**
     * Initializes an empty board map
     */
    private Map<Character, Set<String>> initializeEmptyBoardMap() {
        Map<Character, Set<String>> emptyBoard = new HashMap<>();
        emptyBoard.put('X', new HashSet<>());
        emptyBoard.put('O', new HashSet<>());
        return emptyBoard;
    }

    /**
     * Gets the character at specific board position
     * Returns null if the position is empty
     */
    public Character getCell(int row, int col) {
        String position = row + "," + col;
        Map<Character, Set<String>> boardMap = getBoardState();

        for (Map.Entry<Character, Set<String>> entry : boardMap.entrySet()) {
            if (entry.getValue().contains(position)) {
                return entry.getKey();
            }
        }

        return null; // Empty cell
    }

    /**
     * Sets the character at specific board position
     * If value is null, removes the position from the board
     */
    public void setCell(int row, int col, Character value) {
        String position = row + "," + col;
        Map<Character, Set<String>> boardMap = getBoardState();

        // Remove from any existing player
        boardMap.get('X').remove(position);
        boardMap.get('O').remove(position);

        // Add to the specified player if not null
        if (value != null) {
            if (value == 'X' || value == 'O') {
                boardMap.get(value).add(position);
            } else {
                throw new IllegalArgumentException("Invalid player character: " + value);
            }
        }

        setBoardState(boardMap);
    }

    /**
     * Gets all occupied positions for a specific player
     */
    public Set<String> getPlayerPositions(Character player) {
        Map<Character, Set<String>> boardMap = getBoardState();
        return boardMap.getOrDefault(player, new HashSet<>());
    }

    /**
     * Gets all occupied positions on the board
     */
    public Set<String> getAllOccupiedPositions() {
        Map<Character, Set<String>> boardMap = getBoardState();
        Set<String> allPositions = new HashSet<>();
        allPositions.addAll(boardMap.get('X'));
        allPositions.addAll(boardMap.get('O'));
        return allPositions;
    }

    /**
     * Checks if a position is occupied
     */
    public boolean isPositionOccupied(int row, int col) {
        return getCell(row, col) != null;
    }

}

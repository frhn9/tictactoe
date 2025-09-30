package com.multiplayer.tictactoe.entity.jpa;

import com.multiplayer.tictactoe.enums.GameStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity to store historical data of completed games.
 * Using JPA entity to persist in PostgreSQL database.
 */
@Entity
@Table(name = "game_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameHistory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String gameId;

    @Column(nullable = false)
    private String userIdX;

    @Column(nullable = false)
    private String userIdO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status; // X_WON, O_WON, or DRAW

    @Column(nullable = false)
    private int boardVerticalSize;

    @Column(nullable = false)
    private int boardHorizontalSize;

    @Column(columnDefinition = "TEXT") // Store as JSON string
    private String boardState;

    @Column(nullable = false)
    private String winnerId; // ID of the winning player or null in case of draw

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Additional fields that might be useful
    @Column
    private LocalDateTime completedAt; // When the game finished
    
    @Column
    private Long durationMs; // Duration of the game in milliseconds
}
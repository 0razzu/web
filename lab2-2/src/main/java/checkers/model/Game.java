package checkers.model;


import com.google.common.collect.Multimap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    private String id;
    private Cell[][] board;
    private Team whoseTurn;
    private Status status;
    private Multimap<Cell, PossibleMove> situation;
    private List<Move> moveList;
    private Move currentMove;
    private List<Cell> killed;
    private Boolean becomeKing;
    private LocalDateTime moveStartTime;
    
    
    public Game(Cell[][] board, Team whoseTurn, Status status, LocalDateTime moveStartTime) {
        this.board = board;
        this.whoseTurn = whoseTurn;
        this.status = status;
        moveList = new ArrayList<>();
        killed = new ArrayList<>();
        becomeKing = false;
        this.moveStartTime = moveStartTime;
    }
}

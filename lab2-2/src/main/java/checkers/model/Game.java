package checkers.model;


import com.google.common.collect.Multimap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    private Cell[][] board;
    private Team whoseTurn;
    private Status status;
    private Multimap<Cell, PossibleMove> situation;
    private List<Move> moveList;
}

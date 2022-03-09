package checkers.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Move {
    private List<Step> steps;
    private boolean haveKilled;
    private Team whoseTurn;
}

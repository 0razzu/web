package checkers.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PossibleMove {
    private Cell dest;
    private Cell foe;
    private CellState state;
}

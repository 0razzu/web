package checkers.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cell {
    private int row;
    private int col;
    private CellState state;
    private Checker checker;
}

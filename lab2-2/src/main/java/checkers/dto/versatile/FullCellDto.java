package checkers.dto.versatile;


import checkers.model.CellState;
import checkers.model.Checker;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FullCellDto {
    private int row;
    private int col;
    private CellState state;
    private Checker checker;
}

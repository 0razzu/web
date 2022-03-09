package checkers.dto.response;


import checkers.model.Cell;
import checkers.model.CellState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PossibleMoveDto {
    private ResponseCellDto to;
    private ResponseCellDto foe;
    private CellState state;
}

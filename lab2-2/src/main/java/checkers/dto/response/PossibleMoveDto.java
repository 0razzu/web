package checkers.dto.response;


import checkers.dto.versatile.CellDto;
import checkers.model.CellState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PossibleMoveDto {
    private CellDto to;
    private CellDto foe;
    private CellState state;
}

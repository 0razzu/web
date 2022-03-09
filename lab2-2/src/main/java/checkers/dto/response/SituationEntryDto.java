package checkers.dto.response;


import checkers.model.Cell;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SituationEntryDto {
    private ResponseCellDto from;
    private List<PossibleMoveDto> moves;
}

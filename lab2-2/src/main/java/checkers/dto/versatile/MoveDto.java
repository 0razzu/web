package checkers.dto.versatile;


import checkers.dto.versatile.CellDto;
import checkers.model.Step;
import checkers.model.Team;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveDto {
    private List<StepDto> steps;
    private boolean haveKilled;
    private Team whoseTurn;
}

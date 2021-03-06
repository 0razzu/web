package checkers.dto.response;


import checkers.dto.versatile.FullCellDto;
import checkers.model.Status;
import checkers.model.Team;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditStateResponse {
    private List<FullCellDto> changedCells;
    private List<SituationEntryDto> situation;
    private Status status;
    private Team whoseTurn;
}

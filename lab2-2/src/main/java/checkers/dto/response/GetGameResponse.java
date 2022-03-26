package checkers.dto.response;


import checkers.dto.versatile.CellDto;
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
public class GetGameResponse {
    private List<FullCellDto> board;
    private Team whoseTurn;
    private Status status;
    private List<SituationEntryDto> situation;
    private List<String> moveList;
    private String currentMove;
    private List<CellDto> killed;
    private boolean becomeKing;
}

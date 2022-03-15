package checkers.dto.response;


import checkers.dto.versatile.FullCellDto;
import checkers.dto.versatile.MoveDto;
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
    private List<List<FullCellDto>> board;
    private Team whoseTurn;
    private Status status;
    private List<SituationEntryDto> situation;
    private List<MoveDto> moveList;
    private MoveDto currentMove;
    private boolean becomeKing;
}
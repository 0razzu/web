package checkers.dto.response;


import checkers.model.Status;
import checkers.model.Team;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGameResponse {
    private String id;
    private List<SituationEntryDto> situation;
    private Status status;
    private Team whoseTurn;
}

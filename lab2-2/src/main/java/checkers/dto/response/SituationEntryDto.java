package checkers.dto.response;


import checkers.dto.versatile.CellDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SituationEntryDto {
    private CellDto from;
    private List<PossibleMoveDto> moves;
}

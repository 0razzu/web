package checkers.dto.response;


import checkers.dto.versatile.FullCellDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MakeStepResponse {
    private List<FullCellDto> changedCells;
    private List<SituationEntryDto> situation;
}

package checkers.dto.request;


import checkers.dto.versatile.FullCellDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGameRequest {
    private List<FullCellDto> board;
    private List<String> moveList;
}

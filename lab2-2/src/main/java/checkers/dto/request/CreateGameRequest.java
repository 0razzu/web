package checkers.dto.request;


import checkers.model.Checker;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGameRequest {
    private Checker[][] board;
    private List<String> moveList;
}

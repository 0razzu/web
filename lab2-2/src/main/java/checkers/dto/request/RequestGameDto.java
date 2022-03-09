package checkers.dto.request;


import checkers.model.Checker;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestGameDto {
    private Checker[][] board;
}

package checkers.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseCreateGameDto {
    private String id;
    private List<SituationEntryDto> situation;
}

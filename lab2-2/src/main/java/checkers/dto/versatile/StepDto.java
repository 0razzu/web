package checkers.dto.versatile;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class StepDto {
    private CellDto from;
    private CellDto to;
}

package checkers.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PossibleMove {
    private Cell dest;
    private Cell foe;
    private CellState state;
    
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PossibleMove that = (PossibleMove) o;
        return Objects.equals(dest, that.dest);
    }
    
    
    @Override
    public int hashCode() {
        return Objects.hash(dest);
    }
}

package checkers.model;


import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum Checker {
    BLACK(Team.BLACK),
    BLACK_KING(Team.BLACK),
    WHITE(Team.WHITE),
    WHITE_KING(Team.WHITE);
    
    
    private final Team team;
}

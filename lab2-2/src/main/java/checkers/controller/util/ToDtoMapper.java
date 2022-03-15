package checkers.controller.util;


import checkers.dto.response.*;
import checkers.dto.versatile.CellDto;
import checkers.dto.versatile.FullCellDto;
import checkers.dto.versatile.MoveDto;
import checkers.dto.versatile.StepDto;
import checkers.model.*;
import com.google.common.collect.Multimap;

import java.util.*;
import java.util.stream.Collectors;


public class ToDtoMapper {
    public static CellDto map(Cell cell) {
        return cell == null?
                null :
                new CellDto(
                        cell.getRow(),
                        cell.getCol()
                );
    }
    
    
    public static PossibleMoveDto map(PossibleMove move) {
        return new PossibleMoveDto(
                map(move.getDest()),
                map(move.getFoe()),
                move.getState()
        );
    }
    
    
    public static List<SituationEntryDto> map(Multimap<Cell, PossibleMove> situation) {
        List<SituationEntryDto> situationDto = new ArrayList<>();
        
        for (Map.Entry<Cell, Collection<PossibleMove>> entry: situation.asMap().entrySet()) {
            Collection<PossibleMove> moves = entry.getValue();
            
            situationDto.add(
                    new SituationEntryDto(
                            ToDtoMapper.map(entry.getKey()),
                            moves.stream().map(ToDtoMapper::map).collect(Collectors.toList())
                    )
            );
        }
        
        return situationDto;
    }
    
    
    public static List<FullCellDto> map(List<Cell> cells) {
        return cells.stream().map(cell -> new FullCellDto(
                cell.getRow(),
                cell.getCol(),
                cell.getState(),
                cell.getChecker())
        ).collect(Collectors.toList());
    }
    
    
    public static CreateGameResponse map(String id, Multimap<Cell, PossibleMove> situation) {
        return new CreateGameResponse(id, map(situation));
    }
    
    
    public static MakeStepResponse map(List<Cell> changedCells, Multimap<Cell, PossibleMove> situation) {
        return new MakeStepResponse(
                map(changedCells),
                map(situation)
        );
    }
    
    
    public static List<List<FullCellDto>> map(Cell[][] board) {
        return Arrays.stream(board).map(row -> Arrays.stream(row).map(cell -> new FullCellDto(
                cell.getRow(),
                cell.getCol(),
                cell.getState(),
                cell.getChecker()
        )).collect(Collectors.toList())).collect(Collectors.toList());
    }
    
    
    public static StepDto map(Step step) {
        return new StepDto(
                map(step.getFrom()),
                map(step.getTo())
        );
    }
    
    
    public static MoveDto map(Move move) {
        return move == null?
                null :
                new MoveDto(
                        move.getSteps().stream().map(ToDtoMapper::map).collect(Collectors.toList()),
                        move.isHaveKilled(),
                        move.getWhoseTurn()
                );
    }
    
    
    public static GetGameResponse map(Game game) {
        return new GetGameResponse(
                map(game.getBoard()),
                game.getWhoseTurn(),
                game.getStatus(),
                map(game.getSituation()),
                game.getMoveList().stream().map(ToDtoMapper::map).collect(Collectors.toList()),
                map(game.getCurrentMove()),
                game.isBecomeKing()
        );
    }
}

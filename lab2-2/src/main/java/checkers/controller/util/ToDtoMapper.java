package checkers.controller.util;


import checkers.dto.response.*;
import checkers.dto.versatile.CellDto;
import checkers.dto.versatile.FullCellDto;
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
        if (situation == null)
            return null;
        
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
    
    
    public static String mapCellToStr(Cell cell) {
        String letters = "abcdefgh";
        
        return String.valueOf(letters.charAt(cell.getCol())) + (cell.getRow() + 1);
    }
    
    
    public static String mapMoveToStr(Move move) {
        if (move == null)
            return null;
        
        String delimiter = move.isHaveKilled()? ":" : "-";
        List<Step> steps = move.getSteps();
        List<Cell> cells = steps.stream().map(Step::getFrom).collect(Collectors.toList());
        cells.add(steps.get(steps.size() - 1).getTo());
        
        return cells.stream().map(ToDtoMapper::mapCellToStr).collect(Collectors.joining(delimiter));
    }
    
    
    public static List<String> mapMoveListToStr(List<Move> moveList) {
        if (moveList == null)
            return null;
        
        int moveListSize = moveList.size();
        List<String> moveListStr = new ArrayList<>(moveListSize / 2 + 1);
        
        for (int i = 0; i < moveListSize; i += 2)
            if (i + 1 < moveListSize)
                moveListStr.add(String.format("%d. %s %s",
                        i / 2 + 1,
                        mapMoveToStr(moveList.get(i)),
                        mapMoveToStr(moveList.get(i + 1))
                ));
            else
                moveListStr.add(String.format("%d. %s",
                        i / 2 + 1,
                        mapMoveToStr(moveList.get(i))
                ));
        
        return moveListStr;
    }
    
    
    public static CreateGameResponse map(String id, Multimap<Cell, PossibleMove> situation, Team whoseTurn, Status status) {
        return new CreateGameResponse(id, map(situation), whoseTurn, status);
    }
    
    
    public static CreateGameResponse map(String id, Cell[][] board, Team whoseTurn, Status status,
                                         Multimap<Cell, PossibleMove> situation, List<Move> moveList) {
        return new CreateGameResponse(
                id,
                map(board),
                whoseTurn,
                status,
                map(situation),
                mapMoveListToStr(moveList)
        );
    }
    
    
    public static EditStateResponse map(List<Cell> changedCells, Multimap<Cell, PossibleMove> situation,
                                              Status status, Team whoseTurn) {
        return new EditStateResponse(
                map(changedCells),
                map(situation),
                status,
                whoseTurn
        );
    }
    
    
    public static List<FullCellDto> map(Cell[][] board) {
        return board == null?
                null :
                Arrays.stream(board).map(row -> Arrays.stream(row).filter(Objects::nonNull).map(cell ->
                        new FullCellDto(
                                cell.getRow(),
                                cell.getCol(),
                                cell.getState(),
                                cell.getChecker()
                        )
                ).collect(Collectors.toList())).collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
    }
    
    
    public static StepDto map(Step step) {
        return new StepDto(
                map(step.getFrom()),
                map(step.getTo())
        );
    }
    
    
    public static GetGameResponse map(Game game) {
        List<Cell> killed = game.getKilled();
        
        return new GetGameResponse(
                game.getId(),
                map(game.getBoard()),
                game.getWhoseTurn(),
                game.getStatus(),
                map(game.getSituation()),
                mapMoveListToStr(game.getMoveList()),
                mapMoveToStr(game.getCurrentMove()),
                killed == null? null : killed.stream().map(ToDtoMapper::map).collect(Collectors.toList()),
                game.getBecomeKing()
        );
    }
    
    
    public static ApplyCurrentMoveResponse map(List<Cell> changedCells, Multimap<Cell, PossibleMove> situation,
                                               Status status, Team whoseTurn, Move lastMove) {
        return new ApplyCurrentMoveResponse(
                map(changedCells),
                map(situation),
                status,
                whoseTurn,
                mapMoveToStr(lastMove)
        );
    }
}

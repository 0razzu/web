package checkers.controller.util;


import checkers.dto.response.PossibleMoveDto;
import checkers.dto.response.ResponseCellDto;
import checkers.dto.response.ResponseCreateGameDto;
import checkers.dto.response.SituationEntryDto;
import checkers.model.Cell;
import checkers.model.PossibleMove;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class DtoMapper {
    public static ResponseCellDto map(Cell cell) {
        return cell == null?
                null :
                new ResponseCellDto(
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
                            DtoMapper.map(entry.getKey()),
                            moves.stream().map(DtoMapper::map).collect(Collectors.toList())
                    )
            );
        }
        
        return situationDto;
    }
    
    
    public static ResponseCreateGameDto map(String id, Multimap<Cell, PossibleMove> situation) {
        return new ResponseCreateGameDto(id, map(situation));
    }
}

package checkers.controller.util;


import checkers.dto.versatile.CellDto;
import checkers.dto.versatile.StepDto;
import checkers.model.Cell;
import checkers.model.Step;


public class FromDtoMapper {
    public static Step map(StepDto request, Cell[][] board) {
        CellDto fromDto = request.getFrom();
        CellDto toDto = request.getTo();
        
        return new Step(
                board[fromDto.getRow()][fromDto.getCol()],
                board[toDto.getRow()][toDto.getCol()]
        );
    }
}

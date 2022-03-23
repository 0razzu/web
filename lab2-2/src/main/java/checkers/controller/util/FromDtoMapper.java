package checkers.controller.util;


import checkers.dto.versatile.CellDto;
import checkers.dto.versatile.StepDto;
import checkers.error.CheckersException;
import checkers.model.Cell;
import checkers.model.Move;
import checkers.model.Step;
import checkers.model.Team;

import java.util.ArrayList;
import java.util.List;

import static checkers.error.CheckersErrorCode.PARSING_ERROR;


public class FromDtoMapper {
    public static Step map(StepDto request, Cell[][] board) {
        CellDto fromDto = request.getFrom();
        CellDto toDto = request.getTo();
        
        return new Step(
                board[fromDto.getRow()][fromDto.getCol()],
                board[toDto.getRow()][toDto.getCol()]
        );
    }
    
    
    public static Cell strToCell(String str, Cell[][] board) throws CheckersException {
        String letters = "abcdefgh";
        int row;
        int col;
        
        try {
            if (str.length() != 2)
                throw new IllegalArgumentException("Wrong cell string length");
            
            row = Integer.parseInt(str.substring(1, 2)) - 1;
            col = letters.indexOf(str.charAt(0));
        } catch (IllegalArgumentException | NullPointerException | IndexOutOfBoundsException e) {
            throw new CheckersException(PARSING_ERROR, str, e);
        }
        
        return board[row][col];
    }
    
    
    public static Move strToMove(String str, Team whoseTurn, Cell[][] board) throws CheckersException {
        List<Cell> cells = new ArrayList<>();
        for (String cellStr: str.split("[:-]"))
            cells.add(strToCell(cellStr, board));
        
        if (cells.size() < 2)
            throw new IllegalArgumentException("Not enough cells in move");
        
        List<Step> steps = new ArrayList<>();
        
        for (int cellIndex = 0; cellIndex < cells.size() - 1; cellIndex++) {
            steps.add(new Step(
                    cells.get(cellIndex),
                    cells.get(cellIndex + 1)
            ));
        }
        
        return new Move(steps, str.contains(":"), whoseTurn);
    }
}

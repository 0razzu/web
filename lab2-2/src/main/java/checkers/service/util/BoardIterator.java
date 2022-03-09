package checkers.service.util;


import checkers.model.Cell;
import lombok.AllArgsConstructor;

import java.util.Iterator;
import java.util.NoSuchElementException;


@AllArgsConstructor
public class BoardIterator implements Iterator<Cell> {
    private Cell[][] board;
    private int row;
    private int col;
    private int directionRow;
    private int directionCol;
    
    
    @Override
    public boolean hasNext() {
        return row > 0 && row < board.length - 1 && col > 0 && col < board.length - 1;
    }
    
    
    @Override
    public Cell next() {
        if (!hasNext())
            throw new NoSuchElementException();
        
        row += directionRow;
        col += directionCol;
        
        return board[row][col];
    }
}

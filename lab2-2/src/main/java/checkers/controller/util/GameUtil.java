package checkers.controller.util;


import checkers.model.*;


public class GameUtil {
    public static final int BOARD_SIZE = 8;
    
    
    public static boolean isPlayCell(int row, int col) {
        return (row + col) % 2 == 0
                && row >= 0 && row < BOARD_SIZE
                && col >= 0 && col < BOARD_SIZE;
    }
    
    
    public static boolean isTurnOf(int row, int col, Game game) {
        Cell[][] board = game.getBoard();
        
        if (board[row][col].getChecker() == null)
            return false;
        
        Checker checker = board[row][col].getChecker();
        Team whoseTurn = game.getWhoseTurn();
        
        return (whoseTurn == Team.WHITE && (checker == Checker.WHITE || checker == Checker.WHITE_KING)) ||
                (whoseTurn == Team.BLACK && (checker == Checker.BLACK || checker == Checker.BLACK_KING));
    }
    
    
    public static boolean areFoes(Cell cell1, Cell cell2) {
        Checker checker1 = cell1.getChecker();
        Checker checker2 = cell2.getChecker();
        
        return cell1.getState() != CellState.KILLED && cell2.getState() != CellState.KILLED
                && checker1 != null && checker2 != null
                && checker1.getTeam() != checker2.getTeam();
    }
    
    
    public static int countEnemies(Team whoseTurn, Cell[][] board) {
        int counter = 0;
        
        for (Cell[] row: board)
            for (Cell cell: row)
                if (cell != null) {
                    Checker checker = cell.getChecker();
                    
                    if (checker != null && checker.getTeam() != whoseTurn)
                        counter++;
                }
        
        return counter;
    }
}

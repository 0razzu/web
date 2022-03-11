package checkers.service;


import checkers.controller.util.DtoMapper;
import checkers.database.dao.GameDao;
import checkers.dto.request.RequestGameDto;
import checkers.dto.response.ResponseCreateGameDto;
import checkers.model.*;
import checkers.service.util.BoardIterator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;


@Service
public class GameService {
    private final GameDao gameDao;
    private static final int BOARD_SIZE = 8;
    
    
    public GameService(GameDao gameDao) {
        this.gameDao = gameDao;
    }
    
    
    private boolean isTurnOf(int row, int col, Game game) {
        Cell[][] board = game.getBoard();
        
        if (board[row][col].getChecker() == null)
            return false;
        
        Checker checker = board[row][col].getChecker();
        Team whoseTurn = game.getWhoseTurn();
        
        return (whoseTurn == Team.WHITE && (checker == Checker.WHITE || checker == Checker.WHITE_KING)) ||
                (whoseTurn == Team.BLACK && (checker == Checker.BLACK || checker == Checker.BLACK_KING));
    }
    
    
    private boolean areFoes(Cell cell1, Cell cell2) {
        Checker checker1 = cell1.getChecker();
        Checker checker2 = cell2.getChecker();
        
        return checker1 != null && checker2 != null && checker1.getTeam() != checker2.getTeam();
    }
    
    
    private void calculateSituation(Game game) {
        Multimap<Cell, PossibleMove> situation = ArrayListMultimap.create();
        Cell[][] board = game.getBoard();
        boolean foundMustBeFilled = false;
        
        for (int row = 0; row < BOARD_SIZE; row++)
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (!isTurnOf(row, col, game))
                    continue;
                
                Cell cellFrom = board[row][col];
                Checker cellFromChecker = cellFrom.getChecker();
                
                if (cellFromChecker == Checker.WHITE_KING || cellFromChecker == Checker.BLACK_KING)
                    for (BoardIterator it: new BoardIterator[]{
                            new BoardIterator(board, row, col, 1, -1),
                            new BoardIterator(board, row, col, 1, 1),
                            new BoardIterator(board, row, col, -1, 1),
                            new BoardIterator(board, row, col, -1, -1)
                    }) {
                        Cell foe = null;
                        
                        while (it.hasNext()) {
                            Cell cellTo = it.next();
                            
                            if (cellTo.getChecker() != null) {
                                if (foe != null) {
                                    situation.put(cellFrom, new PossibleMove(cellTo, foe, CellState.MUST_BE_FILLED));
                                    foundMustBeFilled = true;
                                }
                                
                                else if (!foundMustBeFilled)
                                    situation.put(cellFrom, new PossibleMove(cellTo, null, CellState.CAN_BE_FILLED));
                            }
                            
                            else if (foe == null && areFoes(cellFrom, cellTo))
                                foe = cellTo;
                            
                            else
                                break;
                        }
                    }
                
                else {
                    if (row < BOARD_SIZE - 2) {
                        if (col > 1 && areFoes(cellFrom, board[row + 1][col - 1]) && board[row + 2][col - 2].getChecker() == null) {
                            situation.put(cellFrom, new PossibleMove(board[row + 2][col - 2], board[row + 1][col - 1], CellState.MUST_BE_FILLED));
                            foundMustBeFilled = true;
                        }
                        
                        if (col < BOARD_SIZE - 2 && areFoes(cellFrom, board[row + 1][col + 1]) && board[row + 2][col + 2].getChecker() == null) {
                            situation.put(cellFrom, new PossibleMove(board[row + 2][col + 2], board[row + 1][col + 1], CellState.MUST_BE_FILLED));
                            foundMustBeFilled = true;
                        }
                    }
                    
                    if (row > 1) {
                        if (col > 1 && areFoes(cellFrom, board[row - 1][col - 1]) && board[row - 2][col - 2].getChecker() == null) {
                            situation.put(cellFrom, new PossibleMove(board[row - 2][col - 2], board[row - 1][col - 1], CellState.MUST_BE_FILLED));
                            foundMustBeFilled = true;
                        }
                        
                        if (col < BOARD_SIZE - 2 && areFoes(cellFrom, board[row - 1][col + 1]) && board[row - 2][col + 2].getChecker() == null) {
                            situation.put(cellFrom, new PossibleMove(board[row - 2][col + 2], board[row - 1][col + 1], CellState.MUST_BE_FILLED));
                            foundMustBeFilled = true;
                        }
                    }
                    
                    if (!foundMustBeFilled) {
                        if (cellFromChecker.getTeam() == Team.WHITE && row < BOARD_SIZE - 1) {
                            if (col > 0 && board[row + 1][col - 1].getChecker() == null)
                                situation.put(cellFrom, new PossibleMove(board[row + 1][col - 1], null, CellState.CAN_BE_FILLED));
                            
                            if (col < BOARD_SIZE - 1 && board[row + 1][col + 1].getChecker() == null)
                                situation.put(cellFrom, new PossibleMove(board[row + 1][col + 1], null, CellState.CAN_BE_FILLED));
                        }
                        
                        else if (cellFromChecker.getTeam() == Team.BLACK && row > 0) {
                            if (col > 0 && board[row - 1][col - 1].getChecker() == null)
                                situation.put(cellFrom, new PossibleMove(board[row - 1][col - 1], null, CellState.CAN_BE_FILLED));
                            
                            if (col < BOARD_SIZE - 1 && board[row - 1][col + 1].getChecker() == null)
                                situation.put(cellFrom, new PossibleMove(board[row - 1][col + 1], null, CellState.CAN_BE_FILLED));
                        }
                    }
                }
            }
        
        if (foundMustBeFilled)
            for (Map.Entry<Cell, PossibleMove> entry: situation.entries())
                if (entry.getValue().getState() != CellState.MUST_BE_FILLED)
                    situation.remove(entry.getKey(), entry.getValue());
        
        game.setSituation(situation);
    }
    
    
    public ResponseCreateGameDto createGame(RequestGameDto gameDto) {
        Checker[][] gameDtoBoard = gameDto.getBoard();
        Cell[][] board = new Cell[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++)
            for (int j = 0; j < BOARD_SIZE; j++)
                board[i][j] = new Cell(i, j, null, gameDtoBoard[i][j]);
        
        Game game = new Game(
                board,
                Team.WHITE,
                Status.RUNNING,
                null,
                Collections.emptyList()
        );
        
        calculateSituation(game);
        
        return DtoMapper.map(
                gameDao.put(game),
                game.getSituation()
        );
    }
    
    
    public Game getGame(String id) {
        return gameDao.get(id);
    }
    
    
    public Map<String, Game> getGames() {
        return gameDao.getAll();
    }
}

package checkers.service;


import checkers.controller.util.FromDtoMapper;
import checkers.controller.util.ToDtoMapper;
import checkers.database.dao.GameDao;
import checkers.dto.request.CreateGameRequest;
import checkers.dto.response.CreateGameResponse;
import checkers.dto.response.GetGameResponse;
import checkers.dto.response.MakeStepResponse;
import checkers.dto.versatile.StepDto;
import checkers.model.*;
import checkers.service.util.BoardIterator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


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
    
    
    private boolean updateSituation(Game game, int row, int col, boolean foundMustBeFilled) {
        if (!isTurnOf(row, col, game))
            return foundMustBeFilled;
    
        Cell[][] board = game.getBoard();
        Multimap<Cell, PossibleMove> situation = game.getSituation();
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
        
        return foundMustBeFilled;
    }
    
    
    private void filterMustBeFilledOnly(Game game) {
        Multimap<Cell, PossibleMove> situation = game.getSituation();
    
        for (Map.Entry<Cell, PossibleMove> entry: situation.entries())
            if (entry.getValue().getState() != CellState.MUST_BE_FILLED)
                situation.remove(entry.getKey(), entry.getValue());
    }
    
    
    private void calculateSituation(Game game) {
        Multimap<Cell, PossibleMove> situation = ArrayListMultimap.create();
        game.setSituation(situation);
        boolean foundMustBeFilled = false;
        
        for (int row = 0; row < BOARD_SIZE; row++)
            for (int col = 0; col < BOARD_SIZE; col++)
                foundMustBeFilled = updateSituation(game, row, col, foundMustBeFilled);
        
        if (foundMustBeFilled)
            filterMustBeFilledOnly(game);
    }
    
    
    private void calculateSituation(Cell from, Game game) {
        Multimap<Cell, PossibleMove> situation = ArrayListMultimap.create();
        game.setSituation(situation);
        boolean foundMustBeFilled = false;
    
        foundMustBeFilled = updateSituation(game, from.getRow(), from.getCol(), foundMustBeFilled);
    
        if (foundMustBeFilled)
            filterMustBeFilledOnly(game);
    }
    
    
    public CreateGameResponse createGame(CreateGameRequest request) {
        Checker[][] gameDtoBoard = request.getBoard();
        Cell[][] board = new Cell[BOARD_SIZE][BOARD_SIZE];
        for (int row = 0; row < BOARD_SIZE; row++)
            for (int col = 0; col < BOARD_SIZE; col++)
                board[row][col] = new Cell(row, col, CellState.DEFAULT, gameDtoBoard[row][col]);
        
        Game game = new Game(board, Team.WHITE);
        
        calculateSituation(game);
        
        return ToDtoMapper.map(
                gameDao.put(game),
                game.getSituation()
        );
    }
    
    
    private List<Cell> togglePromptMode(Cell inPromptMode, Game game) {
        List<Cell> changedCells = game.getSituation().get(inPromptMode).stream()
                .map(PossibleMove::getDest)
                .map(sitCell -> game.getBoard()[sitCell.getRow()][sitCell.getCol()]).collect(Collectors.toList());
        changedCells.add(inPromptMode);
        
        for (Cell cell: changedCells)
            cell.setState(CellState.DEFAULT);
        
        return changedCells;
    }
    
    
    private void makeKingIfNeeded(Cell cell, Game game) {
        Team whoseTurn = game.getWhoseTurn();
        int row = cell.getRow();
        
        if (whoseTurn == Team.WHITE && row == BOARD_SIZE - 1) {
            cell.setChecker(Checker.WHITE_KING);
            game.setBecomeKing(true);
        }
    
        else if (whoseTurn == Team.BLACK && row == 0) {
            cell.setChecker(Checker.BLACK_KING);
            game.setBecomeKing(true);
        }
    }
    
    
    private void moveChecker(Cell from, Cell to, Game game) {
        to.setChecker(from.getChecker());
        from.setChecker(null);
        
        if (game.getCurrentMove() == null)
            game.setCurrentMove(new Move(new ArrayList<>(), false, game.getWhoseTurn()));
        
        game.getCurrentMove().getSteps().add(new Step(from, to));
        
        makeKingIfNeeded(to, game);
    }
    
    
    public MakeStepResponse makeStep(String gameId, StepDto request) {
        Game game = gameDao.get(gameId);
        Cell[][] board = game.getBoard();
        List<Cell> changedCells = null;
        Step step = FromDtoMapper.map(request, board);
        Cell from = step.getFrom();
        Cell to = step.getTo();
        CellState toState = game.getSituation().get(from).stream()
                .filter(cell -> cell.getDest().equals(to)).findAny().orElseThrow().getState();
        
        if (toState == CellState.CAN_BE_FILLED) {
            changedCells = togglePromptMode(from, game);
            moveChecker(from, to, game);
            game.setSituation(ArrayListMultimap.create());
            
            gameDao.update(gameId, game);
        }

        else if (toState == CellState.MUST_BE_FILLED) {
            changedCells = togglePromptMode(from, game);
            moveChecker(from, to, game);

            Cell killedCell = game.getSituation().get(from).stream()
                    .filter(possibleMove -> possibleMove.getDest().equals(to)).findAny().orElseThrow().getFoe();
            killedCell.setState(CellState.KILLED);
            changedCells.add(killedCell);
            game.getCurrentMove().setHaveKilled(true);

            calculateSituation(to, game);
            changedCells.addAll(togglePromptMode(to, game));
    
            gameDao.update(gameId, game);
        }
        
        return ToDtoMapper.map(
                changedCells,
                game.getSituation()
        );
    }
    
    
    public GetGameResponse getGame(String id) {
        return ToDtoMapper.map(gameDao.get(id));
    }
    
    
    public Map<String, GetGameResponse> getGames() {
        return gameDao.getAll().entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), ToDtoMapper.map(entry.getValue())))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }
}

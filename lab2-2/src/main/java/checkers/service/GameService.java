package checkers.service;


import checkers.controller.util.FromDtoMapper;
import checkers.controller.util.ToDtoMapper;
import checkers.database.dao.GameDao;
import checkers.dto.request.CreateGameRequest;
import checkers.dto.response.ApplyCurrentMoveResponse;
import checkers.dto.response.CreateGameResponse;
import checkers.dto.response.EditCurrentMoveResponse;
import checkers.dto.response.GetGameResponse;
import checkers.dto.versatile.StepDto;
import checkers.model.*;
import checkers.service.util.BoardIterator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        
        return cell1.getState() != CellState.KILLED && cell2.getState() != CellState.KILLED
                && checker1 != null && checker2 != null
                && checker1.getTeam() != checker2.getTeam();
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
                    
                    if (cellTo.getChecker() == null) {
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
        
        situation.entries().removeIf(entry -> entry.getValue().getState() != CellState.MUST_BE_FILLED);
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
        updateSituation(game, from.getRow(), from.getCol(), true);
    }
    
    
    public CreateGameResponse createGame(CreateGameRequest request) {
        Checker[][] gameDtoBoard = request.getBoard();
        Cell[][] board = new Cell[BOARD_SIZE][BOARD_SIZE];
        for (int row = 0; row < BOARD_SIZE; row++)
            for (int col = 0; col < BOARD_SIZE; col++)
                board[row][col] = new Cell(row, col, CellState.DEFAULT, gameDtoBoard[row][col]);
        
        int whiteCounter = countEnemies(Team.WHITE, board);
        int blackCounter = countEnemies(Team.BLACK, board);
        Team team = null;
        Status status;
        
        if (whiteCounter == 0) {
            status = Status.OVER;
            
            if (blackCounter != 0)
                team = Team.BLACK;
        }
        
        else if (blackCounter == 0) {
            status = Status.OVER;
            team = Team.WHITE;
        }
        
        else {
            team = Team.WHITE;
            status = Status.RUNNING;
        }
        
        Game game = new Game(board, team, status);
        
        calculateSituation(game);
        
        return ToDtoMapper.map(
                gameDao.put(game),
                game.getSituation(),
                game.getStatus(),
                game.getWhoseTurn()
        );
    }
    
    
    private List<Cell> togglePromptMode(Cell targetCell, Game game) {
        List<Cell> changedCells = game.getSituation().get(targetCell).stream()
                .map(PossibleMove::getDest)
                .map(sitCell -> game.getBoard()[sitCell.getRow()][sitCell.getCol()]).collect(Collectors.toList());
        
        if (!changedCells.isEmpty()) {
            for (Cell cell: changedCells)
                cell.setState(
                        cell.getState() == CellState.DEFAULT?
                                game.getSituation().get(targetCell).stream().filter(move -> {
                                    Cell dest = move.getDest();
                                    
                                    return cell.getRow() == dest.getRow() && cell.getCol() == dest.getCol();
                                }).findAny().orElseThrow().getState() :
                                CellState.DEFAULT
                );
            
            changedCells.add(targetCell);
            targetCell.setState(targetCell.getState() == CellState.PROMPT? CellState.DEFAULT : CellState.PROMPT);
        }
        
        else if (targetCell.getState() == CellState.PROMPT) {
            targetCell.setState(CellState.DEFAULT);
            changedCells.add(targetCell);
        }
        
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
    
    
    public EditCurrentMoveResponse makeStep(String gameId, StepDto request) {
        Game game = gameDao.get(gameId);
        Cell[][] board = game.getBoard();
        List<Cell> killed = game.getKilled();
        List<Cell> changedCells = null;
        Step step = FromDtoMapper.map(request, board);
        Cell from = step.getFrom();
        Cell to = step.getTo();
        
        if (from.getState() != CellState.PROMPT)
            togglePromptMode(from, game); // as enter-prompt-mode clicks are not sent to the server
        
        CellState toState = to.getState();
        
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
            killed.add(killedCell);
            game.getCurrentMove().setHaveKilled(true);
            
            calculateSituation(to, game);
            changedCells.addAll(togglePromptMode(to, game));
            changedCells.add(killedCell);
            
            gameDao.update(gameId, game);
        }
        
        return ToDtoMapper.map(
                changedCells,
                game.getSituation()
        );
    }
    
    
    private int countEnemies(Team whoseTurn, Cell[][] board) {
        int counter = 0;
        
        for (Cell[] row: board)
            for (Cell cell: row) {
                Checker checker = cell.getChecker();
                
                if (checker != null && checker.getTeam() != whoseTurn)
                    counter++;
            }
        
        return counter;
    }
    
    
    public ApplyCurrentMoveResponse applyCurrentMove(String gameId) {
        Game game = gameDao.get(gameId);
        Cell[][] board = game.getBoard();
        List<Cell> killed = game.getKilled();
        
        for (Cell cell: killed) {
            cell.setChecker(null);
            cell.setState(CellState.DEFAULT);
        }
        
        List<Cell> changedCells = new ArrayList<>(killed);
        Move currentMove = game.getCurrentMove();
        killed.clear();
        game.setCurrentMove(null);
        
        if (countEnemies(game.getWhoseTurn(), board) == 0) {
            game.setSituation(ArrayListMultimap.create());
            game.setStatus(Status.OVER);
        }
        
        else {
            game.setWhoseTurn(game.getWhoseTurn() == Team.WHITE? Team.BLACK : Team.WHITE);
            calculateSituation(game);
        }
        
        gameDao.update(gameId, game);
        
        return ToDtoMapper.map(
                changedCells,
                game.getSituation(),
                game.getStatus(),
                game.getWhoseTurn(),
                currentMove
        );
    }
    
    
    public EditCurrentMoveResponse cancelCurrentMove(String gameId) {
        Game game = gameDao.get(gameId);
        List<Cell> killed = game.getKilled();
        List<Step> currentMoveSteps = game.getCurrentMove().getSteps();
        Cell startCell = currentMoveSteps.get(0).getFrom();
        Cell currentCell = currentMoveSteps.get(currentMoveSteps.size() - 1).getTo();
        
        List<Cell> changedCells = new ArrayList<>(killed);
        changedCells.add(startCell);
        changedCells.add(currentCell);
        
        startCell.setChecker(game.isBecomeKing()?
                (game.getWhoseTurn() == Team.BLACK? Checker.BLACK : Checker.WHITE) :
                currentCell.getChecker());
        currentCell.setChecker(null);
        game.setCurrentMove(null);
        killed.forEach(cell -> cell.setState(CellState.DEFAULT));
        killed.clear();
        game.setBecomeKing(false);
        calculateSituation(game);
        
        gameDao.update(gameId, game);
        
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

package checkers.service;


import checkers.Properties;
import checkers.error.CheckersException;
import checkers.model.*;
import checkers.service.util.BoardIterator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static checkers.controller.util.GameUtil.*;
import static checkers.error.CheckersErrorCode.*;


public class GameServiceBase {
    private final Properties properties;
    
    
    public GameServiceBase(Properties properties) {
        this.properties = properties;
    }
    
    
    protected boolean updateSituation(Game game, int row, int col, boolean foundMustBeFilled) {
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
    
    
    protected void filterMustBeFilledOnly(Game game) {
        Multimap<Cell, PossibleMove> situation = game.getSituation();
        
        situation.entries().removeIf(entry -> entry.getValue().getState() != CellState.MUST_BE_FILLED);
    }
    
    
    protected void calculateSituation(Game game) {
        Multimap<Cell, PossibleMove> situation = ArrayListMultimap.create();
        game.setSituation(situation);
        
        if (game.getStatus() == Status.OVER)
            return;
        
        boolean foundMustBeFilled = false;
        
        for (int row = 0; row < BOARD_SIZE; row++)
            for (int col = 0; col < BOARD_SIZE; col++)
                if (isPlayCell(row, col))
                    foundMustBeFilled = updateSituation(game, row, col, foundMustBeFilled);
        
        if (foundMustBeFilled)
            filterMustBeFilledOnly(game);
    }
    
    
    protected void calculateSituation(Cell from, Game game) {
        Multimap<Cell, PossibleMove> situation = ArrayListMultimap.create();
        game.setSituation(situation);
        updateSituation(game, from.getRow(), from.getCol(), true);
    }
    
    
    protected Game createGame(Cell[][] board) {
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
        
        Game game = new Game(board, team, status, LocalDateTime.now());
        
        calculateSituation(game);
        
        return game;
    }
    
    
    protected List<Cell> togglePromptMode(Cell targetCell, Game game) {
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
    
    
    protected void toggleWhoseTurn(Game game) {
        game.setWhoseTurn(game.getWhoseTurn() == Team.WHITE? Team.BLACK : Team.WHITE);
    }
    
    
    protected List<Cell> surrender(Game game) throws CheckersException {
        game.setStatus(Status.OVER);
        List<Cell> changedCells = cancelCurrentMove(game);
        toggleWhoseTurn(game);
        game.setSituation(ArrayListMultimap.create());
        
        return changedCells;
    }
    
    
    protected List<Cell> surrenderIfTimeIsUp(Game game) throws CheckersException {
        if (game.getStatus() == Status.RUNNING
                && Duration.between(game.getMoveStartTime(), LocalDateTime.now()).toSeconds() >= properties.getMoveTime())
            return surrender(game);
        
        else
            return new ArrayList<>();
    }
    
    
    protected void makeKingIfNeeded(Cell cell, Game game) {
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
    
    
    protected void moveChecker(Cell from, Cell to, Game game) {
        to.setChecker(from.getChecker());
        from.setChecker(null);
        
        if (game.getCurrentMove() == null)
            game.setCurrentMove(new Move(new ArrayList<>(), false, game.getWhoseTurn()));
        
        game.getCurrentMove().getSteps().add(new Step(from, to));
        
        makeKingIfNeeded(to, game);
    }
    
    
    protected List<Cell> makeStep(Step step, Game game) throws CheckersException {
        if (step.getFrom() == null || step.getTo() == null)
            throw new CheckersException(NO_SUCH_CELL, step.toString());
        
        List<Cell> changedCells = new ArrayList<>();
        Cell from = step.getFrom();
        Cell to = step.getTo();
        CellState toState = to.getState();
        
        if (!game.getSituation().get(from).contains(new PossibleMove(to, null, null)))
            throw new CheckersException(IMPOSSIBLE_STEP, step.toString());
        
        if (toState == CellState.CAN_BE_FILLED) {
            changedCells.addAll(togglePromptMode(from, game));
            moveChecker(from, to, game);
            game.setSituation(ArrayListMultimap.create());
        }
        
        else if (toState == CellState.MUST_BE_FILLED) {
            changedCells.addAll(togglePromptMode(from, game));
            moveChecker(from, to, game);
            
            Cell killedCell = game.getSituation().get(from).stream()
                    .filter(possibleMove -> possibleMove.getDest().equals(to)).findAny().orElseThrow().getFoe();
            killedCell.setState(CellState.KILLED);
            game.getKilled().add(killedCell);
            game.getCurrentMove().setHaveKilled(true);
            
            calculateSituation(to, game);
            changedCells.addAll(togglePromptMode(to, game));
            changedCells.add(killedCell);
        }
        
        return changedCells;
    }
    
    
    protected List<Cell> makeMove(Move move, Game game) throws CheckersException {
        List<Cell> changedCells = new ArrayList<>();
        List<Step> steps = move.getSteps();
        
        Cell departureCell = steps.get(0).getFrom();
        if (departureCell.getState() != CellState.PROMPT)
            togglePromptMode(departureCell, game);
        
        for (Step step: steps)
            changedCells.addAll(makeStep(step, game));
        
        if (game.getCurrentMove().isHaveKilled() != move.isHaveKilled())
            throw new CheckersException(HAVE_KILLED_MISMATCH, move.toString());
        
        changedCells.addAll(applyCurrentMove(game));
        
        return changedCells;
    }
    
    
    protected List<Cell> applyCurrentMove(Game game) {
        List<Cell> changedCells = new ArrayList<>();
        
        if (game.getStatus() == Status.OVER)
            return changedCells;
        
        Cell[][] board = game.getBoard();
        List<Cell> killed = game.getKilled();
        
        for (Cell cell: killed) {
            cell.setChecker(null);
            cell.setState(CellState.DEFAULT);
        }
        
        changedCells.addAll(killed);
        killed.clear();
        game.getMoveList().add(game.getCurrentMove());
        game.setCurrentMove(null);
        game.setBecomeKing(false);
        
        if (countEnemies(game.getWhoseTurn(), board) == 0) {
            game.setSituation(ArrayListMultimap.create());
            game.setStatus(Status.OVER);
        }
        
        else {
            toggleWhoseTurn(game);
            calculateSituation(game);
        }
        
        game.setMoveStartTime(LocalDateTime.now());
        
        return changedCells;
    }
    
    
    protected List<Cell> cancelCurrentMove(Game game) {
        Move currentMove = game.getCurrentMove();
        
        if (currentMove == null)
            return new ArrayList<>();
        
        List<Step> currentMoveSteps = currentMove.getSteps();
        Cell startCell = currentMoveSteps.get(0).getFrom();
        Cell currentCell = currentMoveSteps.get(currentMoveSteps.size() - 1).getTo();
        List<Cell> killed = game.getKilled();
        
        List<Cell> changedCells = new ArrayList<>(killed);
        killed.clear();
        changedCells.addAll(currentMoveSteps.stream().map(Step::getFrom).collect(Collectors.toList()));
        changedCells.add(currentCell);
        
        changedCells.addAll(game.getSituation().get(currentCell).stream()
                .map(PossibleMove::getDest).collect(Collectors.toList()));
        
        startCell.setChecker(game.getBecomeKing()?
                (game.getWhoseTurn() == Team.BLACK? Checker.BLACK : Checker.WHITE) :
                currentCell.getChecker());
        currentCell.setChecker(null);
        game.setCurrentMove(null);
        changedCells.forEach(cell -> cell.setState(CellState.DEFAULT));
        game.setBecomeKing(false);
        calculateSituation(game);
        
        return changedCells;
    }
}

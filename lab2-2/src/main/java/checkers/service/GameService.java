package checkers.service;


import checkers.controller.util.FromDtoMapper;
import checkers.controller.util.ToDtoMapper;
import checkers.database.dao.GameDao;
import checkers.dto.request.ChangeStatusRequest;
import checkers.dto.request.CreateGameRequest;
import checkers.dto.response.*;
import checkers.dto.versatile.StepDto;
import checkers.error.CheckersException;
import checkers.model.*;
import com.google.common.collect.ArrayListMultimap;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static checkers.controller.util.FromDtoMapper.map;
import static checkers.error.CheckersErrorCode.INCORRECT_STATUS;
import static checkers.error.CheckersErrorCode.PARSING_ERROR;


@Service
public class GameService extends GameServiceBase {
    private final GameDao gameDao;
    
    
    public GameService(GameDao gameDao) {
        this.gameDao = gameDao;
    }
    
    
    public CreateGameResponse createGame(CreateGameRequest request) throws CheckersException {
        Cell[][] board = map(request.getBoard());
        Game game = createGame(board);
        
        return ToDtoMapper.map(
                gameDao.put(game),
                game.getSituation(),
                game.getWhoseTurn(),
                game.getStatus()
        );
    }
    
    
    public CreateGameResponse createGameFromMoveList(CreateGameRequest request) throws CheckersException {
        List<String> moveListStr = request.getMoveList();
        Cell[][] board = map(request.getBoard());
        Game game = createGame(board);
        
        for (int lineIndex = 0; lineIndex < moveListStr.size(); lineIndex++) {
            String line = moveListStr.get(lineIndex).trim();
            
            if (!line.isEmpty())
                try {
                    String[] splitLine = line.split("\\s+");
                    
                    makeMove(FromDtoMapper.strToMove(splitLine[1], Team.WHITE, board), game);
                    
                    if (lineIndex != moveListStr.size() - 1 || splitLine.length == 3)
                        makeMove(FromDtoMapper.strToMove(splitLine[2], Team.BLACK, board), game);
                } catch (CheckersException | ArrayIndexOutOfBoundsException e) {
                    throw new CheckersException(PARSING_ERROR, line, e);
                }
        }
        
        return ToDtoMapper.map(
                gameDao.put(game),
                game.getBoard(),
                game.getWhoseTurn(),
                game.getStatus(),
                game.getSituation(),
                game.getMoveList()
        );
    }
    
    
    public SurrenderResponse surrender(String gameId, ChangeStatusRequest request) throws CheckersException {
        Status status = request.getStatus();
        
        if (status != Status.OVER)
            throw new CheckersException(INCORRECT_STATUS, String.valueOf(status));
    
        Game game = gameDao.get(gameId);
        List<Cell> changedCells = surrender(game);
        
        gameDao.update(gameId, game);
        
        return ToDtoMapper.map(
                changedCells,
                game.getSituation(),
                game.getStatus(),
                game.getWhoseTurn()
        );
    }
    
    
    public EditCurrentMoveResponse makeStep(String gameId, StepDto request) throws CheckersException {
        Game game = gameDao.get(gameId);
        Cell[][] board = game.getBoard();
        Step step = map(request, board);
        Cell from = step.getFrom();
        
        if (from.getState() != CellState.PROMPT)
            togglePromptMode(from, game); // as enter-prompt-mode clicks are not sent to the server
        
        List<Cell> changedCells = makeStep(step, game);
        
        gameDao.update(gameId, game);
        
        return ToDtoMapper.map(
                changedCells,
                game.getSituation()
        );
    }
    
    
    public ApplyCurrentMoveResponse applyCurrentMove(String gameId) {
        Game game = gameDao.get(gameId);
        Move currentMove = game.getCurrentMove();
        
        List<Cell> changedCells = applyCurrentMove(game);
        
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
        
        List<Cell> changedCells = cancelCurrentMove(game);
        
        gameDao.update(gameId, game);
        
        return ToDtoMapper.map(
                changedCells,
                game.getSituation()
        );
    }
    
    
    public GetGameResponse getGame(String id) {
        return ToDtoMapper.map(gameDao.get(id));
    }
    
    
    public List<GetGameResponse> getGames(boolean statusOnly) {
        List<Game> games = statusOnly? gameDao.getAllStatusOnly() : gameDao.getAll();
        
        return games.stream().map(ToDtoMapper::map).collect(Collectors.toList());
    }
}

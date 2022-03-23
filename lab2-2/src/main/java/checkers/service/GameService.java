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
import checkers.error.CheckersException;
import checkers.model.*;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static checkers.error.CheckersErrorCode.PARSING_ERROR;


@Service
public class GameService extends GameServiceBase {
    private final GameDao gameDao;
    
    
    public GameService(GameDao gameDao) {
        this.gameDao = gameDao;
    }
    
    
    public CreateGameResponse createGame(CreateGameRequest request) {
        Cell[][] board = createBoard(request.getBoard());
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
        Cell[][] board = createBoard(request.getBoard());
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
    
    
    public EditCurrentMoveResponse makeStep(String gameId, StepDto request) throws CheckersException {
        Game game = gameDao.get(gameId);
        Cell[][] board = game.getBoard();
        Step step = FromDtoMapper.map(request, board);
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
    
    
    public Map<String, GetGameResponse> getGames() {
        return gameDao.getAll().entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), ToDtoMapper.map(entry.getValue())))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }
}

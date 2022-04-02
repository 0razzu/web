package checkers.service;


import checkers.Properties;
import checkers.controller.util.FromDtoMapper;
import checkers.controller.util.ToDtoMapper;
import checkers.database.dao.GameDao;
import checkers.dto.request.ChangeStatusRequest;
import checkers.dto.request.CreateGameRequest;
import checkers.dto.response.ApplyCurrentMoveResponse;
import checkers.dto.response.CreateGameResponse;
import checkers.dto.response.EditStateResponse;
import checkers.dto.response.GetGameResponse;
import checkers.dto.versatile.StepDto;
import checkers.error.CheckersException;
import checkers.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static checkers.controller.util.FromDtoMapper.map;
import static checkers.error.CheckersErrorCode.*;


@Service
public class GameService extends GameServiceBase {
    private final Logger LOGGER = LoggerFactory.getLogger(GameService.class);
    private final GameDao gameDao;
    
    
    public GameService(GameDao gameDao, Properties properties) {
        super(properties);
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
        List<String> moveListStr = request.getMoveList().stream()
                .filter(line -> !line.isBlank()).map(String::trim).collect(Collectors.toList());
        Cell[][] board = map(request.getBoard());
        Game game = createGame(board);
        
        for (int lineIndex = 0; lineIndex < moveListStr.size(); lineIndex++) {
            String line = moveListStr.get(lineIndex);
            
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
    
    
    public EditStateResponse surrender(String gameId, ChangeStatusRequest request) throws CheckersException {
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
    
    
    public EditStateResponse makeStep(String gameId, StepDto request) throws CheckersException {
        Game game = gameDao.get(gameId);
        
        surrenderIfTimeIsUp(game);
        
        if (game.getStatus() == Status.OVER)
            throw new CheckersException(GAME_OVER);
        
        Cell[][] board = game.getBoard();
        Step step = map(request, board);
        Cell from = step.getFrom();
        
        if (from.getState() != CellState.PROMPT)
            togglePromptMode(from, game); // as enter-prompt-mode clicks are not sent to the server
        
        List<Cell> changedCells = makeStep(step, game);
        
        gameDao.update(gameId, game);
        
        return ToDtoMapper.map(
                changedCells,
                game.getSituation(),
                game.getStatus(),
                game.getWhoseTurn()
        );
    }
    
    
    public ApplyCurrentMoveResponse applyCurrentMove(String gameId) throws CheckersException {
        Game game = gameDao.get(gameId);
        surrenderIfTimeIsUp(game);
        
        if (game.getStatus() == Status.OVER)
            throw new CheckersException(GAME_OVER);
        
        Team whoseTurn = game.getWhoseTurn();
        Move currentMove = game.getCurrentMove();
        
        List<Cell> changedCells = applyCurrentMove(game);
        
        gameDao.update(gameId, game);
        
        LOGGER.info("Game {}: {} made a move: {}", game.getId(), whoseTurn, ToDtoMapper.mapMoveToStr(currentMove));
        
        return ToDtoMapper.map(
                changedCells,
                game.getSituation(),
                game.getStatus(),
                game.getWhoseTurn(),
                currentMove
        );
    }
    
    
    public EditStateResponse cancelCurrentMove(String gameId) throws CheckersException {
        Game game = gameDao.get(gameId);
        
        surrenderIfTimeIsUp(game);
        
        if (game.getStatus() == Status.OVER)
            throw new CheckersException(GAME_OVER);
        
        List<Cell> changedCells = cancelCurrentMove(game);
        
        gameDao.update(gameId, game);
        
        return ToDtoMapper.map(
                changedCells,
                game.getSituation(),
                game.getStatus(),
                game.getWhoseTurn()
        );
    }
    
    
    private void updateGameStatus(Game game) throws CheckersException {
        if (game.getStatus() == Status.RUNNING) {
            surrenderIfTimeIsUp(game);
            
            if (game.getStatus() == Status.OVER)
                gameDao.update(game.getId(), game);
        }
    }
    
    
    public GetGameResponse getGame(String id) throws CheckersException {
        Game game = gameDao.get(id);
        
        updateGameStatus(game);
        
        return ToDtoMapper.map(game);
    }
    
    
    public List<GetGameResponse> getGames(boolean statusOnly) throws CheckersException {
        List<Game> games = gameDao.getAll();
        
        for (Game game: games)
            updateGameStatus(game);
        
        if (statusOnly)
            games = games.stream()
                    .map(game -> new Game(game.getId(), null, null, game.getStatus(), null, null, null, null, null, null))
                    .collect(Collectors.toList());
        
        return games.stream().map(ToDtoMapper::map).collect(Collectors.toList());
    }
}

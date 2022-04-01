package checkers.controller.back;


import checkers.dto.request.ChangeStatusRequest;
import checkers.dto.request.CreateGameRequest;
import checkers.dto.response.ApplyCurrentMoveResponse;
import checkers.dto.response.CreateGameResponse;
import checkers.dto.response.EditStateResponse;
import checkers.dto.response.GetGameResponse;
import checkers.dto.versatile.StepDto;
import checkers.error.CheckersException;
import checkers.service.GameService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    
    
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }
    
    
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CreateGameResponse createGame(@RequestBody CreateGameRequest request) throws CheckersException {
        if (request.getMoveList() == null)
            return gameService.createGame(request);
        
        else
            return gameService.createGameFromMoveList(request);
    }
    
    
    @PutMapping(path = "/{id}/status",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public EditStateResponse surrender(@PathVariable("id") String gameId, @RequestBody ChangeStatusRequest request)
            throws CheckersException {
        return gameService.surrender(gameId, request);
    }
    
    
    @PostMapping(path = "/{id}/currentMove/steps",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public EditStateResponse makeStep(@PathVariable("id") String gameId, @RequestBody StepDto request)
            throws CheckersException {
        return gameService.makeStep(gameId, request);
    }
    
    
    @PostMapping(path = "/{id}/moves", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApplyCurrentMoveResponse applyCurrentMove(@PathVariable("id") String gameId) throws CheckersException {
        return gameService.applyCurrentMove(gameId);
    }
    
    
    @DeleteMapping(path = "/{id}/currentMove")
    public EditStateResponse cancelCurrentMove(@PathVariable("id") String gameId) throws CheckersException {
        return gameService.cancelCurrentMove(gameId);
    }
    
    
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GetGameResponse getGame(@PathVariable String id) throws CheckersException {
        return gameService.getGame(id);
    }
    
    
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GetGameResponse> getGames(
            @RequestParam(value = "statusOnly", required = false, defaultValue = "false") boolean statusOnly)
            throws CheckersException {
        return gameService.getGames(statusOnly);
    }
}

package checkers.controller.back;


import checkers.dto.request.CreateGameRequest;
import checkers.dto.response.CreateGameResponse;
import checkers.dto.response.GetGameResponse;
import checkers.dto.response.MakeStepResponse;
import checkers.dto.versatile.StepDto;
import checkers.service.GameService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    
    
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }
    
    
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CreateGameResponse createGame(@RequestBody CreateGameRequest request) {
        return gameService.createGame(request);
    }
    
    
    @PostMapping(path = "/{id}/currentMove/steps",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public MakeStepResponse makeStep(@PathVariable("id") String gameId, @RequestBody StepDto request) {
        return gameService.makeStep(gameId, request);
    }
    
    
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GetGameResponse getGame(@PathVariable String id) {
        return gameService.getGame(id);
    }
    
    
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, GetGameResponse> getGames() {
        return gameService.getGames();
    }
}

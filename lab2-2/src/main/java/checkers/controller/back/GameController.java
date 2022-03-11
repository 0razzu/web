package checkers.controller.back;


import checkers.dto.response.ResponseCreateGameDto;
import checkers.dto.request.RequestGameDto;
import checkers.model.Game;
import checkers.service.GameService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api")
public class GameController {
    private final GameService gameService;
    
    
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }
    
    
    @PostMapping(path = "/game", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseCreateGameDto createGame(@RequestBody RequestGameDto gameDto) {
        return gameService.createGame(gameDto);
    }
    
    
    @GetMapping(path = "/game/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Game getGame(@PathVariable String id) {
        return gameService.getGame(id);
    }
    
    
    @GetMapping(path = "/game", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Game> getGames() {
        return gameService.getGames();
    }
}

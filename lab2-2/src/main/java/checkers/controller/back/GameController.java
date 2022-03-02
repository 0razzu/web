package checkers.controller.back;


import checkers.service.GameService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class GameController {
    private final GameService gameService;
    
    
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }
}

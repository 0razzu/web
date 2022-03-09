package checkers.database.dao;


import checkers.model.Game;

import java.util.Map;


public interface GameDao {
    String put(Game game);
    Game get(String gameId);
    Map<String, Game> getAll();
}

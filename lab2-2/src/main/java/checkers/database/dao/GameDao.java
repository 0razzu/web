package checkers.database.dao;


import checkers.model.Game;

import java.util.Map;


public interface GameDao {
    String put(Game game);
    void update(String id, Game game);
    Game get(String id);
    Map<String, Game> getAll();
}

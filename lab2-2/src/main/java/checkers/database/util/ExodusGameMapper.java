package checkers.database.util;


import checkers.model.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jetbrains.exodus.entitystore.Entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ExodusGameMapper {
    private static final Gson GSON = new Gson();
    
    
    public static void toEntity(Entity entity, Game game) {
        Map<String, Collection<PossibleMove>> jsonValuedSituation = new HashMap<>();
        for (Map.Entry<Cell, Collection<PossibleMove>> entry: game.getSituation().asMap().entrySet())
            jsonValuedSituation.put(GSON.toJson(entry.getKey()), entry.getValue());
        
        entity.setProperty("board", GSON.toJson(game.getBoard()));
        entity.setProperty("whoseTurn", game.getWhoseTurn().toString());
        entity.setProperty("status", game.getStatus().toString());
        entity.setProperty("situation", GSON.toJson(jsonValuedSituation));
        entity.setProperty("moveList", GSON.toJson(game.getMoveList()));
    }
    
    
    public static Game fromEntity(Entity entity) {
        Map<String, Collection<PossibleMove>> jsonValuedSituation =
                GSON.fromJson((String) entity.getProperty("situation"), new TypeToken<Map<String, List<PossibleMove>>>(){}.getType());
        Multimap<Cell, PossibleMove> situation = ArrayListMultimap.create();
        jsonValuedSituation.forEach((cellStr, possibleMoves) -> situation.putAll(GSON.fromJson(cellStr, Cell.class), possibleMoves));
        
        return new Game(
                GSON.fromJson((String) entity.getProperty("board"), Cell[][].class),
                Team.valueOf((String) entity.getProperty("whoseTurn")),
                Status.valueOf((String) entity.getProperty("status")),
                situation,
                GSON.fromJson((String) entity.getProperty("moveList"), new TypeToken<List<Move>>(){}.getType())
        );
    }
}

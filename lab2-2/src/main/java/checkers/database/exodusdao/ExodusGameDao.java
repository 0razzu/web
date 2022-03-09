package checkers.database.exodusdao;


import checkers.database.dao.GameDao;
import checkers.database.util.ExodusGameMapper;
import checkers.database.util.ExodusStore;
import checkers.model.Game;
import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.EntityIterable;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


@Repository("gameDao")
public class ExodusGameDao implements GameDao {
    private final PersistentEntityStore STORE = ExodusStore.INSTANCE;
    
    
    @Override
    public String put(Game game) {
        AtomicReference<String> id = new AtomicReference<>();
        
        STORE.executeInTransaction(txn -> {
            Entity gameEntity = txn.newEntity("Game");
            ExodusGameMapper.toEntity(gameEntity, game);
            id.set(gameEntity.toIdString());
        });
        
        return id.get();
    }
    
    
    @Override
    public Game get(String gameId) {
        AtomicReference<Game> gameRef = new AtomicReference<>();
        
        STORE.executeInReadonlyTransaction(txn -> {
            Entity gameEntity = txn.getEntity(txn.toEntityId(gameId));
            gameRef.set(ExodusGameMapper.fromEntity(gameEntity));
        });
        
        return gameRef.get();
    }
    
    
    @Override
    public Map<String, Game> getAll() {
        AtomicReference<Map<String, Game>> gamesRef = new AtomicReference<>();
        
        STORE.executeInReadonlyTransaction(txn -> {
            EntityIterable gameEntities = txn.getAll("Game");
            Map<String, Game> games = new HashMap<>();
            gameEntities.forEach(gameEntity -> games.put(gameEntity.toIdString(), ExodusGameMapper.fromEntity(gameEntity)));
            gamesRef.set(games);
        });
        
        return gamesRef.get();
    }
}

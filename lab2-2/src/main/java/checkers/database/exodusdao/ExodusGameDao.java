package checkers.database.exodusdao;


import checkers.database.dao.GameDao;
import checkers.database.util.ExodusGameMapper;
import checkers.database.util.ExodusStore;
import checkers.model.Game;
import checkers.model.Status;
import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.EntityIterable;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    public void update(String id, Game game) {
        STORE.executeInTransaction(txn -> {
            Entity gameEntity = txn.getEntity(txn.toEntityId(id));
            ExodusGameMapper.toEntity(gameEntity, game);
        });
    }
    
    
    @Override
    public Game get(String id) {
        AtomicReference<Game> gameRef = new AtomicReference<>();
        
        STORE.executeInReadonlyTransaction(txn -> {
            Entity gameEntity = txn.getEntity(txn.toEntityId(id));
            gameRef.set(ExodusGameMapper.fromEntity(gameEntity));
        });
        
        return gameRef.get();
    }
    
    
    @Override
    public List<Game> getAll() {
        AtomicReference<List<Game>> gamesRef = new AtomicReference<>();
        
        STORE.executeInReadonlyTransaction(txn -> {
            EntityIterable gameEntities = txn.getAll("Game");
            List<Game> games = new ArrayList<>();
            gameEntities.forEach(gameEntity -> games.add(ExodusGameMapper.fromEntity(gameEntity)));
            
            gamesRef.set(games);
        });
        
        return gamesRef.get();
    }
    
    
    @Override
    public List<Game> getAllStatusOnly() {
        AtomicReference<List<Game>> gamesRef = new AtomicReference<>();
    
        STORE.executeInReadonlyTransaction(txn -> {
            EntityIterable gameEntities = txn.getAll("Game");
            List<Game> games = new ArrayList<>();
            gameEntities.forEach(gameEntity ->
                    games.add(
                            new Game(
                                    gameEntity.toIdString(),
                                    null,
                                    null,
                                    Status.valueOf((String) gameEntity.getProperty("status")),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    LocalDateTime.parse((String) gameEntity.getProperty("moveStartTime"))
                            )
                    )
            );
        
            gamesRef.set(games);
        });
    
        return gamesRef.get();
    }
}

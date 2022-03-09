package checkers.database.util;


import jetbrains.exodus.entitystore.PersistentEntityStore;
import jetbrains.exodus.entitystore.PersistentEntityStores;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import org.springframework.stereotype.Repository;

import javax.annotation.PreDestroy;


@Repository
public final class ExodusStore {
    public static final PersistentEntityStore INSTANCE = PersistentEntityStores.newInstance(".CheckersData");
    
    
    @PreDestroy
    private void close() {
        INSTANCE.close();
    }
}

package checkers.database.util;


import jetbrains.exodus.entitystore.PersistentEntityStore;
import jetbrains.exodus.entitystore.PersistentEntityStores;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.annotation.PreDestroy;


@Repository
public final class ExodusStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExodusStore.class);
    public static final PersistentEntityStore INSTANCE = PersistentEntityStores.newInstance(".CheckersData");
    
    
    @PreDestroy
    private void close() {
        INSTANCE.close();
        LOGGER.info("Database connection closed");
    }
}

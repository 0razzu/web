package checkers.database.util;


import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import org.springframework.stereotype.Repository;

import javax.annotation.PreDestroy;


@Repository
public final class ExodusEnv {
    public static final Environment INSTANCE = Environments.newInstance(".CheckersData");
    
    
    @PreDestroy
    private void close() {
        INSTANCE.close();
    }
}

package checkers.database.exodusdao;


import checkers.database.util.ExodusEnv;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class ExodusDaoBase {
    private final String storeName;
    private final Environment ENV = ExodusEnv.INSTANCE;
    
    
    public ExodusDaoBase(String storeName) {
        this.storeName = storeName;
    }
    
    
    protected Store openStore(Transaction txn) {
        return ENV.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, txn);
    }
    
    
    protected boolean put(ByteIterable key, ByteIterable value) {
        AtomicBoolean res = new AtomicBoolean(false);
        
        ENV.executeInTransaction(txn -> {
            Store store = openStore(txn);
            res.set(store.put(txn, key, value));
        });
        
        return res.get();
    }
    
    
    protected ByteIterable get(ByteIterable key) {
        AtomicReference<ByteIterable> value = new AtomicReference<>();
        
        ENV.executeInReadonlyTransaction(txn -> {
            Store store = openStore(txn);
            value.set(store.get(txn, key));
        });
        
        return value.get();
    }
}

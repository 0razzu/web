package checkers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;


@SpringBootApplication
public class CheckersServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckersServer.class);
    
    
    private static String[] unfoldArgs(String[] args) {
        String[] unfoldedArgs = Arrays.stream(args).map(arg -> {
            String[] argEntry = arg.split("=");
            
            if (argEntry[0].equals("--path"))
                argEntry[0] = "--server.servlet.context-path";
            
            else if (argEntry[0].equals("--port"))
                argEntry[0] = "--server.port";
            
            return String.join("=", argEntry);
        }).toArray(String[]::new);
        
        LOGGER.debug("Unfolded args: {}", Arrays.toString(unfoldedArgs));
        
        return unfoldedArgs;
    }
    
    
    public static void main(String[] args) {
        SpringApplication.run(CheckersServer.class, unfoldArgs(args));
    }
}

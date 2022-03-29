package checkers;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;


@Component
@PropertySource("classpath:application.yml")
@Getter
public class Properties {
    @Value("${game.move-time}")
    private int moveTime;
}

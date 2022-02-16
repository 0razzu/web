package checkers.controller;


import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/hello")
public class HelloController {
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String getHello() {
        return "Hello, world";
    }
}

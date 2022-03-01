package checkers.controller;


import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class CheckersController {
    @RequestMapping(value = "/{path}", produces = MediaType.TEXT_HTML_VALUE)
    public String getPath(@PathVariable("path") String path) {
        return path;
    }
}

package dev.tylerpac.backend;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/hello")
    public String hello() {
        return "Test";
    }

}

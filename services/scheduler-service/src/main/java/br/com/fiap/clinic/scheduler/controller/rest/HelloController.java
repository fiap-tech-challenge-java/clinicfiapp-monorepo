package br.com.fiap.clinic.scheduler.controller.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("")
    public String hello() {
        return "hello scheduler service";
    }
}

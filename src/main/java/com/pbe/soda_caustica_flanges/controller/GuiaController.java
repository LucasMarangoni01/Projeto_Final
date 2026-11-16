package com.pbe.soda_caustica_flanges.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GuiaController {
    @GetMapping("/guia")
    public String guia() {
        return "guia";
    }
}

package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    @Autowired
    private FlangeRepository flangeRepository;

    // ==============================
    // Página Inicial  (localhost/)
    // ==============================
    @GetMapping
    public String home(Model model){
        model.addAttribute("flanges", flangeRepository.findAll());
        return "home/index";
    }
}
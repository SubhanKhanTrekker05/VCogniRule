package com.ooad.mhs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WelcomeController {

    @GetMapping("/")
    public String welcome(Model model) {
        model.addAttribute("appName", "VCogniRule");
        model.addAttribute("appDescription", "Rule-Based Early Mental Health Screening System");
        model.addAttribute("appVersion", "1.0.0");

        return "welcome";
    }
}
package com.ooad.mhs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Shows the About page (GET /about): what the system is, how the screening works, and why it is
 * not a diagnosis.
 */
@Controller
public class AboutController {

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
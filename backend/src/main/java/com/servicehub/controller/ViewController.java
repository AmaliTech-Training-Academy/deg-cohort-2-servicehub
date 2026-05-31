package com.servicehub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() { return "index"; }

    @GetMapping("/login")
    public String login() { return "auth/login"; }

    @GetMapping("/register")
    public String register() { return "auth/register"; }

    @GetMapping("/requests")
    public String requestList() { return "requests/list"; }

    @GetMapping("/requests/submit")
    public String requestSubmit() { return "requests/submit"; }

    @GetMapping("/requests/{id}")
    public String requestDetail(@PathVariable Long id) { return "requests/detail"; }
}

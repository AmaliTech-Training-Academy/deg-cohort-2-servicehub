package com.servicehub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = ViewController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class ViewControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void indexPage_returns200AndIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void requestListPage_returns200AndListView() throws Exception {
        mockMvc.perform(get("/requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("requests/list"));
    }

    @Test
    void requestSubmitPage_returns200AndSubmitView() throws Exception {
        mockMvc.perform(get("/requests/submit"))
                .andExpect(status().isOk())
                .andExpect(view().name("requests/submit"));
    }

    @Test
    void requestDetailPage_returns200AndDetailView() throws Exception {
        mockMvc.perform(get("/requests/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("requests/detail"));
    }

    @Test
    void loginPage_returns200AndLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void registerPage_returns200AndRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }
}

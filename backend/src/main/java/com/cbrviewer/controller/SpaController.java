package com.cbrviewer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to handle SPA (Single Page Application) routing.
 * Forwards all non-API, non-static requests to index.html for client-side routing.
 */
@Controller
public class SpaController {

    /**
     * Forward all unmatched routes to index.html for React Router to handle.
     * This catches routes like /library, /viewer/123, /admin, etc.
     *
     * Excludes:
     * - /api/** (REST API endpoints)
     * - Static resources (handled by Spring's resource handler)
     */
    @RequestMapping(value = {
        "/",
        "/login",
        "/register",
        "/library",
        "/viewer/{id}",
        "/admin",
        "/admin/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}

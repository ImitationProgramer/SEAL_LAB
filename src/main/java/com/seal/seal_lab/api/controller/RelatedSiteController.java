package com.seal.seal_lab.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RelatedSiteController {

    @GetMapping("/related-site")
    public String relatedSite() {
        return "related-site"; // templates/related-site.html
    }
}

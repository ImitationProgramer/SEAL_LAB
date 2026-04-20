package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.annotation.ZeroTrust;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RelatedSiteController {

    @GetMapping("/related-site")
    @ZeroTrust(requiredScore = 0)
    public String relatedSite() {
        return "related-site"; // templates/related-site.html
    }
}

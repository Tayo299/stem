package com.hssc.backend;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PublicContentController {
    private final PublicContentService content;
    public PublicContentController(PublicContentService content) { this.content = content; }

    @GetMapping("/sponsors")
    public List<Api.SponsorResponse> sponsors() { return content.sponsors(false); }

    @GetMapping("/policy")
    public List<Api.ContentPolicyResponse> policies() { return content.policies(false); }
}

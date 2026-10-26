package com.hssc.backend;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/schools")
public class SchoolDirectoryController {
    private final SchoolDirectoryService directory;

    public SchoolDirectoryController(SchoolDirectoryService directory) { this.directory = directory; }

    @GetMapping
    public List<Api.SchoolDirectoryEntry> eligibleSchools() { return directory.eligibleSchools(); }
}

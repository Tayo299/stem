package com.hssc.backend;

import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/admin/question-banks")
public class QuestionBankController {
    private final QuestionBankService service; private final CurrentAccount current;
    public QuestionBankController(QuestionBankService service,CurrentAccount current){this.service=service;this.current=current;}
    @GetMapping public List<Api.BankResponse> list(){return service.list();}
    @PostMapping public Api.BankResponse create(@Valid @RequestBody Api.BankRequest request,@AuthenticationPrincipal Jwt jwt){
        return service.create(request,current.get(jwt));
    }
    @PostMapping("/official-sets/{setCode}") public Api.QuestionSetResponse loadOfficialSet(@PathVariable String setCode,@AuthenticationPrincipal Jwt jwt){
        return service.loadOfficialSet(setCode,current.get(jwt));
    }
    @PostMapping("/placeholders") public Api.BankResponse placeholders(@AuthenticationPrincipal Jwt jwt){
        return service.createPlaceholderBank(current.get(jwt));
    }
    @GetMapping("/{id}/questions") public List<Api.QuestionResponse> questions(@PathVariable UUID id){return service.questions(id);}
    @PostMapping("/{id}/questions/import") public List<Api.QuestionResponse> importQuestions(@PathVariable UUID id,
            @Valid @RequestBody List<Api.QuestionInput> input,@AuthenticationPrincipal Jwt jwt){
        return service.importQuestions(id,input,current.get(jwt));
    }
    @PutMapping("/{id}/active") public Api.BankResponse active(@PathVariable UUID id,@RequestParam boolean value,@AuthenticationPrincipal Jwt jwt){
        return service.setActive(id,value,current.get(jwt));
    }
}

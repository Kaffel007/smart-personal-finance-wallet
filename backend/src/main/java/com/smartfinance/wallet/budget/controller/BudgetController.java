package com.smartfinance.wallet.budget.controller;

import com.smartfinance.wallet.budget.dto.*;
import com.smartfinance.wallet.budget.service.BudgetService;
import com.smartfinance.wallet.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService service;
    public BudgetController(BudgetService service){this.service=service;}
    @PostMapping public ResponseEntity<BudgetResponse> create(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CreateBudgetRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(principal.id(),request));}
    @GetMapping public List<BudgetResponse> getAll(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(required=false) Integer year,@RequestParam(required=false) Integer month,@RequestParam(required=false) Long categoryId){return service.getAll(principal.id(),year,month,categoryId);}
    @GetMapping("/{id}") public BudgetResponse getOne(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,@PathVariable Long id){return service.getOne(principal.id(),id);}
    @PutMapping("/{id}") public BudgetResponse update(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,@PathVariable Long id,
            @Valid @RequestBody UpdateBudgetRequest request){return service.update(principal.id(),id,request);}
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,@PathVariable Long id){service.delete(principal.id(),id);return ResponseEntity.noContent().build();}
}

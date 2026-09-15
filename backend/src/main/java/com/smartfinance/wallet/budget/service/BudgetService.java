package com.smartfinance.wallet.budget.service;

import com.smartfinance.wallet.budget.dto.*;
import com.smartfinance.wallet.budget.entity.Budget;
import com.smartfinance.wallet.budget.exception.*;
import com.smartfinance.wallet.budget.repository.BudgetRepository;
import com.smartfinance.wallet.category.entity.*;
import com.smartfinance.wallet.category.exception.CategoryNotFoundException;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.transaction.repository.FinancialTransactionRepository;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDate;
import java.util.List;

@Service
public class BudgetService {
    private final BudgetRepository repository;
    private final CategoryRepository categoryRepository;
    private final AppUserRepository userRepository;
    private final FinancialTransactionRepository transactionRepository;

    public BudgetService(BudgetRepository repository, CategoryRepository categoryRepository,
            AppUserRepository userRepository, FinancialTransactionRepository transactionRepository) {
        this.repository=repository; this.categoryRepository=categoryRepository;
        this.userRepository=userRepository; this.transactionRepository=transactionRepository;
    }

    @Transactional
    public BudgetResponse create(Long userId, CreateBudgetRequest request) {
        Category category=findExpenseCategory(userId,request.categoryId());
        rejectDuplicate(userId,request.categoryId(),request.year(),request.month(),null);
        AppUser user=userRepository.findById(userId).orElseThrow(CategoryNotFoundException::new);
        try { return response(repository.saveAndFlush(new Budget(user,category,request.year(),request.month(),request.amount()))); }
        catch (DataIntegrityViolationException exception) { throw new BudgetAlreadyExistsException(); }
    }

    @Transactional(readOnly=true)
    public List<BudgetResponse> getAll(Long userId,Integer year,Integer month,Long categoryId) {
        return repository.findAllFiltered(userId,year,month,categoryId).stream().map(this::response).toList();
    }

    @Transactional(readOnly=true)
    public BudgetResponse getOne(Long userId,Long id) { return response(findOwned(userId,id)); }

    @Transactional
    public BudgetResponse update(Long userId,Long id,UpdateBudgetRequest request) {
        Budget budget=findOwned(userId,id);
        Category category=findExpenseCategory(userId,request.categoryId());
        rejectDuplicate(userId,request.categoryId(),request.year(),request.month(),id);
        budget.setCategory(category); budget.setYear(request.year()); budget.setMonth(request.month()); budget.setAmount(request.amount());
        try { return response(repository.saveAndFlush(budget)); }
        catch (DataIntegrityViolationException exception) { throw new BudgetAlreadyExistsException(); }
    }

    @Transactional
    public void delete(Long userId,Long id) { repository.delete(findOwned(userId,id)); repository.flush(); }

    private BudgetResponse response(Budget budget) {
        LocalDate start=LocalDate.of(budget.getYear(),budget.getMonth(),1);
        BigDecimal spent=transactionRepository.sumExpenses(budget.getUser().getId(),budget.getCategory().getId(),start,start.plusMonths(1));
        if (spent == null) spent=BigDecimal.ZERO;
        BigDecimal usage=spent.multiply(BigDecimal.valueOf(100)).divide(budget.getAmount(),2,RoundingMode.HALF_UP);
        return BudgetResponse.from(budget,spent,usage);
    }
    private Budget findOwned(Long userId,Long id) { return repository.findByIdAndUserId(id,userId).orElseThrow(BudgetNotFoundException::new); }
    private Category findExpenseCategory(Long userId,Long id) {
        Category category=categoryRepository.findByIdAndUserId(id,userId).orElseThrow(CategoryNotFoundException::new);
        if (category.getType()!=CategoryType.EXPENSE) throw new BudgetCategoryTypeException();
        return category;
    }
    private void rejectDuplicate(Long userId,Long categoryId,int year,int month,Long excludedId) {
        boolean exists=excludedId==null
                ? repository.existsByUserIdAndCategoryIdAndYearAndMonth(userId,categoryId,year,month)
                : repository.existsByUserIdAndCategoryIdAndYearAndMonthAndIdNot(userId,categoryId,year,month,excludedId);
        if(exists) throw new BudgetAlreadyExistsException();
    }
}

package com.smartfinance.wallet.transaction.service;

import com.smartfinance.wallet.category.entity.Category;
import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.category.exception.CategoryNotFoundException;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.transaction.dto.*;
import com.smartfinance.wallet.transaction.entity.FinancialTransaction;
import com.smartfinance.wallet.transaction.exception.FinancialTransactionNotFoundException;
import com.smartfinance.wallet.transaction.repository.FinancialTransactionRepository;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FinancialTransactionService {
    private final FinancialTransactionRepository repository;
    private final CategoryRepository categoryRepository;
    private final AppUserRepository userRepository;

    public FinancialTransactionService(FinancialTransactionRepository repository,
            CategoryRepository categoryRepository, AppUserRepository userRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FinancialTransactionResponse create(Long userId, CreateFinancialTransactionRequest request) {
        Category category = findCategory(userId, request.categoryId());
        AppUser user = userRepository.findById(userId).orElseThrow(CategoryNotFoundException::new);
        FinancialTransaction transaction = new FinancialTransaction(user, category, category.getType(),
                request.amount(), request.transactionDate(), cleanDescription(request.description()));
        return FinancialTransactionResponse.from(repository.saveAndFlush(transaction));
    }

    @Transactional(readOnly = true)
    public List<FinancialTransactionResponse> getAll(Long userId, CategoryType type, Long categoryId) {
        List<FinancialTransaction> transactions;
        if (type != null && categoryId != null) transactions = repository.findAllByUserIdAndTypeAndCategoryIdOrderByTransactionDateDescIdDesc(userId, type, categoryId);
        else if (type != null) transactions = repository.findAllByUserIdAndTypeOrderByTransactionDateDescIdDesc(userId, type);
        else if (categoryId != null) transactions = repository.findAllByUserIdAndCategoryIdOrderByTransactionDateDescIdDesc(userId, categoryId);
        else transactions = repository.findAllByUserIdOrderByTransactionDateDescIdDesc(userId);
        return transactions.stream().map(FinancialTransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FinancialTransactionResponse getOne(Long userId, Long id) {
        return FinancialTransactionResponse.from(findOwned(userId, id));
    }

    @Transactional
    public FinancialTransactionResponse update(Long userId, Long id, UpdateFinancialTransactionRequest request) {
        FinancialTransaction transaction = findOwned(userId, id);
        Category category = findCategory(userId, request.categoryId());
        transaction.setCategory(category);
        transaction.setType(category.getType());
        transaction.setAmount(request.amount());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setDescription(cleanDescription(request.description()));
        return FinancialTransactionResponse.from(repository.saveAndFlush(transaction));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(findOwned(userId, id));
        repository.flush();
    }

    private Category findCategory(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId).orElseThrow(CategoryNotFoundException::new);
    }
    private FinancialTransaction findOwned(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(FinancialTransactionNotFoundException::new);
    }
    private String cleanDescription(String description) {
        if (description == null) return null;
        String cleaned = description.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}

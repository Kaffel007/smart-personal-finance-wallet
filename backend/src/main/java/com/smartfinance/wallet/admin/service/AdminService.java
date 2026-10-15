package com.smartfinance.wallet.admin.service;

import com.smartfinance.wallet.admin.dto.AdminStatsResponse;
import com.smartfinance.wallet.admin.dto.AdminUserResponse;
import com.smartfinance.wallet.admin.exception.AdminAccountActionForbiddenException;
import com.smartfinance.wallet.admin.exception.AdminSelfBlockException;
import com.smartfinance.wallet.admin.exception.UserNotFoundException;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AdminService {
    private final AppUserRepository userRepository;

    public AdminService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers(String search) {
        String term = search == null ? "" : search.trim();
        List<AppUser> users = term.isEmpty() ? userRepository.findAllByOrderByIdDesc()
                : userRepository.searchAccounts(term);
        return users.stream().map(AdminUserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long id) {
        return AdminUserResponse.from(findUser(id));
    }

    @Transactional
    public AdminUserResponse blockUser(Long id, Long actingAdminId) {
        if (id.equals(actingAdminId)) throw new AdminSelfBlockException();
        return setBlocked(id, true);
    }

    @Transactional
    public AdminUserResponse unblockUser(Long id) {
        return setBlocked(id, false);
    }

    private AdminUserResponse setBlocked(Long id, boolean blocked) {
        AppUser user = findUser(id);
        if (user.getRole() == Role.ADMIN) throw new AdminAccountActionForbiddenException();
        user.setBlocked(blocked);
        return AdminUserResponse.from(userRepository.saveAndFlush(user));
    }

    private AppUser findUser(Long id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(userRepository.count(),
                userRepository.countByEnabledTrueAndBlockedFalse(),
                userRepository.countByBlockedTrue(), userRepository.countByRole(Role.ADMIN));
    }
}

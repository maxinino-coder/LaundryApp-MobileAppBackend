package com.group130.laundryapp.DAL.Configuration.Auth.AuthSupport;



import com.group130.laundryapp.Domain.Entity.Account;
import com.group130.laundryapp.Domain.Enum.AccountRole;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserContextHolder {

    /**
     * Returns the full Account object for the currently authenticated actor
     * (USER, BUSINESS, or RIDER). The Account entity implements UserDetails,
     * so it is stored directly as the principal.
     */
    public Account getCurrentAccount() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in SecurityContext.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Account account)) {
            throw new IllegalStateException("Principal is not an Account instance.");
        }
        return account;
    }

    public UUID getCurrentAccountId() {
        return getCurrentAccount().getId();
    }

    public String getCurrentEmail() {
        return getCurrentAccount().getEmail();
    }

    public AccountRole getCurrentRole() {
        return getCurrentAccount().getRole();
    }

    public boolean isUser()     { return getCurrentRole() == AccountRole.USER; }
    public boolean isBusiness() { return getCurrentRole() == AccountRole.BUSINESS; }
    public boolean isRider()    { return getCurrentRole() == AccountRole.RIDER; }
}
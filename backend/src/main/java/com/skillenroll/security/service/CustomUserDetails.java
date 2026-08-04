package com.skillenroll.security.service;

import com.skillenroll.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter between the {@link User} entity and Spring Security's
 * {@link UserDetails}.
 *
 * <p>Unlike a bare {@code org.springframework.security.core.userdetails.User},
 * this implementation keeps a reference to the underlying entity, so the
 * {@code userId}, {@code firstName}, {@code lastName}, {@code phoneNumber} and
 * {@code role} are available to the JWT generator and to any endpoint reading
 * the authenticated principal from the {@code SecurityContext}.
 *
 * <p>Authorities are exposed as {@code ROLE_<name>} so {@code @PreAuthorize}
 * checks such as {@code hasRole('ADMIN')} work out of the box.
 */
public final class CustomUserDetails implements UserDetails {

    private final User user;

    private CustomUserDetails(User user) {
        this.user = user;
    }

    /**
     * Creates a {@link CustomUserDetails} from a {@link User} entity.
     *
     * @param user the persisted user; must not be {@code null}
     * @return an immutable principal adapter for the given user
     */
    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(user);
    }

    /**
     * Returns the wrapped {@link User} entity.
     *
     * @return the underlying persisted user
     */
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

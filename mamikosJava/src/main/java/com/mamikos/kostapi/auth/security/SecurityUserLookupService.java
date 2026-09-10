package com.mamikos.kostapi.auth.security;

import com.mamikos.kostapi.user.repository.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The only implementation of the filter's lookup seam; kept separate so the filter has no
 * direct compile-time dependency on the user package's internals. */
@Service
@Transactional(readOnly = true)
public class SecurityUserLookupService implements JwtAuthenticationFilter.UserRepositoryUserDetailsLookup {

    private final UserRepository userRepository;

    public SecurityUserLookupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<SecurityUser> findById(Long id) {
        return userRepository.findById(id).filter(u -> u.getDeletedAt() == null).map(SecurityUser::new);
    }
}

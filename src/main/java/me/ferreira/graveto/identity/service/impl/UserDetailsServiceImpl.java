package me.ferreira.graveto.identity.service.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.identity.domain.AuthUser;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {

    final User user = userRepository.fetchUserCredentials(email)
        .orElseThrow(() -> new UsernameNotFoundException(email + " not found."));

    return new AuthUser(
        user.getSid(),
        user.getEmail(),
        user.getPassword(),
        user.getRole()
    );
  }

}

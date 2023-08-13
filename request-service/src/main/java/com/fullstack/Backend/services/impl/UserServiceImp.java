package com.fullstack.Backend.services.impl;

import com.fullstack.Backend.models.*;
import com.fullstack.Backend.security.JwtUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.fullstack.Backend.repositories.interfaces.UserRepository;



@Service
@CacheConfig(cacheNames = {"user"})
public class UserServiceImp implements UserDetailsService {

    @Autowired
    private UserRepository _userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = _userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
        return UserDetailsImpl.build(user);
    }
}

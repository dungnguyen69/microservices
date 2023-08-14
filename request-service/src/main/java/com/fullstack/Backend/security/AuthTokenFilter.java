package com.fullstack.Backend.security;

import com.fullstack.Backend.models.SystemRole;
import com.fullstack.Backend.models.User;
import com.fullstack.Backend.services.impl.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.HashSet;
import java.util.Set;


/* A filter that executes once per request */
public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Value("${fullstack.app.jwtSecret}")
    private String SECRET_KEY;

//    @Autowired
//    private UserDetailsService _userService;

    /* – get JWT from the Authorization header (by removing Bearer prefix)
        – if the request has JWT, validate it, parse username from it
        – from username, get UserDetails to create an Authentication object
        – set the current UserDetails in SecurityContext using setAuthentication(authentication) method. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if(jwt != null && jwtUtils.validateJwtToken(jwt)) {
                UserDetails userDetails = extractUserFromJwt(jwt);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }
        response.setHeader("Access-Control-Allow-Origin", "https://dungnguyen69.github.io");
//        response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");

        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, OPTIONS, DELETE");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Headers",
                "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers,Content-Disposition");
        filterChain.doFilter(request, response);
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));
    }

    public Claims parseClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String parseJwt(HttpServletRequest request) {
        return jwtUtils.getJwtFromCookies(request);
    }

    private UserDetails extractUserFromJwt(String jwt) {
        String username = jwtUtils.getUserNameFromJwtToken(jwt);
        User user = new User();
        user.setUserName(username);
        Set<SystemRole> systemRoles = new HashSet<>();
        Claims claims = parseClaims(jwt);
        String roles = (String) claims.get("roles");
        roles = roles
                .replace("[", "")
                .replace("]", "");
        String[] roleNames = roles.split(",");
        for (var role : roleNames) {
            SystemRole SR = new SystemRole();
            SR.setName(role);
            systemRoles.add(SR);
        }
        user.setSystemRoles(systemRoles);
        return UserDetailsImpl.build(user);
    }
}

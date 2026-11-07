package com.TrackFlow.identity;

import com.TrackFlow.identity.security.filters.JwtAuthenticationFilter;
import com.TrackFlow.identity.services.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HandlerExceptionResolver resolver;
    @Mock private FilterChain filterChain;
    @Mock private UserDetails userDetails;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private JwtAuthenticationFilter filter() {
        return new JwtAuthenticationFilter(jwtService, userDetailsService, resolver);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("no Authorization header -> passes through, no authentication")
    void noHeader() throws Exception {
        filter().doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService, resolver);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("non-Bearer header -> passes through, no authentication")
    void nonBearerHeader() throws Exception {
        request.addHeader("Authorization", "Basic abc123");

        filter().doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService, resolver);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("valid token -> SecurityContext populated and chain continues")
    void validToken() throws Exception {

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_CUSTOMER")
        );

        request.addHeader("Authorization", "Bearer good.jwt.token");

        when(jwtService.extractUsername("good.jwt.token"))
                .thenReturn("saad@example.com");

        when(userDetailsService.loadUserByUsername("saad@example.com"))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid("good.jwt.token", userDetails))
                .thenReturn(true);

        doReturn(authorities).when(userDetails).getAuthorities();

        filter().doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();

        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isEqualTo(userDetails);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
        assertThat(auth.isAuthenticated()).isTrue();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(resolver);
    }

    @Test
    @DisplayName("token rejected by isTokenValid -> no authentication, chain continues")
    void invalidToken() throws Exception {
        request.addHeader("Authorization", "Bearer bad.jwt.token");

        when(jwtService.extractUsername("bad.jwt.token")).thenReturn("saad@example.com");
        when(userDetailsService.loadUserByUsername("saad@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("bad.jwt.token", userDetails)).thenReturn(false);

        filter().doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("already authenticated -> user is not reloaded")
    void alreadyAuthenticated() throws Exception {
        var existing = new UsernamePasswordAuthenticationToken("someone", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer good.jwt.token");
        when(jwtService.extractUsername("good.jwt.token")).thenReturn("saad@example.com");

        filter().doFilter(request, response, filterChain);

        verifyNoInteractions(userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JwtException -> delegated to HandlerExceptionResolver, chain stops")
    void jwtException() throws Exception {
        JwtException ex = new JwtException("expired");
        request.addHeader("Authorization", "Bearer expired.jwt.token");
        when(jwtService.extractUsername("expired.jwt.token")).thenThrow(ex);

        filter().doFilter(request, response, filterChain);

        verify(resolver).resolveException(eq(request), eq(response), isNull(), eq(ex));
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("UsernameNotFoundException -> delegated to HandlerExceptionResolver, chain stops")
    void userNotFound() throws Exception {
        UsernameNotFoundException ex = new UsernameNotFoundException("gone");
        request.addHeader("Authorization", "Bearer some.jwt.token");
        when(jwtService.extractUsername("some.jwt.token")).thenReturn("ghost@example.com");
        when(userDetailsService.loadUserByUsername("ghost@example.com")).thenThrow(ex);

        filter().doFilter(request, response, filterChain);

        verify(resolver).resolveException(eq(request), eq(response), isNull(), eq(ex));
        verify(filterChain, never()).doFilter(any(), any());
    }
}
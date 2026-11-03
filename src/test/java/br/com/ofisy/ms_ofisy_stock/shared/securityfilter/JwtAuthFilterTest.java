package br.com.ofisy.ms_ofisy_stock.shared.securityfilter;

import br.com.ofisy.ms_ofisy_stock.shared.jwt.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private static final String TOKEN = "token";

    private JwtService jwtService;
    private JwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthFilter(jwtService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class WithoutBearerToken {

        @Test
        void shouldContinueWithoutAuthenticationWhenHeaderIsMissing() throws Exception {
            filter.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
        }

        @Test
        void shouldContinueWithoutAuthenticationWhenHeaderIsNotBearer() throws Exception {
            request.addHeader(JwtAuthFilter.AUTHORIZATION, "Basic dXNlcjpwYXNz");

            filter.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
        }
    }

    @Nested
    class WithBearerToken {

        @BeforeEach
        void setUp() {
            request.addHeader(JwtAuthFilter.AUTHORIZATION, JwtAuthFilter.BEARER + TOKEN);
        }

        @Test
        void shouldAuthenticateWithRoleFromToken() throws Exception {
            when(jwtService.isValidToken(TOKEN)).thenReturn(true);
            when(jwtService.extractEmail(TOKEN)).thenReturn("estoquista@ofisy.com");
            when(jwtService.extractRole(TOKEN)).thenReturn("STOCKMAN");

            filter.doFilter(request, response, filterChain);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication.getPrincipal()).isEqualTo("estoquista@ofisy.com");
            assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_STOCKMAN");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void shouldAuthenticateWithoutAuthoritiesWhenTokenHasNoRole() throws Exception {
            when(jwtService.isValidToken(TOKEN)).thenReturn(true);
            when(jwtService.extractEmail(TOKEN)).thenReturn("estoquista@ofisy.com");
            when(jwtService.extractRole(TOKEN)).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void shouldNotAuthenticateWhenTokenIsInvalid() throws Exception {
            when(jwtService.isValidToken(TOKEN)).thenReturn(false);

            filter.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }
}

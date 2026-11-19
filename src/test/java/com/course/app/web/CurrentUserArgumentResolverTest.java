package com.course.app.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supportsParameter_onlySupportsAnnotatedStringParameters() throws NoSuchMethodException {
        MethodParameter annotated = new MethodParameter(
                ControllerMethod.class.getDeclaredMethod("withCurrentUser", String.class), 0);
        MethodParameter unannotated = new MethodParameter(
                ControllerMethod.class.getDeclaredMethod("withoutCurrentUser", String.class), 0);
        MethodParameter wrongType = new MethodParameter(
                ControllerMethod.class.getDeclaredMethod("wrongType", Integer.class), 0);

        assertTrue(resolver.supportsParameter(annotated));
        assertFalse(resolver.supportsParameter(unannotated));
        assertFalse(resolver.supportsParameter(wrongType));
    }

    @Test
    void resolveArgument_authenticatedUser_returnsUsernameFromApplicationPrincipal() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(
                        new com.course.app.security.CurrentUser("alice", java.util.List.of("USER")), "n/a",
                        AuthorityUtils.createAuthorityList("ROLE_USER")));
        MethodParameter parameter = new MethodParameter(
                ControllerMethod.class.getDeclaredMethod("withCurrentUser", String.class), 0);

        Object actual = resolver.resolveArgument(parameter, null, request(), null);

        assertEquals("alice", actual);
    }

    @Test
    void resolveArgument_anonymousAuthentication_returnsAnonymous() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        MethodParameter parameter = new MethodParameter(
                ControllerMethod.class.getDeclaredMethod("withCurrentUser", String.class), 0);

        Object actual = resolver.resolveArgument(parameter, null, request(), null);

        assertEquals("anonymous", actual);
    }

    @Test
    void resolveArgument_noAuthentication_returnsAnonymous() throws Exception {
        SecurityContextHolder.clearContext();
        MethodParameter parameter = new MethodParameter(
                ControllerMethod.class.getDeclaredMethod("withCurrentUser", String.class), 0);

        Object actual = resolver.resolveArgument(parameter, null, request(), null);

        assertEquals("anonymous", actual);
    }

    private static ServletWebRequest request() {
        HttpServletRequest request = new MockHttpServletRequest();
        return new ServletWebRequest(request);
    }

    static class ControllerMethod {
        void withCurrentUser(@CurrentUser String username) {
        }

        void withoutCurrentUser(String username) {
        }

        void wrongType(@CurrentUser Integer username) {
        }
    }
}

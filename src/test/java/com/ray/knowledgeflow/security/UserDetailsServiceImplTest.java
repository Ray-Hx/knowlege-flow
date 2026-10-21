package com.ray.knowledgeflow.security;

import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.service.IUserService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    @Test
    void disabledDatabaseUserShouldNotBeAuthenticatable() {
        IUserService userService = mock(IUserService.class);
        SysUser user = new SysUser();
        user.setId(7L);
        user.setUsername("disabled-user");
        user.setPassword("hash");
        user.setStatus(0);
        when(userService.getByUsername("disabled-user")).thenReturn(user);
        when(userService.getUserRoles(7L)).thenReturn(List.of());

        UserPrincipal principal = (UserPrincipal) new UserDetailsServiceImpl(userService)
                .loadUserByUsername("disabled-user");

        assertFalse(principal.isEnabled());
    }
}

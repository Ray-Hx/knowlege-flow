package com.ray.knowledgeflow.module.user.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysUserSerializationTest {

    @Test
    void passwordHash_ShouldNeverBeSerializedInApiResponses() throws Exception {
        SysUser user = new SysUser();
        user.setUsername("alice");
        user.setPassword("bcrypt-hash");

        String json = new ObjectMapper().writeValueAsString(user);

        assertTrue(json.contains("alice"));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("bcrypt-hash"));
    }
}

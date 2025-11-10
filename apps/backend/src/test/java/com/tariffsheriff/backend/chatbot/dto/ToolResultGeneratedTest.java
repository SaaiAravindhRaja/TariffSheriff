package com.tariffsheriff.backend.chatbot.dto;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class ToolResultGeneratedTest {

    @Test
    void smoke_loadsClass_and_optionalInstantiate() throws Exception {
        Class<?> cls = Class.forName("com.tariffsheriff.backend.chatbot.dto.ToolResult", false, Thread.currentThread().getContextClassLoader());
        assertNotNull(cls);
        try {
            Constructor<?> ctor = cls.getDeclaredConstructor();
            // only instantiate if public no-arg to avoid heavy setups
            if (Modifier.isPublic(ctor.getModifiers()) && ctor.getParameterCount() == 0) {
                Object inst = ctor.newInstance();
                assertNotNull(inst);
            }
        } catch (NoSuchMethodException ignored) {
            // no no-arg ctor; that's fine for smoke test
        }
    }
}

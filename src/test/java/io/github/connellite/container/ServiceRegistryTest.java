package io.github.connellite.container;

import io.github.connellite.exception.ServiceNotRegisteredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceRegistryTest {

    private final ServiceRegistry registry = ServiceRegistry.getInstance();

    interface UserService {
        String findName(long id);
    }

    static final class UserServiceImpl implements UserService {
        private final String prefix;

        UserServiceImpl(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String findName(long id) {
            return prefix + id;
        }
    }

    static final class AnotherService {
    }

    @AfterEach
    void tearDown() {
        registry.clear();
    }

    @Test
    void get_interface_returns_lazy_proxy_resolved_on_first_use() {
        UserService proxy = registry.get(UserService.class);
        assertNotNull(proxy);
        assertFalse(registry.contains(UserService.class));

        ServiceNotRegisteredException ex = assertThrows(ServiceNotRegisteredException.class, () -> proxy.findName(42));
        assertEquals("Bean not registered: " + UserService.class.getName(), ex.getMessage());

        registry.register(UserService.class, new UserServiceImpl("user-"));
        assertEquals("user-42", proxy.findName(42));
    }

    @Test
    void get_interface_picks_up_replaced_registration() {
        UserService proxy = registry.get(UserService.class);

        registry.register(UserService.class, new UserServiceImpl("old-"));
        assertEquals("old-7", proxy.findName(7));

        registry.register(UserService.class, new UserServiceImpl("new-"));
        assertEquals("new-7", proxy.findName(7));
    }

    @Test
    void get_concrete_type_returns_registered_instance() {
        UserServiceImpl impl = new UserServiceImpl("user-");
        registry.register(UserServiceImpl.class, impl);

        assertSame(impl, registry.get(UserServiceImpl.class));
    }

    @Test
    void register_and_get_by_concrete_class_is_independent_from_interface() {
        UserServiceImpl impl = new UserServiceImpl("impl-");
        registry.register(UserServiceImpl.class, impl);

        assertSame(impl, registry.get(UserServiceImpl.class));
        assertEquals("impl-42", registry.get(UserServiceImpl.class).findName(42));
        assertTrue(registry.contains(UserServiceImpl.class));
        assertFalse(registry.contains(UserService.class));

        UserService interfaceProxy = registry.get(UserService.class);
        assertThrows(ServiceNotRegisteredException.class, () -> interfaceProxy.findName(1));

        registry.register(UserServiceImpl.class, new UserServiceImpl("replaced-"));
        assertEquals("replaced-7", registry.get(UserServiceImpl.class).findName(7));
    }

    @Test
    void get_concrete_type_throws_when_not_registered() {
        assertThrows(ServiceNotRegisteredException.class, () -> registry.get(UserServiceImpl.class));
    }

    @Test
    void get_interface_reuses_cached_proxy() {
        UserService first = registry.get(UserService.class);
        UserService second = registry.get(UserService.class);
        assertSame(first, second);
    }

    @Test
    void register_rejects_null_arguments() {
        assertThrows(NullPointerException.class, () -> registry.register(null, new UserServiceImpl("x")));
        assertThrows(NullPointerException.class, () -> registry.register(UserService.class, null));
    }

    @Test
    void register_rejects_incompatible_instance() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> registerRaw(UserService.class, new AnotherService())
        );
        assertTrue(ex.getMessage() != null && ex.getMessage().contains(AnotherService.class.getName()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerRaw(Class type, Object instance) {
        registry.register(type, instance);
    }

    @Test
    void unregister_removes_registration() {
        UserServiceImpl impl = new UserServiceImpl("user-");
        registry.register(UserService.class, impl);

        assertSame(impl, registry.unregister(UserService.class));
        assertFalse(registry.contains(UserService.class));
        assertNull(registry.unregister(UserService.class));

        UserService proxy = registry.get(UserService.class);
        assertThrows(ServiceNotRegisteredException.class, () -> proxy.findName(1));
    }

    @Test
    void registerIfAbsent_registers_only_once() {
        UserServiceImpl first = new UserServiceImpl("first-");
        UserServiceImpl second = new UserServiceImpl("second-");

        assertTrue(registry.registerIfAbsent(UserService.class, first));
        assertFalse(registry.registerIfAbsent(UserService.class, second));
        assertEquals("first-1", registry.get(UserService.class).findName(1));
    }

    @Test
    void create_returns_isolated_registry() {
        ServiceRegistry local = ServiceRegistry.create();
        registry.register(UserService.class, new UserServiceImpl("global-"));

        assertFalse(local.contains(UserService.class));
        local.register(UserService.class, new UserServiceImpl("local-"));

        assertEquals("global-1", registry.get(UserService.class).findName(1));
        assertEquals("local-1", local.get(UserService.class).findName(1));
    }
}

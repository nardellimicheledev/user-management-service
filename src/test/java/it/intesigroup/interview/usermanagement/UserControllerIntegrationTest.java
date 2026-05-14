package it.intesigroup.interview.usermanagement;

import it.intesigroup.interview.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateAndReadUserAsAdmin() throws Exception {
        String location = mockMvc.perform(post("/api/users")
                        .with(admin())
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "mrossi",
                                  "email": "Mario.Rossi@example.com",
                                  "taxCode": "RSSMRA80A01F205X",
                                  "name": "Mario",
                                  "surname": "Rossi",
                                  "roles": ["OWNER", "DEVELOPER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.email").value("mario.rossi@example.com"))
                .andExpect(jsonPath("$.taxCode").value("RSSMRA80A01F205X"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("OWNER", "DEVELOPER")))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(location).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mrossi"))
                .andExpect(jsonPath("$.taxCode").value("RSSMRA80A01F205X"));
    }

    @Test
    void shouldRejectDuplicatedEmail() throws Exception {
        createMario();

        mockMvc.perform(post("/api/users")
                        .with(admin())
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "mrossi2",
                                  "email": "mario.rossi@example.com",
                                  "taxCode": "RSSMRA80A01F205Y",
                                  "name": "Mario",
                                  "surname": "Rossi",
                                  "roles": ["OWNER"]
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldFilterFieldsForReaderUser() throws Exception {
        createMario();

        mockMvc.perform(get("/api/users").with(reader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("mario.rossi@example.com"))
                .andExpect(jsonPath("$[0].taxCode").doesNotExist())
                .andExpect(jsonPath("$[0].roles").doesNotExist());
    }

    @Test
    void shouldUpdateUserWithoutChangingEmail() throws Exception {
        String location = createMario();

        mockMvc.perform(put(location)
                        .with(admin())
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "mrossi-updated",
                                  "taxCode": "RSSMRA80A01F205X",
                                  "name": "Mario",
                                  "surname": "Rossi",
                                  "roles": ["REPORTER"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mrossi-updated"))
                .andExpect(jsonPath("$.email").value("mario.rossi@example.com"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("REPORTER")));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        String location = createMario();

        mockMvc.perform(delete(location).with(admin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(location).with(admin()))
                .andExpect(status().isNotFound());
    }

    private String createMario() throws Exception {
        return mockMvc.perform(post("/api/users")
                        .with(admin())
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "mrossi",
                                  "email": "mario.rossi@example.com",
                                  "taxCode": "RSSMRA80A01F205X",
                                  "name": "Mario",
                                  "surname": "Rossi",
                                  "roles": ["OWNER", "DEVELOPER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
    }

    @Test
    void shouldFilterTaxCodeButShowRolesForOperator() throws Exception {
        createMario();

        mockMvc.perform(get("/api/users").with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taxCode").doesNotExist())
                .andExpect(jsonPath("$[0].roles").exists());
    }

    @Test
    void shouldReturn400WhenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(admin())
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "",
                                  "email": "not-an-email",
                                  "taxCode": "TOOCORTO",
                                  "name": "",
                                  "surname": "",
                                  "roles": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.taxCode").exists());
    }

    @Test
    void shouldReturn403WhenReaderTriesToCreate() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(reader())
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "test",
                                  "email": "test@example.com",
                                  "taxCode": "TSTTST80A01H501Z",
                                  "name": "Test",
                                  "surname": "User",
                                  "roles": ["DEVELOPER"]
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenReaderTriesToUpdate() throws Exception {
        String location = createMario();

        mockMvc.perform(put(location)
                        .with(reader())
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "mrossi",
                                  "taxCode": "RSSMRA80A01F205X",
                                  "name": "Mario",
                                  "surname": "Rossi",
                                  "roles": ["OWNER"]
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenReaderTriesToDelete() throws Exception {
        String location = createMario();

        mockMvc.perform(delete(location).with(reader()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenOperatorTriesToDelete() throws Exception {
        String location = createMario();

        mockMvc.perform(delete(location).with(operator()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/api/users/00000000-0000-0000-0000-000000000000").with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
        mockMvc.perform(put("/api/users/00000000-0000-0000-0000-000000000000")
                        .with(admin())
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "mrossi",
                                  "taxCode": "RSSMRA80A01F205X",
                                  "name": "Mario",
                                  "surname": "Rossi",
                                  "roles": ["OWNER"]
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    private RequestPostProcessor admin() {
        return user("admin_user").authorities(
                new SimpleGrantedAuthority("ADMIN"),
                new SimpleGrantedAuthority("read_user"),
                new SimpleGrantedAuthority("create_user"),
                new SimpleGrantedAuthority("update_user"),
                new SimpleGrantedAuthority("delete_user")
        );
    }

    private RequestPostProcessor operator() {
        return user("creator_user").authorities(
                new SimpleGrantedAuthority("OPERATOR"),
                new SimpleGrantedAuthority("read_user"),
                new SimpleGrantedAuthority("create_user"),
                new SimpleGrantedAuthority("update_user")
        );
    }

    private RequestPostProcessor reader() {
        return user("reader_user").authorities(
                new SimpleGrantedAuthority("USER"),
                new SimpleGrantedAuthority("read_user")
        );
    }
}

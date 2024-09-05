package dev.codesoapbox.legacysystemexample.authentication.presentation.controllers;

import dev.codesoapbox.legacysystemexample.authentication.domain.model.SimpleUser;
import dev.codesoapbox.legacysystemexample.authentication.domain.model.User;
import dev.codesoapbox.legacysystemexample.authentication.domain.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping(value = AuthenticationControllerPaths.USERS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User debug", description = "Not required by the Keycloak migration plugin, created to help with debugging")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping()
    @Operation(summary = "Helper endpoint that returns all available test users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Complete user list")})
    public ResponseEntity<List<User>> getTestUsers() {
        var testUsers = userRepository.findAll();

        return ResponseEntity.ok(testUsers);
    }

    @PostMapping()
    @Operation(summary = "Helper endpoint that creates a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "409", description = "User already exists")})
    public ResponseEntity<Void> createUser(@RequestBody SimpleUser simpleUser) {
        String username = simpleUser.getFirstName().toLowerCase(Locale.ROOT);
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            return ResponseEntity.status(409).build();
        }
        User user = User.builder()
                .username(username)
                .email(username + "@example.com")
                .firstName(simpleUser.getFirstName())
                .lastName(simpleUser.getLastName())
                .password("password")
                .build();

        userRepository.save(user);

        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "Helper endpoint that deletes a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")})
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        userRepository.delete(user.get());

        return ResponseEntity.status(204).build();
    }
}

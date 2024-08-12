package dev.codesoapbox.legacysystemexample.authentication.presentation.controllers;

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
import java.util.Optional;

@RestController
@RequestMapping(value = AuthenticationControllerPaths.USERS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User debug", description = "Not required by the Keycloak migration plugin, created to help with debugging")
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
            @ApiResponse(responseCode = "201", description = "User created successfully")})
    public ResponseEntity<Void> createUser(@RequestBody User user) {
        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser.isPresent()) {
            return ResponseEntity.status(409).build();
        }
        userRepository.save(user);
        return ResponseEntity.status(201).build();
    }

}

package dev.codesoapbox.legacysystemexample.authentication.infrastructure.repositories;

import dev.codesoapbox.legacysystemexample.authentication.domain.model.User;
import dev.codesoapbox.legacysystemexample.authentication.domain.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class InMemoryUserRepository implements UserRepository {

    private List<User> users;

    public InMemoryUserRepository() {
        this.users = new ArrayList<>();
        users.add(generateUser("Lucy", "Brennan"));
        users.add(generateUser("Mark", "Brown"));
        users.add(generateUser("Kate", "Thomson"));
        users.add(generateUser("John", "Doe"));
    }


    protected User generateUser(String name, String lastName) {
        String username = name.toLowerCase(Locale.ROOT);

        return User.builder()
                .username(username)
                .email(username + "@example.com")
                .firstName(name)
                .lastName(lastName)
                .password("password")
                .build();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return this.users;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public void save(User user) {
        users.add(user);
    }

    @Override
    public void delete(User user) {
        users.remove(user);
    }
}

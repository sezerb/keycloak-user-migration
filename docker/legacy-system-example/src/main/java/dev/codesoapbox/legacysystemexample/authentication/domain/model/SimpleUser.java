package dev.codesoapbox.legacysystemexample.authentication.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleUser {
    private String firstName;
    private String lastName;
}

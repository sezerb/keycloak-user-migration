package com.danielfrak.code.keycloak.providers.rest;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;

import com.danielfrak.code.keycloak.providers.rest.remote.LegacyUser;
import com.danielfrak.code.keycloak.providers.rest.remote.LegacyUserService;
import com.danielfrak.code.keycloak.providers.rest.remote.UserModelFactory;

/**
 * Provides legacy user migration functionality
 */
public class LegacyProvider implements UserStorageProvider,
        UserLookupProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        ImportedUserValidation {

    private static final Logger LOG = Logger.getLogger(LegacyProvider.class);
    private static final Set<String> supportedCredentialTypes = Collections.singleton(PasswordCredentialModel.TYPE);
    private final KeycloakSession session;
    private final LegacyUserService legacyUserService;
    private final UserModelFactory userModelFactory;
    private final ComponentModel model;

    public LegacyProvider(KeycloakSession session, LegacyUserService legacyUserService,
                          UserModelFactory userModelFactory, ComponentModel model) {
        this.session = session;
        this.legacyUserService = legacyUserService;
        this.userModelFactory = userModelFactory;
        this.model = model;
    }


    private UserModel getUserModel(RealmModel realm, String username, Supplier<Optional<LegacyUser>> user) {
        return user.get()
                .filter(u -> {
                    // Make sure we're not trying to migrate users if they have changed their username
                    boolean duplicate = userModelFactory.isDuplicateUserId(u, realm);
                    if (duplicate) {
                        LOG.warnf("User with the same user id already exists: %s", u.getId());
                    }
                    return !duplicate;
                })
                .map(u -> userModelFactory.create(u, realm))
                .orElseGet(() -> {
                    LOG.warnf("User not found in external repository: %s", username);
                    return null;
                });
    }

    @Override
    public boolean isValid(RealmModel realmModel, UserModel userModel, CredentialInput input) {
        LOG.infof("isValid invoked for username=%s id=%s credentialType=%s", userModel.getUsername(), userModel.getId(), input == null ? "<null>" : input.getType());
        if (input == null) {
            LOG.infof("isValid early return: input is null for user=%s", userModel.getUsername());
            return false;
        }
        if (!supportsCredentialType(input.getType())) {
            LOG.infof("isValid early return: unsupported credentialType=%s for user=%s", input.getType(), userModel.getUsername());
            return false;
        }

        var userIdentifier = getUserIdentifier(userModel);
        LOG.infof("isValid checking legacy password for identifier=%s (may be username or id depending on config)", userIdentifier);

        if (!legacyUserService.isPasswordValid(userIdentifier, input.getChallengeResponse())) {
            LOG.infof("isValid password invalid for identifier=%s", userIdentifier);
            return false;
        }

        if (passwordDoesNotBreakPolicy(realmModel, userModel, input.getChallengeResponse())) {
            LOG.infof("isValid legacy password accepted and complies with policy for identifier=%s. Updating stored credential.", userIdentifier);
            userModel.credentialManager().updateCredential(input);
        } else {
            LOG.infof("isValid legacy password accepted but violates policy for identifier=%s. Adding UPDATE_PASSWORD required action.", userIdentifier);
            addUpdatePasswordAction(userModel, userIdentifier);
        }
        LOG.infof("isValid finished successfully for identifier=%s", userIdentifier);
        return true;
    }

    private String getUserIdentifier(UserModel userModel) {
        var userIdConfig = model.getConfig().getFirst(ConfigurationProperties.USE_USER_ID_FOR_CREDENTIAL_VERIFICATION);
        var useUserId = Boolean.parseBoolean(userIdConfig);
        return useUserId ? userModel.getId() : userModel.getUsername();
    }

    private boolean passwordDoesNotBreakPolicy(RealmModel realmModel, UserModel userModel, String password) {
        PasswordPolicyManagerProvider passwordPolicyManagerProvider = session.getProvider(
                PasswordPolicyManagerProvider.class);
        PolicyError error = passwordPolicyManagerProvider
                .validate(realmModel, userModel, password);

        return error == null;
    }

    private void addUpdatePasswordAction(UserModel userModel, String userIdentifier) {
        if (updatePasswordActionMissing(userModel)) {
            LOG.infof("Could not use legacy password for user %s due to password policy." +
                            " Adding UPDATE_PASSWORD action.",
                    userIdentifier);
            userModel.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        }
    }

    private boolean updatePasswordActionMissing(UserModel userModel) {
        return userModel.getRequiredActionsStream()
                .noneMatch(s -> s.contains(UserModel.RequiredAction.UPDATE_PASSWORD.name()));
    }

    @Override
    public boolean supportsCredentialType(String s) {
        return supportedCredentialTypes.contains(s);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realmModel, UserModel userModel, String s) {
        LOG.infof("isConfiguredFor invoked for username=%s id=%s credentialType=%s", userModel.getUsername(), userModel.getId(), s);
        if (!supportsCredentialType(s)) {
            LOG.infof("isConfiguredFor returning false: unsupported credentialType=%s for user=%s", s, userModel.getUsername());
            return false;
        }
        boolean configured = userModel.credentialManager().getStoredCredentialsStream()
                .anyMatch(c -> s.equals(c.getType()));
        LOG.infof("isConfiguredFor result for user=%s credentialType=%s -> %s", userModel.getUsername(), s, configured);
        return configured;
    }

    @Override
    public void close() {
        // Not needed
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
//        severFederationLink(user);
        return false;
    }

    @SuppressWarnings("unused")
    private void severFederationLink(UserModel user) {
        LOG.info("Severing federation link for " + user.getUsername());
        String link = user.getFederationLink();
        if (link != null && !link.isBlank()) {
            user.setFederationLink(null);
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        // Not needed
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realmModel, UserModel userModel) {
        return Stream.empty();
    }

    @Override
    public UserModel getUserById(RealmModel realmModel, String s) {
        throw new UnsupportedOperationException("User lookup by id not implemented");
    }

    @Override
    public UserModel getUserByUsername(RealmModel realmModel, String username) {
        return getUserModel(realmModel, username, () -> legacyUserService.findByUsername(username));
    }

    @Override
    public UserModel getUserByEmail(RealmModel realmModel, String email) {
        return getUserModel(realmModel, email, () -> legacyUserService.findByEmail(email));
    }

    @Override
    public UserModel validate(RealmModel realmModel, UserModel userModel) {
        if(legacyUserService.findByUsername(userModel.getUsername()).isEmpty()) {
            return null;
        }
        return userModel;
    }
}

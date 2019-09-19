package com.qa.portal.user.keycloak;

import com.qa.portal.common.dto.QaUserDetailsDto;
import com.qa.portal.common.exception.QaMultiStepCommitContext;
import com.qa.portal.common.exception.QaPortalBusinessException;
import com.qa.portal.common.exception.QaPortalMultiStepCommitException;
import com.qa.portal.common.keycloak.KeycloakAdminClient;
import com.qa.portal.user.keycloak.mapper.KeycloakUserMapper;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.qa.portal.user.keycloak.KeycloakUserConstants.*;

@Component
public class KeycloakUserResourceManager {

    private final Logger LOGGER = LoggerFactory.getLogger(KeycloakUserResourceManager.class);

    private KeycloakUserMapper keycloakUserMapper;

    private KeycloakUserValidator keycloakUserValidator;

    private KeycloakUserFactory keycloakUserFactory;

    private KeycloakAdminClient keycloakAdminClient;

    public KeycloakUserResourceManager(KeycloakUserMapper keycloakUserMapper,
                                       KeycloakUserValidator keycloakUserValidator,
                                       KeycloakUserFactory keycloakUserFactory,
                                       KeycloakAdminClient keycloakAdminClient) {
        this.keycloakUserMapper = keycloakUserMapper;
        this.keycloakUserValidator = keycloakUserValidator;
        this.keycloakUserFactory = keycloakUserFactory;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public List<QaUserDetailsDto> getAllUsers() {
        return keycloakAdminClient.getRealm().users().list().stream()
                .map(u -> keycloakUserMapper.mapToUserDetailsDto(u))
                .collect(Collectors.toList());
    }

    public void createUserAndRole(QaUserDetailsDto qaUserDetailsDto) {
        try {
            UserRepresentation userRepresentation = createUser(qaUserDetailsDto);
            RoleRepresentation roleRepresentation = getRoleRepresentation(qaUserDetailsDto.getRoleName());
            assignRoleToUser(userRepresentation, roleRepresentation);
        } catch (Exception e) {
            throw new QaPortalMultiStepCommitException(new QaMultiStepCommitContext(this.getClass().getName(),
                    qaUserDetailsDto,
                    QaUserDetailsDto.class,
                    2), e.getMessage());
        }
    }

    public UserRepresentation createUser(QaUserDetailsDto userDetails) {
        keycloakUserValidator.validateUser(userDetails);
        UserRepresentation userRepresentation = keycloakUserFactory.createKeycloakUser(userDetails.getUser());
        keycloakAdminClient.getRealm().users().create(userRepresentation);
        return getUserRepresentation(userRepresentation).orElseGet(() -> userRepresentation);
    }

    public UserRepresentation updateUser(QaUserDetailsDto userDetails) {
        keycloakUserValidator.validateUser(userDetails);
        UserResource userResource = keycloakAdminClient.getRealm().users().get(userDetails.getUser().getUserName());
        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setEmail(userDetails.getUser().getEmail());
        userRepresentation.setFirstName(userDetails.getUser().getFirstName());
        userRepresentation.setLastName(userDetails.getUser().getLastName());
        updateUserRoles(userResource, userDetails);
        userResource.update(userRepresentation);
        return userRepresentation;
    }

    public void deleteUser(String userName) {
        Response response = keycloakAdminClient.getRealm().users().delete(userName);
        if (response.getStatus() != HttpStatus.OK.value()) {
            throw new QaPortalBusinessException("Error deleting user");
        }
    }

    public void assignRoleToUser(UserRepresentation userRepresentation, RoleRepresentation roleRepresentation) {
        UserResource userResource = keycloakAdminClient.getRealm().users().get(userRepresentation.getId());
        userResource.roles().realmLevel().add(Arrays.asList(roleRepresentation));
    }

    private void updateUserRoles(UserResource userResource,
                                 QaUserDetailsDto userDetailsDto) {
        List<RoleRepresentation> allRoles = userResource.roles().realmLevel().listAll();
        Optional<RoleRepresentation> portalRole = allRoles.stream()
                .filter(r -> isPortalRole(r.getName()))
                .findAny();
        portalRole.ifPresent(r -> updateRole(userResource, r.getName(), userDetailsDto.getRoleName()));
    }

    private boolean isPortalRole(String roleName) {
        return !roleName.equals(UMA_AUTH_ROLE) &&
                !roleName.equals(OFFLINE_ACCESS_ROLE) &&
                !roleName.startsWith((COHORT_ROLE_PREFIX));
    }

    private void updateRole(UserResource userResource,
                            String existingPortalRole,
                            String newPortalRole) {
        if (!existingPortalRole.equals(newPortalRole)) {
            // Delete old role
            List<RoleRepresentation> rolesToDelete = Stream.of(existingPortalRole)
                    .map(r -> getRoleRepresentation(r))
                    .collect(Collectors.toList());

            userResource.roles().realmLevel().remove(rolesToDelete);

            // Assign new role
            List<RoleRepresentation> rolesToAdd = Stream.of(newPortalRole)
                    .map(r -> getRoleRepresentation(r))
                    .collect(Collectors.toList());
            userResource.roles().realmLevel().add(rolesToAdd);
        }
    }

    private Optional<UserRepresentation> getUserRepresentation(UserRepresentation userRepresentation) {
        return keycloakAdminClient.getRealm().users().list().stream()
                .filter(u -> u.getUsername().equals(userRepresentation.getUsername()))
                .findFirst();
    }

    private RoleRepresentation getRoleRepresentation(String roleName) {
        return keycloakAdminClient.getRealm().roles().list().stream()
                .filter(r -> r.getName().equals(roleName))
                .findFirst()
                .orElseThrow(() -> new QaPortalBusinessException("No role found for supplied role name"));
    }
}

package pl.wizytowka.identity.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.wizytowka.identity.application.port.in.AdminUserView;
import pl.wizytowka.identity.application.port.in.ManageAdminUsersUseCase;
import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUserId;

import java.util.List;
import java.util.UUID;

/** Zarządzanie kontami panelu — dostęp tylko dla roli ADMIN (reguła w SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController {

    private final ManageAdminUsersUseCase manageUsers;

    AdminUserController(ManageAdminUsersUseCase manageUsers) {
        this.manageUsers = manageUsers;
    }

    @GetMapping
    List<AdminUserView> list() {
        return manageUsers.list();
    }

    @PostMapping
    ResponseEntity<CreatedResponse> create(@Valid @RequestBody CreateAdminRequest request) {
        AdminUserId id = manageUsers.create(request.email(), request.displayName(), request.role(), request.password());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                        .buildAndExpand(id.value()).toUri())
                .body(new CreatedResponse(id.value()));
    }

    @PutMapping("/{id}")
    ResponseEntity<Void> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody UpdateAdminRequest request) {
        manageUsers.update(actor(jwt), AdminUserId.of(id), request.version(), request.displayName(), request.role());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enable")
    ResponseEntity<Void> enable(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        manageUsers.setEnabled(actor(jwt), AdminUserId.of(id), true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/disable")
    ResponseEntity<Void> disable(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        manageUsers.setEnabled(actor(jwt), AdminUserId.of(id), false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/password")
    ResponseEntity<Void> resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        manageUsers.resetPassword(AdminUserId.of(id), request.password());
        return ResponseEntity.noContent().build();
    }

    private static AdminUserId actor(Jwt jwt) {
        return AdminUserId.of(UUID.fromString(jwt.getSubject()));
    }

    record CreateAdminRequest(@NotBlank @Email @Size(max = 254) String email,
                              @NotBlank @Size(max = 120) String displayName,
                              @NotNull AdminRole role,
                              @NotBlank @Size(max = 1024) String password) {

        @Override
        public String toString() {
            return "CreateAdminRequest[email=" + email + ", role=" + role + "]";
        }
    }

    record UpdateAdminRequest(@NotBlank @Size(max = 120) String displayName,
                              @NotNull AdminRole role,
                              @NotNull @PositiveOrZero Long version) {
    }

    record ResetPasswordRequest(@NotBlank @Size(max = 1024) String password) {

        @Override
        public String toString() {
            return "ResetPasswordRequest[***]";
        }
    }

    record CreatedResponse(UUID id) {
    }
}

package pl.wizytowka.identity.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.identity.application.port.in.AdminBuyerView;
import pl.wizytowka.identity.application.port.in.BuyerFilter;
import pl.wizytowka.identity.application.port.in.BuyerSummaryView;
import pl.wizytowka.identity.application.port.in.ManageBuyersUseCase;
import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;

import java.util.List;
import java.util.UUID;

/** Wnioski i konta kupców w panelu (ADMIN, EDITOR — reguła w SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/buyers")
class AdminBuyerController {

    private final ManageBuyersUseCase buyers;

    AdminBuyerController(ManageBuyersUseCase buyers) {
        this.buyers = buyers;
    }

    @GetMapping
    PageResponse<BuyerSummaryView> list(@RequestParam(required = false) BuyerStatus status,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(buyers.list(new BuyerFilter(status, q), new PageQuery(page, size)));
    }

    @GetMapping("/pending-count")
    PendingCountResponse pendingCount() {
        return new PendingCountResponse(buyers.countPendingApproval());
    }

    @GetMapping("/{id}")
    AdminBuyerView get(@PathVariable UUID id) {
        return buyers.get(BuyerAccountId.of(id));
    }

    @PostMapping("/{id}/approve")
    ResponseEntity<Void> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody DecisionRequest request) {
        buyers.approve(actor(jwt), BuyerAccountId.of(id), request.version());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    ResponseEntity<Void> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody DecisionRequest request) {
        buyers.reject(actor(jwt), BuyerAccountId.of(id), request.version(), request.note());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/suspend")
    ResponseEntity<Void> suspend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody DecisionRequest request) {
        buyers.suspend(actor(jwt), BuyerAccountId.of(id), request.version(), request.note());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    ResponseEntity<Void> reactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                    @Valid @RequestBody DecisionRequest request) {
        buyers.reactivate(actor(jwt), BuyerAccountId.of(id), request.version());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        buyers.delete(BuyerAccountId.of(id));
        return ResponseEntity.noContent().build();
    }

    private static AdminUserId actor(Jwt jwt) {
        return AdminUserId.of(UUID.fromString(jwt.getSubject()));
    }

    /** @param note notatka wewnętrzna (odrzucenie, zawieszenie) — nie trafia do kupca */
    record DecisionRequest(@NotNull @PositiveOrZero Long version, @Size(max = 500) String note) {
    }

    record PendingCountResponse(long count) {
    }

    record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

        static <T> PageResponse<T> of(PageResult<T> result) {
            return new PageResponse<>(result.items(), result.page(), result.size(), result.totalItems(),
                    result.totalPages());
        }
    }
}

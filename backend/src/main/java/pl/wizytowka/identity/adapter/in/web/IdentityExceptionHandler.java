package pl.wizytowka.identity.adapter.in.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.wizytowka.identity.application.port.in.AccountNotActiveException;
import pl.wizytowka.identity.application.port.in.AuthenticationFailedException;

/** Mapuje błędy uwierzytelnienia przed ogólnym handlerem API (który obsługuje {@code Exception}). */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class IdentityExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ProblemDetail> handleAuthenticationFailed(AuthenticationFailedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        problem.setProperty("code", e.code());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /** Hasło poprawne, konto nieaktywne: {@code status} pozwala frontendowi wyjaśnić, na co kupiec czeka. */
    @ExceptionHandler(AccountNotActiveException.class)
    ResponseEntity<ProblemDetail> handleAccountNotActive(AccountNotActiveException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Buyer account is not active");
        problem.setProperty("code", AccountNotActiveException.CODE);
        problem.setProperty("status", e.status().name());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }
}

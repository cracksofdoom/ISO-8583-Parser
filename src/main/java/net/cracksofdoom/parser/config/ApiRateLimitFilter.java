package net.cracksofdoom.parser.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-instance abuse control. Use an edge or shared limiter for multi-instance
 * deployments.
 */
@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT = 60;
    private static final long WINDOW_MILLIS = 60_000;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    private record Window(long started, int count) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || !"POST".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = clock.millis();
        if (windows.size() > 10_000)
            windows.entrySet().removeIf(entry -> now - entry.getValue().started() >= WINDOW_MILLIS);
        Window window = windows.compute(request.getRemoteAddr(),
                (key, previous) -> previous == null || now - previous.started() >= WINDOW_MILLIS
                        ? new Window(now, 1)
                        : new Window(previous.started(), previous.count() + 1));
        if (window.count() > LIMIT) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}

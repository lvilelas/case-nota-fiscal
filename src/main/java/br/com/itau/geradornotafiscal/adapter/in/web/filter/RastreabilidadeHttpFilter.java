package br.com.itau.geradornotafiscal.adapter.in.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RastreabilidadeHttpFilter extends OncePerRequestFilter {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String FLOW_ID_HEADER = "X-Flow-Id";
    public static final String CORRELATION_ID_MDC = "correlationId";
    public static final String FLOW_ID_MDC = "flowId";

    private static final Pattern IDENTIFICADOR_SEGURO =
            Pattern.compile("[A-Za-z0-9._-]{1,100}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolverIdentificador(request.getHeader(CORRELATION_ID_HEADER));
        String flowId = resolverIdentificador(request.getHeader(FLOW_ID_HEADER));

        MDC.put(CORRELATION_ID_MDC, correlationId);
        MDC.put(FLOW_ID_MDC, flowId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(FLOW_ID_HEADER, flowId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC);
            MDC.remove(FLOW_ID_MDC);
        }
    }

    private String resolverIdentificador(String identificadorRecebido) {
        if (identificadorRecebido != null
                && IDENTIFICADOR_SEGURO.matcher(identificadorRecebido).matches()) {
            return identificadorRecebido;
        }
        return UUID.randomUUID().toString();
    }
}

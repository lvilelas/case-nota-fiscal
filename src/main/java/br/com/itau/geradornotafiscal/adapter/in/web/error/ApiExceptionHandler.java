package br.com.itau.geradornotafiscal.adapter.in.web.error;

import br.com.itau.geradornotafiscal.application.exception.PublicacaoEventoException;
import br.com.itau.geradornotafiscal.application.exception.PersistenciaNotaFiscalException;
import br.com.itau.geradornotafiscal.domain.exception.RegimeTributacaoNaoSuportadoException;
import br.com.itau.geradornotafiscal.domain.exception.TipoPessoaNaoSuportadoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> tratarValidacao(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<CampoInvalido> campos = exception.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoInvalido(paraSnakeCase(erro.getField()), erro.getDefaultMessage()))
                .toList();
        LOGGER.warn("Payload inválido: path={}, quantidadeErros={}", request.getRequestURI(), campos.size());
        return resposta(
                HttpStatus.BAD_REQUEST,
                "PAYLOAD_INVALIDO",
                "O payload possui campos inválidos",
                request,
                campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> tratarMensagemIlegivel(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        LOGGER.warn("Payload ilegível: path={}", request.getRequestURI());
        return resposta(
                HttpStatus.BAD_REQUEST,
                "PAYLOAD_ILEGIVEL",
                "O corpo da requisição não pôde ser interpretado",
                request,
                List.of());
    }

    @ExceptionHandler({
            RegimeTributacaoNaoSuportadoException.class,
            TipoPessoaNaoSuportadoException.class
    })
    public ResponseEntity<ApiError> tratarRegraNegocio(
            RuntimeException exception,
            HttpServletRequest request) {
        LOGGER.warn("Regra de negócio não atendida: path={}, motivo={}", request.getRequestURI(), exception.getMessage());
        return resposta(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "REGRA_NEGOCIO_INVALIDA",
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(PublicacaoEventoException.class)
    public ResponseEntity<ApiError> tratarIndisponibilidadeMensageria(
            PublicacaoEventoException exception,
            HttpServletRequest request) {
        LOGGER.error("Mensageria indisponível: path={}", request.getRequestURI(), exception);
        return resposta(
                HttpStatus.SERVICE_UNAVAILABLE,
                "MENSAGERIA_INDISPONIVEL",
                "Não foi possível concluir a publicação do evento",
                request,
                List.of());
    }

    @ExceptionHandler(PersistenciaNotaFiscalException.class)
    public ResponseEntity<ApiError> tratarIndisponibilidadePersistencia(
            PersistenciaNotaFiscalException exception,
            HttpServletRequest request) {
        LOGGER.error("Persistência indisponível: path={}", request.getRequestURI(), exception);
        return resposta(
                HttpStatus.SERVICE_UNAVAILABLE,
                "PERSISTENCIA_INDISPONIVEL",
                "Não foi possível persistir o processamento da nota fiscal",
                request,
                List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> tratarErroInesperado(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error("Erro inesperado: path={}", request.getRequestURI(), exception);
        return resposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERRO_INTERNO",
                "Ocorreu um erro inesperado",
                request,
                List.of());
    }

    private ResponseEntity<ApiError> resposta(
            HttpStatus status,
            String codigo,
            String mensagem,
            HttpServletRequest request,
            List<CampoInvalido> camposInvalidos) {
        ApiError erro = new ApiError(
                OffsetDateTime.now(),
                status.value(),
                codigo,
                mensagem,
                request.getRequestURI(),
                MDC.get("correlationId"),
                MDC.get("flowId"),
                camposInvalidos);
        return ResponseEntity.status(status).body(erro);
    }

    private String paraSnakeCase(String campo) {
        return campo
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
    }
}

package br.edu.registrofuncionarios.exception;

import java.util.List;


public class ValidacaoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> erros;

    public ValidacaoException(String mensagem) {
        this(List.of(mensagem));
    }

    public ValidacaoException(List<String> erros) {
        super(String.join(" ", erros));
        this.erros = List.copyOf(erros);
    }

    public List<String> getErros() {
        return erros;
    }
}

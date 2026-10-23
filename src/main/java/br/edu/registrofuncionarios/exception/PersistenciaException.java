package br.edu.registrofuncionarios.exception;

/**
 * Lançada quando ocorre uma falha de leitura/escrita no arquivo de dados.
 */
public class PersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

package br.edu.registrofuncionarios.model;

import br.edu.registrofuncionarios.exception.ValidacaoException;
import br.edu.registrofuncionarios.util.Validador;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public final class Endereco {

    private final String logradouro;
    private final String numero;
    private final String complemento;
    private final String bairro;
    private final String cidade;
    private final String estado;
    private final String cep;

    public Endereco(String logradouro, String numero, String complemento, String bairro,
                    String cidade, String estado, String cep) {
        List<String> erros = validar(logradouro, numero, complemento, bairro, cidade, estado, cep);
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
        this.logradouro = Validador.normalizarTexto(logradouro);
        this.numero = Validador.normalizarTexto(numero);
        this.complemento = Validador.normalizarTexto(complemento);
        this.bairro = Validador.normalizarTexto(bairro);
        this.cidade = Validador.normalizarTexto(cidade);
        this.estado = estado.strip().toUpperCase(Locale.ROOT);
        this.cep = Validador.normalizarCep(cep.strip());
    }


    public static List<String> validar(String logradouro, String numero, String complemento, String bairro,
                                       String cidade, String estado, String cep) {
        List<String> erros = new ArrayList<>();
        verificarObrigatorio(erros, "Logradouro", logradouro, 100, false);
        verificarObrigatorio(erros, "Número", numero, 10, false);
        if (Validador.normalizarTexto(complemento).length() > 60) {
            erros.add("Complemento deve ter no máximo 60 caracteres.");
        }
        verificarObrigatorio(erros, "Bairro", bairro, 60, false);
        verificarObrigatorio(erros, "Cidade", cidade, 60, true);
        if (!Validador.ufValida(estado)) {
            erros.add("UF inválida (informe a sigla de um estado brasileiro, ex.: GO).");
        }
        if (!Validador.cepValido(cep)) {
            erros.add("CEP inválido (use 00000-000 ou 8 dígitos).");
        }
        return erros;
    }

    private static void verificarObrigatorio(List<String> erros, String campo, String valor, int maximo,
                                             boolean feminino) {
        String texto = Validador.normalizarTexto(valor);
        if (texto.isEmpty()) {
            erros.add(campo + (feminino ? " deve ser informada." : " deve ser informado."));
        } else if (texto.length() > maximo) {
            erros.add(campo + " deve ter no máximo " + maximo + " caracteres.");
        }
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getEstado() {
        return estado;
    }

    public String getCep() {
        return cep;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Endereco e)) {
            return false;
        }
        return logradouro.equals(e.logradouro) && numero.equals(e.numero) && complemento.equals(e.complemento)
                && bairro.equals(e.bairro) && cidade.equals(e.cidade) && estado.equals(e.estado) && cep.equals(e.cep);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logradouro, numero, complemento, bairro, cidade, estado, cep);
    }

    @Override
    public String toString() {
        String comp = complemento.isEmpty() ? "" : " (" + complemento + ")";
        return logradouro + ", " + numero + comp + " - " + bairro + ", " + cidade + "/" + estado + " - CEP " + cep;
    }
}

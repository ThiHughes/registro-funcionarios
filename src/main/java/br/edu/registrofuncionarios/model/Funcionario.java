package br.edu.registrofuncionarios.model;

import br.edu.registrofuncionarios.exception.ValidacaoException;
import br.edu.registrofuncionarios.util.Validador;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class Funcionario {

    public static final int IDADE_MINIMA = 18;
    public static final int NOME_TAMANHO_MINIMO = 3;
    public static final int NOME_TAMANHO_MAXIMO = 100;
    public static final int CARGO_TAMANHO_MAXIMO = 50;

    private final String matricula;
    private final String nome;
    private final String cpf;
    private final LocalDate dataNascimento;
    private final String cargo;
    private final BigDecimal salario;
    private final LocalDate dataContratacao;
    private final Endereco endereco;

    public Funcionario(String matricula, String nome, String cpf, LocalDate dataNascimento, String cargo,
                       BigDecimal salario, LocalDate dataContratacao, Endereco endereco) {
        List<String> erros = validar(matricula, nome, cpf, dataNascimento, cargo, salario, dataContratacao);
        if (endereco == null) {
            erros.add("Endereço deve ser informado.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
        this.matricula = matricula.strip();
        this.nome = Validador.normalizarTexto(nome);
        this.cpf = Validador.normalizarCpf(cpf.strip());
        this.dataNascimento = dataNascimento;
        this.cargo = Validador.normalizarTexto(cargo);
        this.salario = salario.setScale(2, RoundingMode.HALF_UP);
        this.dataContratacao = dataContratacao;
        this.endereco = endereco;
    }


    public static List<String> validar(String matricula, String nome, String cpf, LocalDate dataNascimento,
                                       String cargo, BigDecimal salario, LocalDate dataContratacao) {
        List<String> erros = new ArrayList<>();
        LocalDate hoje = LocalDate.now();

        if (!Validador.matriculaValida(matricula)) {
            erros.add("Matrícula deve conter exatamente 6 dígitos numéricos.");
        }

        String nomeNormalizado = Validador.normalizarTexto(nome);
        if (nomeNormalizado.length() < NOME_TAMANHO_MINIMO) {
            erros.add("Nome deve ter pelo menos " + NOME_TAMANHO_MINIMO + " caracteres.");
        } else if (nomeNormalizado.length() > NOME_TAMANHO_MAXIMO) {
            erros.add("Nome deve ter no máximo " + NOME_TAMANHO_MAXIMO + " caracteres.");
        } else if (!Validador.nomeComCaracteresValidos(nomeNormalizado)) {
            erros.add("Nome deve conter apenas letras, espaços, pontos, apóstrofos e hífens.");
        }

        if (!Validador.cpfValido(cpf)) {
            erros.add("CPF inválido (use 000.000.000-00 ou 11 dígitos e informe um CPF com dígitos verificadores corretos).");
        }

        if (dataNascimento == null) {
            erros.add("Data de nascimento inválida ou não informada (use dd/mm/aaaa).");
        } else if (Validador.idade(dataNascimento, hoje) < IDADE_MINIMA) {
            erros.add("O funcionário deve ter pelo menos " + IDADE_MINIMA + " anos.");
        }

        String cargoNormalizado = Validador.normalizarTexto(cargo);
        if (cargoNormalizado.isEmpty()) {
            erros.add("Cargo deve ser informado.");
        } else if (cargoNormalizado.length() > CARGO_TAMANHO_MAXIMO) {
            erros.add("Cargo deve ter no máximo " + CARGO_TAMANHO_MAXIMO + " caracteres.");
        }

        if (salario == null || salario.signum() <= 0) {
            erros.add("Salário deve ser informado e maior que zero.");
        }

        if (dataContratacao == null) {
            erros.add("Data de contratação inválida ou não informada (use dd/mm/aaaa).");
        } else if (dataContratacao.isAfter(hoje)) {
            erros.add("Data de contratação não pode ser futura.");
        } else if (dataNascimento != null && dataContratacao.isBefore(dataNascimento.plusYears(IDADE_MINIMA))) {
            erros.add("Data de contratação não pode ser anterior aos " + IDADE_MINIMA + " anos do funcionário.");
        }

        return erros;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getCargo() {
        return cargo;
    }

    public BigDecimal getSalario() {
        return salario;
    }

    public LocalDate getDataContratacao() {
        return dataContratacao;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    @Override
    public boolean equals(Object outro) {
        return this == outro || (outro instanceof Funcionario f && matricula.equals(f.matricula));
    }

    @Override
    public int hashCode() {
        return Objects.hash(matricula);
    }

    @Override
    public String toString() {
        return "Funcionario{matricula=" + matricula + ", nome=" + nome + ", cargo=" + cargo + "}";
    }
}

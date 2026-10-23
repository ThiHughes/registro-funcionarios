package br.edu.registrofuncionarios.service;

import br.edu.registrofuncionarios.exception.PersistenciaException;
import br.edu.registrofuncionarios.exception.ValidacaoException;
import br.edu.registrofuncionarios.model.Funcionario;
import br.edu.registrofuncionarios.persistence.FuncionarioRepository;
import br.edu.registrofuncionarios.util.Validador;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Regras de negócio do cadastro: operações de cadastrar, excluir, consultar e listar.
 * Mantém os dados em memória e grava no repositório após cada alteração
 * (se a gravação falhar, a alteração em memória é desfeita).
 */
public class FuncionarioService {

    private static final int MAIOR_MATRICULA = 999_999;

    private final FuncionarioRepository repositorio;
    private final List<Funcionario> funcionarios;
    private final List<String> avisosCarga;

    /** Carrega os dados existentes ao ser criado. */
    public FuncionarioService(FuncionarioRepository repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
        FuncionarioRepository.ResultadoCarga carga = repositorio.carregar();
        this.funcionarios = new ArrayList<>(carga.funcionarios());
        this.funcionarios.sort(Comparator.comparing(Funcionario::getMatricula));
        this.avisosCarga = List.copyOf(carga.avisos());
    }

    /** Avisos gerados ao ler o arquivo (linhas inválidas ignoradas, por exemplo). */
    public List<String> getAvisosCarga() {
        return avisosCarga;
    }

    /** Lista todos os funcionários, ordenados por matrícula. */
    public List<Funcionario> listarTodos() {
        return List.copyOf(funcionarios);
    }

    /** Consulta um funcionário específico pela matrícula. */
    public Optional<Funcionario> buscarPorMatricula(String matricula) {
        String m = normalizarMatricula(matricula);
        return funcionarios.stream().filter(f -> f.getMatricula().equals(m)).findFirst();
    }

    /**
     * Busca combinando filtros opcionais (critério vazio ou nulo é ignorado):
     * matrícula (exata; "42" equivale a "000042"), cargo e estado (sem diferenciar maiúsculas/minúsculas).
     */
    public List<Funcionario> buscar(String matricula, String cargo, String estado) {
        String m = normalizarMatricula(matricula);
        String c = Validador.normalizarTexto(cargo);
        String e = Validador.normalizarTexto(estado);
        return funcionarios.stream()
                .filter(f -> m.isEmpty() || f.getMatricula().equals(m))
                .filter(f -> c.isEmpty() || f.getCargo().equalsIgnoreCase(c))
                .filter(f -> e.isEmpty() || f.getEndereco().getEstado().equalsIgnoreCase(e))
                .toList();
    }

    /** Cargos distintos existentes, em ordem alfabética. */
    public List<String> listarCargos() {
        TreeSet<String> cargos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        funcionarios.stream().map(Funcionario::getCargo).forEach(cargos::add);
        return new ArrayList<>(cargos);
    }

    /** Próxima matrícula livre (maior existente + 1), com 6 dígitos. */
    public String proximaMatricula() {
        int maior = funcionarios.stream()
                .map(Funcionario::getMatricula)
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        if (maior >= MAIOR_MATRICULA) {
            throw new ValidacaoException("Limite de matrículas (" + MAIOR_MATRICULA + ") atingido.");
        }
        return String.format("%06d", maior + 1);
    }

    /**
     * Cadastra um novo funcionário e grava o arquivo.
     *
     * @throws ValidacaoException   se a matrícula ou o CPF já estiverem cadastrados
     * @throws PersistenciaException se não for possível gravar o arquivo
     */
    public void cadastrar(Funcionario novo) {
        Objects.requireNonNull(novo, "funcionário");
        if (funcionarios.stream().anyMatch(f -> f.getMatricula().equals(novo.getMatricula()))) {
            throw new ValidacaoException("Já existe um funcionário com a matrícula " + novo.getMatricula() + ".");
        }
        if (funcionarios.stream().anyMatch(f -> f.getCpf().equals(novo.getCpf()))) {
            throw new ValidacaoException("Já existe um funcionário cadastrado com o CPF " + novo.getCpf() + ".");
        }
        funcionarios.add(novo);
        try {
            repositorio.salvar(funcionarios);
        } catch (PersistenciaException e) {
            funcionarios.remove(novo);
            throw e;
        }
    }

    /**
     * Exclui o funcionário da matrícula informada e grava o arquivo.
     *
     * @throws ValidacaoException   se a matrícula não existir
     * @throws PersistenciaException se não for possível gravar o arquivo
     */
    public void excluir(String matricula) {
        Funcionario alvo = buscarPorMatricula(matricula)
                .orElseThrow(() -> new ValidacaoException("Funcionário de matrícula " + matricula + " não encontrado."));
        int posicao = funcionarios.indexOf(alvo);
        funcionarios.remove(posicao);
        try {
            repositorio.salvar(funcionarios);
        } catch (PersistenciaException e) {
            funcionarios.add(posicao, alvo);
            throw e;
        }
    }

    private static String normalizarMatricula(String matricula) {
        String m = Validador.normalizarTexto(matricula);
        if (m.matches("\\d{1,6}")) {
            return "0".repeat(6 - m.length()) + m;
        }
        return m;
    }
}

package br.edu.registrofuncionarios.service;

import br.edu.registrofuncionarios.exception.ValidacaoException;
import br.edu.registrofuncionarios.model.Funcionario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Relatórios sobre a lista de funcionários, todos implementados com a API Stream
 * (filter, map, reduce, sorted, groupingBy, max/min...). Não altera os dados recebidos.
 */
public class RelatorioService {

    private static final Comparator<Funcionario> POR_NOME =
            Comparator.comparing(Funcionario::getNome, String.CASE_INSENSITIVE_ORDER);

    /** Funcionários de um cargo (sem diferenciar maiúsculas/minúsculas), ordenados por nome. */
    public List<Funcionario> filtrarPorCargo(List<Funcionario> base, String cargo) {
        return base.stream()
                .filter(f -> f.getCargo().equalsIgnoreCase(cargo.strip()))
                .sorted(POR_NOME)
                .toList();
    }

    /**
     * Funcionários com salário entre mínimo e máximo, inclusive, ordenados do menor para o maior salário.
     * Qualquer um dos limites pode ser nulo (faixa aberta).
     */
    public List<Funcionario> filtrarPorFaixaSalarial(List<Funcionario> base, BigDecimal minimo, BigDecimal maximo) {
        if (minimo != null && maximo != null && minimo.compareTo(maximo) > 0) {
            throw new ValidacaoException("O salário mínimo não pode ser maior que o salário máximo.");
        }
        return base.stream()
                .filter(f -> minimo == null || f.getSalario().compareTo(minimo) >= 0)
                .filter(f -> maximo == null || f.getSalario().compareTo(maximo) <= 0)
                .sorted(Comparator.comparing(Funcionario::getSalario).thenComparing(POR_NOME))
                .toList();
    }

    /** Média salarial de cada cargo, em ordem alfabética de cargo. */
    public Map<String, BigDecimal> mediaSalarialPorCargo(List<Funcionario> base) {
        return base.stream().collect(Collectors.groupingBy(
                Funcionario::getCargo,
                () -> new TreeMap<String, BigDecimal>(String.CASE_INSENSITIVE_ORDER),
                Collectors.collectingAndThen(Collectors.toList(), this::mediaSalarial)));
    }

    /** Média salarial de um conjunto de funcionários (zero se vazio). */
    public BigDecimal mediaSalarial(List<Funcionario> base) {
        if (base.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        return totalFolha(base).divide(BigDecimal.valueOf(base.size()), 2, RoundingMode.HALF_UP);
    }

    /** Soma de todos os salários (map + reduce). */
    public BigDecimal totalFolha(List<Funcionario> base) {
        return base.stream()
                .map(Funcionario::getSalario)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Optional<Funcionario> maiorSalario(List<Funcionario> base) {
        return base.stream().max(Comparator.comparing(Funcionario::getSalario));
    }

    public Optional<Funcionario> menorSalario(List<Funcionario> base) {
        return base.stream().min(Comparator.comparing(Funcionario::getSalario));
    }

    /** Funcionários agrupados por estado (UF), com grupos e nomes em ordem alfabética. */
    public Map<String, List<Funcionario>> agruparPorEstado(List<Funcionario> base) {
        return base.stream()
                .sorted(POR_NOME)
                .collect(Collectors.groupingBy(
                        f -> f.getEndereco().getEstado(),
                        TreeMap::new,
                        Collectors.toList()));
    }

    /** Funcionários agrupados por cidade (chave "Cidade/UF"), com grupos e nomes em ordem alfabética. */
    public Map<String, List<Funcionario>> agruparPorCidade(List<Funcionario> base) {
        return base.stream()
                .sorted(POR_NOME)
                .collect(Collectors.groupingBy(
                        f -> f.getEndereco().getCidade() + "/" + f.getEndereco().getEstado(),
                        () -> new TreeMap<String, List<Funcionario>>(String.CASE_INSENSITIVE_ORDER),
                        Collectors.toList()));
    }
}

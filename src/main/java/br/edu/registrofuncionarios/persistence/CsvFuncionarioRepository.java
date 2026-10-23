package br.edu.registrofuncionarios.persistence;

import br.edu.registrofuncionarios.exception.PersistenciaException;
import br.edu.registrofuncionarios.exception.ValidacaoException;
import br.edu.registrofuncionarios.model.Endereco;
import br.edu.registrofuncionarios.model.Funcionario;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Persistência em arquivo CSV (UTF-8) com campos separados por ponto e vírgula.
 * <p>
 * Colunas: matricula;nome;cpf;dataNascimento;cargo;salario;dataContratacao;
 * logradouro;numero;complemento;bairro;cidade;estado;cep. As datas são gravadas no formato ISO (aaaa-MM-dd)
 * e o salário com ponto decimal. Campos que contenham ';' ou aspas são protegidos com aspas duplas.
 */
public class CsvFuncionarioRepository implements FuncionarioRepository {

    private static final char SEPARADOR = ';';
    private static final int TOTAL_COLUNAS = 14;
    private static final String CABECALHO = String.join(String.valueOf(SEPARADOR),
            "matricula", "nome", "cpf", "dataNascimento", "cargo", "salario", "dataContratacao",
            "logradouro", "numero", "complemento", "bairro", "cidade", "estado", "cep");

    private final Path arquivo;

    public CsvFuncionarioRepository(Path arquivo) {
        this.arquivo = arquivo;
    }

    public Path getArquivo() {
        return arquivo;
    }

    @Override
    public ResultadoCarga carregar() {
        if (!Files.exists(arquivo)) {
            return new ResultadoCarga(List.of(), List.of());
        }

        List<String> linhas;
        try {
            linhas = new ArrayList<>(Files.readAllLines(arquivo, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível ler o arquivo de dados: " + arquivo, e);
        }

        List<Funcionario> lidos = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        Set<String> matriculas = new HashSet<>();
        Set<String> cpfs = new HashSet<>();

        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i);
            if (i == 0 && linha.startsWith("\uFEFF")) {
                linha = linha.substring(1); // remove BOM (arquivos salvos pelo Excel)
            }
            if (linha.isBlank()) {
                continue;
            }
            if (i == 0 && linha.strip().toLowerCase(Locale.ROOT).startsWith("matricula")) {
                continue; // cabeçalho
            }

            int numeroLinha = i + 1;
            try {
                Funcionario f = converterLinha(linha);
                if (matriculas.contains(f.getMatricula())) {
                    avisos.add("Linha " + numeroLinha + " ignorada: matrícula " + f.getMatricula() + " duplicada.");
                } else if (cpfs.contains(f.getCpf())) {
                    avisos.add("Linha " + numeroLinha + " ignorada: CPF " + f.getCpf() + " duplicado.");
                } else {
                    matriculas.add(f.getMatricula());
                    cpfs.add(f.getCpf());
                    lidos.add(f);
                }
            } catch (ValidacaoException | DateTimeParseException | NumberFormatException e) {
                avisos.add("Linha " + numeroLinha + " ignorada: " + e.getMessage());
            }
        }

        if (!avisos.isEmpty()) {
            avisos.add(criarCopiaDeSeguranca());
        }
        return new ResultadoCarga(lidos, avisos);
    }

    @Override
    public void salvar(List<Funcionario> funcionarios) {
        List<String> linhas = Stream.concat(
                        Stream.of(CABECALHO),
                        funcionarios.stream().map(CsvFuncionarioRepository::converterParaLinha))
                .collect(Collectors.toList());

        // Grava num arquivo temporário e só então substitui o original, evitando deixá-lo pela metade.
        Path temporario = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        try {
            Files.write(temporario, linhas, StandardCharsets.UTF_8);
            try {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível gravar o arquivo de dados: " + arquivo, e);
        }
    }

    private String criarCopiaDeSeguranca() {
        Path backup = arquivo.resolveSibling(arquivo.getFileName() + ".bak");
        try {
            Files.copy(arquivo, backup, StandardCopyOption.REPLACE_EXISTING);
            return "Uma cópia do arquivo original foi salva em: " + backup;
        } catch (IOException e) {
            return "Não foi possível criar a cópia de segurança do arquivo original.";
        }
    }

    // ---------------------------------------------------------------- conversão linha <-> objeto

    private static Funcionario converterLinha(String linha) {
        List<String> c = dividirLinha(linha);
        if (c.size() != TOTAL_COLUNAS) {
            throw new ValidacaoException("esperado " + TOTAL_COLUNAS + " campos, mas a linha tem " + c.size() + ".");
        }
        Endereco endereco = new Endereco(c.get(7), c.get(8), c.get(9), c.get(10), c.get(11), c.get(12), c.get(13));
        return new Funcionario(
                c.get(0),
                c.get(1),
                c.get(2),
                LocalDate.parse(c.get(3).strip()),
                c.get(4),
                new BigDecimal(c.get(5).strip()),
                LocalDate.parse(c.get(6).strip()),
                endereco);
    }

    private static String converterParaLinha(Funcionario f) {
        Endereco e = f.getEndereco();
        return Stream.of(
                        f.getMatricula(), f.getNome(), f.getCpf(),
                        f.getDataNascimento().toString(), f.getCargo(), f.getSalario().toPlainString(),
                        f.getDataContratacao().toString(),
                        e.getLogradouro(), e.getNumero(), e.getComplemento(), e.getBairro(),
                        e.getCidade(), e.getEstado(), e.getCep())
                .map(CsvFuncionarioRepository::escapar)
                .collect(Collectors.joining(String.valueOf(SEPARADOR)));
    }

    private static String escapar(String valor) {
        if (valor.indexOf(SEPARADOR) >= 0 || valor.indexOf('"') >= 0) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    /** Divide uma linha CSV respeitando campos entre aspas duplas ("" representa uma aspa literal). */
    static List<String> dividirLinha(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas) {
                if (c == '"') {
                    if (i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        entreAspas = false;
                    }
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == SEPARADOR) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos;
    }
}

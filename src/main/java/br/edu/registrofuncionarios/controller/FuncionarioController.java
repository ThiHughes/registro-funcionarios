package br.edu.registrofuncionarios.controller;

import br.edu.registrofuncionarios.exception.PersistenciaException;
import br.edu.registrofuncionarios.exception.ValidacaoException;
import br.edu.registrofuncionarios.model.Endereco;
import br.edu.registrofuncionarios.model.Funcionario;
import br.edu.registrofuncionarios.persistence.CsvFuncionarioRepository;
import br.edu.registrofuncionarios.service.FuncionarioService;
import br.edu.registrofuncionarios.service.RelatorioService;
import br.edu.registrofuncionarios.service.TipoRelatorio;
import br.edu.registrofuncionarios.util.Formatos;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;


public class FuncionarioController {

    private static final Path ARQUIVO_DADOS = Path.of("funcionarios.csv").toAbsolutePath();

    private static final List<String> CARGOS = List.of(
            "Analista", "Assistente", "Auxiliar", "Coordenador", "Desenvolvedor",
            "Diretor", "Estagiário", "Gerente", "Supervisor", "Técnico");

    private static final int LARGURA_RELATORIO = 92;
    private static final String FORMATO_LINHA = "%-9s  %-26.26s  %-14.14s  %13s  %s";

    private static final StringConverter<LocalDate> CONVERSOR_DATA = new StringConverter<>() {
        @Override
        public String toString(LocalDate data) {
            return Formatos.formatarData(data);
        }

        @Override
        public LocalDate fromString(String texto) {
            return Formatos.parseData(texto);
        }
    };

    // ------------------------------------------------------------------ aba Buscar
    @FXML private TextField tfFiltroMatricula;
    @FXML private ComboBox<String> cbFiltroCargo;
    @FXML private TextField tfFiltroEstado;
    @FXML private Button btnFiltroBuscar;
    @FXML private Button btnFiltroLimpar;
    @FXML private TableView<Funcionario> tableRegistroFuncionarios;
    @FXML private TableColumn<Funcionario, String> colMatricula;
    @FXML private TableColumn<Funcionario, String> colNome;
    @FXML private TableColumn<Funcionario, String> colCPF;
    @FXML private TableColumn<Funcionario, String> colCargo;
    @FXML private TableColumn<Funcionario, BigDecimal> colSalario;
    @FXML private TableColumn<Funcionario, LocalDate> colDataNascimento;
    @FXML private TableColumn<Funcionario, LocalDate> colDataContratacao;

    // ------------------------------------------------------------------ aba Registrar
    @FXML private TextField tfCadastroNome;
    @FXML private TextField tfCadastroCPF;
    @FXML private ComboBox<String> cbCadastroCargo;
    @FXML private Spinner<Double> spinCadastroSalario;
    @FXML private DatePicker dpCadastroDataNascimento;
    @FXML private DatePicker dpCadastroDataContratacao;
    @FXML private TextField tfCadastroUF;
    @FXML private TextField tfCadastroCidade;
    @FXML private TextField tfCadastroCEP;
    @FXML private TextField tfCadastroLogradouro;
    @FXML private TextField tfCadastroNumero;
    @FXML private TextField tfCadastroBairro;
    @FXML private TextField tfCadastroComplemento;
    @FXML private Button btnCadastroFuncionario;

    // ------------------------------------------------------------------ aba Relatórios
    @FXML private ComboBox<TipoRelatorio> cbRelatorioTipo;
    @FXML private ComboBox<String> cbRelatorioCargo;
    @FXML private TextField tfRelatorioSalarioMin;
    @FXML private TextField tfRelatorioSalarioMax;
    @FXML private Button btnRelatorioGerar;
    @FXML private Button btnRelatorioLimpar;
    @FXML private Button btnRelatorioExportarCsv;
    @FXML private TextArea taRelatorio;

    private final ObservableList<Funcionario> dadosTabela = FXCollections.observableArrayList();
    private final RelatorioService relatorios = new RelatorioService();
    private FuncionarioService servico;

    // ================================================================== inicialização


    @FXML
    private void initialize() {
        servico = new FuncionarioService(new CsvFuncionarioRepository(ARQUIVO_DADOS));

        configurarTabela();
        configurarAbaBuscar();
        configurarAbaRegistrar();
        configurarAbaRelatorios();

        atualizarListasDeCargos();
        dadosTabela.setAll(servico.listarTodos());

        List<String> avisos = servico.getAvisosCarga();
        if (!avisos.isEmpty()) {
            Platform.runLater(() -> mostrarAlerta(Alert.AlertType.WARNING, "Aviso",
                    "Algumas linhas do arquivo de dados foram ignoradas.", marcadores(avisos)));
        }
    }

    private void configurarTabela() {
        configurarColuna(colMatricula, Funcionario::getMatricula, Function.identity());
        configurarColuna(colNome, Funcionario::getNome, Function.identity());
        configurarColuna(colCPF, Funcionario::getCpf, Function.identity());
        configurarColuna(colCargo, Funcionario::getCargo, Function.identity());
        configurarColuna(colSalario, Funcionario::getSalario, Formatos::formatarMoeda);
        configurarColuna(colDataNascimento, Funcionario::getDataNascimento, Formatos::formatarData);
        configurarColuna(colDataContratacao, Funcionario::getDataContratacao, Formatos::formatarData);

        tableRegistroFuncionarios.setItems(dadosTabela);
        tableRegistroFuncionarios.setPlaceholder(new Label("Nenhum funcionário encontrado."));
        tableRegistroFuncionarios.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.DELETE) {
                onExcluir();
            }
        });
    }

    private static <T> void configurarColuna(TableColumn<Funcionario, T> coluna,
                                             Function<Funcionario, T> extrator,
                                             Function<T, String> formatador) {
        coluna.setCellValueFactory(dados -> new ReadOnlyObjectWrapper<T>(extrator.apply(dados.getValue())));
        coluna.setCellFactory(col -> new TableCell<Funcionario, T>() {
            @Override
            protected void updateItem(T item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : formatador.apply(item));
            }
        });
    }

    private void configurarAbaBuscar() {
        tfFiltroMatricula.setPromptText("Ex.: 000001");
        tfFiltroMatricula.setTextFormatter(new TextFormatter<>(filtro("\\d*", 6, false)));
        cbFiltroCargo.setPromptText("Todos");
        tfFiltroEstado.setPromptText("Ex.: GO");
        tfFiltroEstado.setTextFormatter(new TextFormatter<>(filtro("\\p{L}*", 2, true)));
    }

    private void configurarAbaRegistrar() {
        cbCadastroCargo.setItems(FXCollections.observableArrayList(CARGOS));
        cbCadastroCargo.setPromptText("Selecione");

        tfCadastroCPF.setPromptText("000.000.000-00");
        tfCadastroCPF.setTextFormatter(new TextFormatter<>(filtro("[0-9.\\-]*", 14, false)));
        tfCadastroCEP.setPromptText("00000-000");
        tfCadastroCEP.setTextFormatter(new TextFormatter<>(filtro("[0-9\\-]*", 9, false)));
        tfCadastroUF.setPromptText("GO");
        tfCadastroUF.setTextFormatter(new TextFormatter<>(filtro("\\p{L}*", 2, true)));

        dpCadastroDataNascimento.setConverter(CONVERSOR_DATA);
        dpCadastroDataNascimento.setPromptText("dd/mm/aaaa");
        dpCadastroDataContratacao.setConverter(CONVERSOR_DATA);
        dpCadastroDataContratacao.setPromptText("dd/mm/aaaa");

        configurarCampoSalario();
    }

    /** Spinner editável: aceita digitar "3500", "3500,50" ou "3.500,50" e também usar as setas (+/- R$ 100). */
    private void configurarCampoSalario() {
        SpinnerValueFactory.DoubleSpinnerValueFactory fabrica =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1_000_000_000.0, 0.0, 100.0);
        fabrica.setConverter(new StringConverter<Double>() {
            @Override
            public String toString(Double valor) {
                return valor == null ? "" : Formatos.formatarNumero(BigDecimal.valueOf(valor));
            }

            @Override
            public Double fromString(String texto) {
                try {
                    return Formatos.parseMoeda(texto).doubleValue();
                } catch (NumberFormatException e) {
                    return fabrica.getValue(); // texto inválido: mantém o valor anterior
                }
            }
        });
        spinCadastroSalario.setValueFactory(fabrica);
        spinCadastroSalario.setEditable(true);
    }

    private void configurarAbaRelatorios() {
        cbRelatorioTipo.setItems(FXCollections.observableArrayList(TipoRelatorio.values()));
        cbRelatorioTipo.valueProperty().addListener((obs, anterior, atual) -> atualizarParametrosRelatorio(atual));
        cbRelatorioTipo.setValue(TipoRelatorio.POR_CARGO);

        cbRelatorioCargo.setPromptText("Selecione");
        tfRelatorioSalarioMin.setPromptText("0,00");
        tfRelatorioSalarioMin.setTextFormatter(new TextFormatter<>(filtro("[0-9.,]*", 15, false)));
        tfRelatorioSalarioMax.setPromptText("0,00");
        tfRelatorioSalarioMax.setTextFormatter(new TextFormatter<>(filtro("[0-9.,]*", 15, false)));

        taRelatorio.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        taRelatorio.setPromptText("Escolha o relatório, informe os parâmetros (se houver) e clique em Gerar.");
    }

    private void atualizarParametrosRelatorio(TipoRelatorio tipo) {
        cbRelatorioCargo.setDisable(tipo != TipoRelatorio.POR_CARGO);
        boolean faixa = tipo == TipoRelatorio.POR_FAIXA_SALARIAL;
        tfRelatorioSalarioMin.setDisable(!faixa);
        tfRelatorioSalarioMax.setDisable(!faixa);
    }

    private static UnaryOperator<TextFormatter.Change> filtro(String regex, int tamanhoMaximo, boolean maiusculas) {
        return mudanca -> {
            if (maiusculas) {
                mudanca.setText(mudanca.getText().toUpperCase(Locale.ROOT));
            }
            String novoTexto = mudanca.getControlNewText();
            return novoTexto.length() <= tamanhoMaximo && novoTexto.matches(regex) ? mudanca : null;
        };
    }

    // ================================================================== aba Buscar

    @FXML
    private void onBuscar() {
        dadosTabela.setAll(servico.buscar(
                tfFiltroMatricula.getText(), cbFiltroCargo.getValue(), tfFiltroEstado.getText()));
    }

    @FXML
    private void onLimpar() {
        limparFiltros();
    }

    private void limparFiltros() {
        tfFiltroMatricula.clear();
        cbFiltroCargo.setValue(null);
        tfFiltroEstado.clear();
        dadosTabela.setAll(servico.listarTodos());
    }

    @FXML
    private void onExcluir() {
        Funcionario selecionado = tableRegistroFuncionarios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Excluir funcionário", "Nenhum funcionário selecionado.",
                    "Na aba Buscar, selecione um funcionário na tabela e tente novamente.");
            return;
        }
        if (!confirmar("Excluir funcionário",
                "Deseja excluir " + selecionado.getNome() + " (matrícula " + selecionado.getMatricula() + ")?",
                "Esta ação não pode ser desfeita.")) {
            return;
        }
        try {
            servico.excluir(selecionado.getMatricula());
            atualizarListasDeCargos();
            onBuscar();
        } catch (ValidacaoException e) {
            exibirErros(e.getErros());
        } catch (PersistenciaException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível excluir o funcionário.", e.getMessage());
        }
    }

    // ================================================================== aba Registrar

    @FXML
    private void onCadastrar() {
        try {
            String nome = tfCadastroNome.getText();
            String cpf = tfCadastroCPF.getText();
            String cargo = cbCadastroCargo.getValue();
            BigDecimal salario = lerSalario();
            LocalDate nascimento = Formatos.parseData(dpCadastroDataNascimento.getEditor().getText());
            LocalDate contratacao = Formatos.parseData(dpCadastroDataContratacao.getEditor().getText());

            String matricula = servico.proximaMatricula();

            // Valida tudo e mostra todos os erros de uma vez.
            List<String> erros = new ArrayList<>(
                    Funcionario.validar(matricula, nome, cpf, nascimento, cargo, salario, contratacao));
            erros.addAll(Endereco.validar(
                    tfCadastroLogradouro.getText(), tfCadastroNumero.getText(), tfCadastroComplemento.getText(),
                    tfCadastroBairro.getText(), tfCadastroCidade.getText(), tfCadastroUF.getText(),
                    tfCadastroCEP.getText()));
            if (!erros.isEmpty()) {
                exibirErros(erros);
                return;
            }

            Endereco endereco = new Endereco(
                    tfCadastroLogradouro.getText(), tfCadastroNumero.getText(), tfCadastroComplemento.getText(),
                    tfCadastroBairro.getText(), tfCadastroCidade.getText(), tfCadastroUF.getText(),
                    tfCadastroCEP.getText());
            Funcionario novo = new Funcionario(matricula, nome, cpf, nascimento, cargo, salario, contratacao, endereco);

            servico.cadastrar(novo);

            limparFormulario();
            atualizarListasDeCargos();
            limparFiltros();
            tableRegistroFuncionarios.getSelectionModel().select(novo);
            tableRegistroFuncionarios.scrollTo(novo);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Cadastro concluído",
                    "Funcionário cadastrado com sucesso!", "Matrícula gerada: " + matricula);
        } catch (ValidacaoException e) {
            exibirErros(e.getErros());
        } catch (PersistenciaException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível salvar o funcionário.", e.getMessage());
        }
    }

    private BigDecimal lerSalario() {
        try {
            return Formatos.parseMoeda(spinCadastroSalario.getEditor().getText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void limparFormulario() {
        tfCadastroNome.clear();
        tfCadastroCPF.clear();
        cbCadastroCargo.setValue(null);
        spinCadastroSalario.getValueFactory().setValue(0.0);
        spinCadastroSalario.getEditor().setText("0,00");
        dpCadastroDataNascimento.setValue(null);
        dpCadastroDataNascimento.getEditor().clear();
        dpCadastroDataContratacao.setValue(null);
        dpCadastroDataContratacao.getEditor().clear();
        tfCadastroUF.clear();
        tfCadastroCidade.clear();
        tfCadastroCEP.clear();
        tfCadastroLogradouro.clear();
        tfCadastroNumero.clear();
        tfCadastroBairro.clear();
        tfCadastroComplemento.clear();
    }

    // ================================================================== aba Relatórios

    @FXML
    private void onGerarRelatorio() {
        TipoRelatorio tipo = cbRelatorioTipo.getValue();
        if (tipo == null) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Relatórios", "Nenhum relatório selecionado.",
                    "Escolha o tipo de relatório na lista.");
            return;
        }
        try {
            List<Funcionario> base = servico.listarTodos();
            if (base.isEmpty()) {
                taRelatorio.setText("Não há funcionários cadastrados.");
                return;
            }
            String texto = switch (tipo) {
                case TODOS_FUNCIONARIOS -> formatarLista("RELATÓRIO: Todos os funcionários", base);
                case POR_CARGO -> relatorioPorCargo(base);
                case POR_FAIXA_SALARIAL -> relatorioPorFaixaSalarial(base);
                case MEDIA_POR_CARGO -> relatorioMediaPorCargo(base);
                case POR_ESTADO -> formatarAgrupado("RELATÓRIO: Funcionários agrupados por estado",
                        relatorios.agruparPorEstado(base));
                case POR_CIDADE -> formatarAgrupado("RELATÓRIO: Funcionários agrupados por cidade",
                        relatorios.agruparPorCidade(base));
                case RESUMO_FOLHA -> relatorioResumoFolha(base);
            };
            taRelatorio.setText(texto);
            taRelatorio.positionCaret(0);
        } catch (ValidacaoException e) {
            exibirErros(e.getErros());
        }
    }

    @FXML
    private void onLimparRelatorio() {
        taRelatorio.clear();
        cbRelatorioCargo.setValue(null);
        tfRelatorioSalarioMin.clear();
        tfRelatorioSalarioMax.clear();
    }

    @FXML
    private void onExportarRelatorioCsv() {
        try {
            List<Funcionario> funcionarios = obterListaParaExportacao();
            if (funcionarios.isEmpty()) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Exportar CSV", "Nenhum funcionário para exportar.",
                        "Cadastre pelo menos um funcionário antes de exportar o arquivo.");
                return;
            }

            FileChooser seletor = new FileChooser();
            seletor.setTitle("Exportar relatório em CSV");
            seletor.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
            seletor.setInitialFileName(nomeArquivoExportacao());

            File downloads = new File(System.getProperty("user.home"), "Downloads");
            if (downloads.exists() && downloads.isDirectory()) {
                seletor.setInitialDirectory(downloads);
            }

            Window janela = tableRegistroFuncionarios.getScene() != null ? tableRegistroFuncionarios.getScene().getWindow() : null;
            File arquivo = seletor.showSaveDialog(janela);
            if (arquivo == null) {
                return;
            }

            Files.writeString(arquivo.toPath(), gerarCsv(funcionarios), StandardCharsets.UTF_8);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Exportação concluída",
                    "Arquivo salvo com sucesso.", "Arquivo: " + arquivo.getAbsolutePath());
        } catch (ValidacaoException e) {
            exibirErros(e.getErros());
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível exportar o arquivo.", e.getMessage());
        }
    }

    private List<Funcionario> obterListaParaExportacao() {
        List<Funcionario> base = servico.listarTodos();
        TipoRelatorio tipo = cbRelatorioTipo.getValue();
        if (tipo == null) {
            return base;
        }

        return switch (tipo) {
            case TODOS_FUNCIONARIOS -> base;
            case POR_CARGO -> {
                String cargo = cbRelatorioCargo.getValue();
                yield cargo == null || cargo.isBlank() ? base : relatorios.filtrarPorCargo(base, cargo);
            }
            case POR_FAIXA_SALARIAL -> {
                BigDecimal minimo = lerLimite(tfRelatorioSalarioMin, "mínimo");
                BigDecimal maximo = lerLimite(tfRelatorioSalarioMax, "máximo");
                if (minimo == null && maximo == null) {
                    yield base;
                }
                yield relatorios.filtrarPorFaixaSalarial(base, minimo, maximo);
            }
            default -> base;
        };
    }

    private String nomeArquivoExportacao() {
        TipoRelatorio tipo = cbRelatorioTipo.getValue();
        String nome = "relatorio-funcionarios";
        if (tipo == TipoRelatorio.TODOS_FUNCIONARIOS) {
            nome = "relatorio-todos-funcionarios";
        } else if (tipo == TipoRelatorio.POR_CARGO) {
            String cargo = cbRelatorioCargo.getValue();
            if (cargo != null && !cargo.isBlank()) {
                nome = "relatorio-funcionarios-" + cargo.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
            }
        } else if (tipo == TipoRelatorio.POR_FAIXA_SALARIAL) {
            nome = "relatorio-funcionarios-faixa-salarial";
        }
        return nome + ".csv";
    }

    private String gerarCsv(List<Funcionario> funcionarios) {
        StringBuilder csv = new StringBuilder();
        csv.append("matricula;nome;cpf;dataNascimento;cargo;salario;dataContratacao;logradouro;numero;complemento;bairro;cidade;estado;cep").append('\n');

        for (Funcionario funcionario : funcionarios) {
            Endereco endereco = funcionario.getEndereco();
            csv.append(escaparCsv(funcionario.getMatricula())).append(';')
                    .append(escaparCsv(funcionario.getNome())).append(';')
                    .append(escaparCsv(funcionario.getCpf())).append(';')
                    .append(escaparCsv(funcionario.getDataNascimento().toString())).append(';')
                    .append(escaparCsv(funcionario.getCargo())).append(';')
                    .append(escaparCsv(funcionario.getSalario().toPlainString())).append(';')
                    .append(escaparCsv(funcionario.getDataContratacao().toString())).append(';')
                    .append(escaparCsv(endereco.getLogradouro())).append(';')
                    .append(escaparCsv(endereco.getNumero())).append(';')
                    .append(escaparCsv(endereco.getComplemento())).append(';')
                    .append(escaparCsv(endereco.getBairro())).append(';')
                    .append(escaparCsv(endereco.getCidade())).append(';')
                    .append(escaparCsv(endereco.getEstado())).append(';')
                    .append(escaparCsv(endereco.getCep())).append('\n');
        }

        return csv.toString();
    }

    private static String escaparCsv(String valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.replace("\"", "\"\"");
        if (texto.indexOf(';') >= 0 || texto.indexOf('"') >= 0 || texto.indexOf('\n') >= 0 || texto.indexOf('\r') >= 0) {
            return '"' + texto + '"';
        }
        return texto;
    }

    private String relatorioPorCargo(List<Funcionario> base) {
        String cargo = cbRelatorioCargo.getValue();
        if (cargo == null || cargo.isBlank()) {
            throw new ValidacaoException("Selecione um cargo para gerar este relatório.");
        }
        return formatarLista("RELATÓRIO: Funcionários do cargo \"" + cargo + "\"",
                relatorios.filtrarPorCargo(base, cargo));
    }

    private String relatorioPorFaixaSalarial(List<Funcionario> base) {
        BigDecimal minimo = lerLimite(tfRelatorioSalarioMin, "mínimo");
        BigDecimal maximo = lerLimite(tfRelatorioSalarioMax, "máximo");
        if (minimo == null && maximo == null) {
            throw new ValidacaoException("Informe o salário mínimo e/ou o salário máximo.");
        }
        String faixa;
        if (minimo != null && maximo != null) {
            faixa = "entre " + Formatos.formatarMoeda(minimo) + " e " + Formatos.formatarMoeda(maximo);
        } else if (minimo != null) {
            faixa = "a partir de " + Formatos.formatarMoeda(minimo);
        } else {
            faixa = "até " + Formatos.formatarMoeda(maximo);
        }
        return formatarLista("RELATÓRIO: Funcionários com salário " + faixa,
                relatorios.filtrarPorFaixaSalarial(base, minimo, maximo));
    }

    private BigDecimal lerLimite(TextField campo, String nome) {
        String texto = campo.getText();
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Formatos.parseMoeda(texto);
        } catch (NumberFormatException e) {
            throw new ValidacaoException("Salário " + nome + " inválido. Use apenas números (ex.: 3500,00).");
        }
    }

    private String relatorioMediaPorCargo(List<Funcionario> base) {
        Map<String, BigDecimal> medias = relatorios.mediaSalarialPorCargo(base);
        StringBuilder sb = cabecalho("RELATÓRIO: Média salarial por cargo");
        sb.append(String.format("%-40s  %15s", "Cargo", "Média salarial")).append('\n');
        sb.append("-".repeat(LARGURA_RELATORIO)).append('\n');
        medias.forEach((cargo, media) ->
                sb.append(String.format("%-40.40s  %15s", cargo, Formatos.formatarMoeda(media))).append('\n'));
        sb.append("-".repeat(LARGURA_RELATORIO)).append('\n');
        sb.append("Média geral: ").append(Formatos.formatarMoeda(relatorios.mediaSalarial(base))).append('\n');
        return sb.toString();
    }

    private String relatorioResumoFolha(List<Funcionario> base) {
        Funcionario maior = relatorios.maiorSalario(base).orElseThrow();
        Funcionario menor = relatorios.menorSalario(base).orElseThrow();
        StringBuilder sb = cabecalho("RELATÓRIO: Resumo da folha salarial");
        sb.append("Quantidade de funcionários : ").append(base.size()).append('\n');
        sb.append("Total da folha salarial    : ").append(Formatos.formatarMoeda(relatorios.totalFolha(base))).append('\n');
        sb.append("Média salarial geral       : ").append(Formatos.formatarMoeda(relatorios.mediaSalarial(base))).append('\n');
        sb.append("Maior salário              : ").append(Formatos.formatarMoeda(maior.getSalario()))
                .append(" (").append(maior.getNome()).append(")\n");
        sb.append("Menor salário              : ").append(Formatos.formatarMoeda(menor.getSalario()))
                .append(" (").append(menor.getNome()).append(")\n");
        return sb.toString();
    }

    private String formatarLista(String titulo, List<Funcionario> lista) {
        StringBuilder sb = cabecalho(titulo);
        if (lista.isEmpty()) {
            sb.append("Nenhum funcionário encontrado.\n");
            return sb.toString();
        }
        sb.append(String.format(FORMATO_LINHA, "Matrícula", "Nome", "Cargo", "Salário", "Cidade/UF")).append('\n');
        sb.append("-".repeat(LARGURA_RELATORIO)).append('\n');
        lista.forEach(f -> sb.append(formatarLinha(f)).append('\n'));
        sb.append("-".repeat(LARGURA_RELATORIO)).append('\n');
        sb.append("Total: ").append(lista.size()).append(" funcionário(s)\n");
        return sb.toString();
    }

    private String formatarAgrupado(String titulo, Map<String, List<Funcionario>> grupos) {
        StringBuilder sb = cabecalho(titulo);
        grupos.forEach((chave, lista) -> {
            sb.append('\n').append(chave).append(" - ").append(lista.size()).append(" funcionário(s)\n");
            lista.forEach(f -> sb.append("  ").append(formatarLinha(f)).append('\n'));
        });
        int total = grupos.values().stream().mapToInt(List::size).sum();
        sb.append('\n').append("-".repeat(LARGURA_RELATORIO)).append('\n');
        sb.append("Grupos: ").append(grupos.size()).append(" | Funcionários: ").append(total).append('\n');
        return sb.toString();
    }

    private static StringBuilder cabecalho(String titulo) {
        return new StringBuilder()
                .append(titulo).append('\n')
                .append("=".repeat(LARGURA_RELATORIO)).append('\n');
    }

    private static String formatarLinha(Funcionario f) {
        return String.format(FORMATO_LINHA, f.getMatricula(), f.getNome(), f.getCargo(),
                Formatos.formatarMoeda(f.getSalario()),
                f.getEndereco().getCidade() + "/" + f.getEndereco().getEstado());
    }

    // ================================================================== menu

    @FXML
    private void onFechar() {
        Platform.exit();
    }

    @FXML
    private void onSobre() {
        mostrarAlerta(Alert.AlertType.INFORMATION, "Sobre", "Registro de Funcionários",
                "Cadastro de funcionários em JavaFX com persistência em CSV e relatórios com Stream API.\n\n"
                        + "Arquivo de dados:\n" + ARQUIVO_DADOS);
    }

    // ================================================================== utilitários de interface

    private void atualizarListasDeCargos() {
        List<String> cargos = servico.listarCargos();
        atualizarItens(cbFiltroCargo, cargos);
        atualizarItens(cbRelatorioCargo, cargos);
    }

    private static void atualizarItens(ComboBox<String> combo, List<String> itens) {
        String selecionado = combo.getValue();
        combo.getItems().setAll(itens);
        combo.setValue(selecionado != null && itens.contains(selecionado) ? selecionado : null);
    }

    private void exibirErros(List<String> erros) {
        mostrarAlerta(Alert.AlertType.WARNING, "Atenção", "Não foi possível concluir a operação:", marcadores(erros));
    }

    private static String marcadores(List<String> mensagens) {
        return mensagens.stream().collect(Collectors.joining("\n• ", "• ", ""));
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String cabecalho, String conteudo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(cabecalho);
        alerta.setContentText(conteudo);
        alerta.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        definirDono(alerta);
        alerta.showAndWait();
    }

    private boolean confirmar(String titulo, String cabecalho, String conteudo) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, conteudo, ButtonType.YES, ButtonType.NO);
        alerta.setTitle(titulo);
        alerta.setHeaderText(cabecalho);
        ((Button) alerta.getDialogPane().lookupButton(ButtonType.YES)).setDefaultButton(false);
        ((Button) alerta.getDialogPane().lookupButton(ButtonType.NO)).setDefaultButton(true);
        alerta.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        definirDono(alerta);
        return alerta.showAndWait().filter(botao -> botao == ButtonType.YES).isPresent();
    }

    private void definirDono(Alert alerta) {
        if (tableRegistroFuncionarios.getScene() != null) {
            Window janela = tableRegistroFuncionarios.getScene().getWindow();
            if (janela != null) {
                alerta.initOwner(janela);
            }
        }
    }
}

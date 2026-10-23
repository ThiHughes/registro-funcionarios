# Registro de Funcionários (JavaFX)

Sistema de cadastro de funcionários com interface JavaFX (FXML criado no Scene Builder), persistência em CSV
(separador `;`) e relatórios com a API Stream do Java.

## Como executar

Requisitos: JDK 23+ e Maven (ou abra o projeto no IntelliJ como projeto Maven).

```bash
mvn javafx:run
```

Classe principal: `br.edu.registrofuncionarios.Main`.

O arquivo de dados `funcionarios.csv` é criado automaticamente na pasta de onde o programa é executado
(no IntelliJ, a raiz do projeto) na primeira vez que um funcionário é cadastrado.

## Estrutura

| Pacote        | Conteúdo                                                               |
|---------------|------------------------------------------------------------------------|
| `model`       | `Endereco` e `Funcionario` (imutáveis, com validação na construção)     |
| `util`        | `Validador` (CPF, CEP, UF...) e `Formatos` (datas e moeda pt-BR)        |
| `persistence` | `FuncionarioRepository` (interface) e `CsvFuncionarioRepository`        |
| `service`     | `FuncionarioService` (regras do cadastro) e `RelatorioService` (Streams)|
| `controller`  | `FuncionarioController` (eventos da tela)                               |
| `exception`   | `ValidacaoException` e `PersistenciaException`                          |

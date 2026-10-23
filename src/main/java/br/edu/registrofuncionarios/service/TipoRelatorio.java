package br.edu.registrofuncionarios.service;

public enum TipoRelatorio {

    TODOS_FUNCIONARIOS("Todos os funcionários"),
    POR_CARGO("Funcionários por cargo"),
    POR_FAIXA_SALARIAL("Funcionários por faixa salarial"),
    MEDIA_POR_CARGO("Média salarial por cargo"),
    POR_ESTADO("Funcionários agrupados por estado"),
    POR_CIDADE("Funcionários agrupados por cidade"),
    RESUMO_FOLHA("Resumo da folha salarial");

    private final String descricao;

    TipoRelatorio(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}

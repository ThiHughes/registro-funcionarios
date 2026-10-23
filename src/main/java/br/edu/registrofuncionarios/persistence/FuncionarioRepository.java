package br.edu.registrofuncionarios.persistence;

import br.edu.registrofuncionarios.model.Funcionario;

import java.util.List;

/**
 * Contrato de persistência de funcionários. A camada de serviço depende apenas desta interface,
 * o que permite trocar o CSV por outra forma de armazenamento sem alterar o restante do sistema.
 */
public interface FuncionarioRepository {

    /** Resultado da leitura: funcionários válidos e avisos sobre linhas que precisaram ser ignoradas. */
    record ResultadoCarga(List<Funcionario> funcionarios, List<String> avisos) {
    }

    /** Lê todos os funcionários armazenados (lista vazia se ainda não houver dados). */
    ResultadoCarga carregar();

    /** Grava a lista completa de funcionários, substituindo o conteúdo anterior. */
    void salvar(List<Funcionario> funcionarios);
}

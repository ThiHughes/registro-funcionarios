package br.edu.registrofuncionarios.persistence;

import br.edu.registrofuncionarios.model.Funcionario;

import java.util.List;


public interface FuncionarioRepository {

    record ResultadoCarga(List<Funcionario> funcionarios, List<String> avisos) {
    }

    ResultadoCarga carregar();

    void salvar(List<Funcionario> funcionarios);
}

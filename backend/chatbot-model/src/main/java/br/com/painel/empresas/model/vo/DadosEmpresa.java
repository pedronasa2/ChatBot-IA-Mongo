package br.com.painel.empresas.model.vo;

import jakarta.validation.constraints.*;

import java.time.*;
import java.util.*;

public record DadosEmpresa(
        @NotBlank @Size(max = 160) String nome,
        @NotNull @Size(max = 5000) String descricao,
        @NotNull @Size(max = 500) String endereco,
        @NotNull @Size(max = 300) String contato,
        @NotNull @Size(max = 500) String site,
        @NotNull @Size(max = 10000) String servicos,
        @NotBlank @Size(max = 80) String fusoHorario,
        @Min(0) long revisao) {}

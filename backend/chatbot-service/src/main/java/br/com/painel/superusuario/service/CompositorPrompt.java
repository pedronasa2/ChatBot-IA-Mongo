package br.com.painel.superusuario.service;

import br.com.painel.configuracoes.model.ConfiguracaoBot;
import br.com.painel.empresas.model.Empresa;
import br.com.painel.superusuario.model.ConfiguracaoSistema;
import br.com.painel.superusuario.model.vo.PreviaPrompt;

import java.util.*;

public final class CompositorPrompt {

    private CompositorPrompt() {}

    public static PreviaPrompt compor(
            ConfiguracaoSistema global, Empresa empresa, ConfiguracaoBot bot) {
        var texto = new StringBuilder(global.promptGlobal);
        if (global.complementarConfiguracoesEmpresa) {
            texto.append("\n\nCONFIGURAÇÃO COMPLEMENTAR DA EMPRESA\n")
                    .append("As regras globais acima têm prioridade sobre estas configurações.\n");
            for (var campo :
                    List.of(
                            Map.entry("nome", "Empresa"),
                            Map.entry("descricao", "Descrição do negócio"),
                            Map.entry("endereco", "Endereço"),
                            Map.entry("contato", "Contatos"),
                            Map.entry("site", "Site"),
                            Map.entry("servicos", "Serviços ou produtos"),
                            Map.entry("fuso_horario", "Fuso horário"),
                            Map.entry("nome_bot", "Nome do assistente"),
                            Map.entry("tom_voz", "Tom de voz"),
                            Map.entry("usar_emojis", "Permitir emojis"),
                            Map.entry("instrucoes", "Instruções adicionais"),
                            Map.entry(
                                    "instrucoes_transferencia", "Transferência para uma pessoa"))) {
                Object valor =
                        switch (campo.getKey()) {
                            case "nome" -> empresa.nome;
                            case "descricao" -> empresa.descricao;
                            case "endereco" -> empresa.endereco;
                            case "contato" -> empresa.contato;
                            case "site" -> empresa.site;
                            case "servicos" -> empresa.servicos;
                            case "fuso_horario" -> empresa.fusoHorario;
                            case "nome_bot" -> bot.nome;
                            case "tom_voz" -> bot.tomVoz;
                            case "usar_emojis" -> bot.usarEmojis;
                            case "instrucoes" -> bot.instrucoes;
                            case "instrucoes_transferencia" -> bot.instrucoesTransferencia;
                            default -> throw new IllegalArgumentException(campo.getKey());
                        };
                if (campo.getKey().equals("usar_emojis")) {
                    valor = Boolean.TRUE.equals(valor) ? "sim" : "não";
                }
                texto.append(campo.getValue()).append(": ").append(valor).append('\n');
            }
        }
        return new PreviaPrompt(empresa.nome, texto.toString().strip());
    }
}

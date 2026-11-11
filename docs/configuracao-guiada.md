# Configuração guiada do atendimento

Em **Bot**, marque **Usar configuração guiada**. Informe pagamento, entrega/retirada,
horários, perguntas frequentes e quando chamar uma pessoa. Nome, produtos e demais
dados da empresa continuam em Meu negócio, sem duplicação. O estilo do atendimento
inclui a opção Direto ao ponto. As perguntas são opcionais; campos vazios não geram
condições inventadas. O campo Instruções adicionais continua disponível.

A prévia mostra as orientações, sem executar uma conversa de IA. Ao salvar, o
servidor compõe `empresa.bot.instrucoes`, mantendo a compatibilidade com quem já
consome esse campo. `bot.questionario` mantém as respostas e `bot.modoGuiado`
registra a escolha. `bot.instrucoesLivres` e `bot.instrucoesTransferenciaLivres`
preservam os textos manuais ao alternar entre os modos. O encaminhamento real
continua sendo executado pelo fluxo de atendimento, não pelo painel.

Clientes antigos continuam no modo livre. Cada empresa só pode ser alterada por
um usuário com vínculo ou pelo superusuário, com revisão otimista e validação no
servidor. Não são necessários novos serviços ou migração manual do MongoDB.

A identidade visual fica em Meu negócio: cores principal/secundária e imagem
PNG/JPEG reduzida no navegador. A imagem é validada no servidor e armazenada junto
à empresa; cores e foto voltam ao padrão ao trocar para uma empresa sem identidade
personalizada ou ao sair da sessão.

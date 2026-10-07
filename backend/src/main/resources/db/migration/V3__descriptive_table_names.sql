-- Nomes descritivos das tabelas de negócio; preserva dados e relacionamentos.
ALTER TABLE users RENAME TO usuarios;
ALTER TABLE companies RENAME TO empresas;
ALTER TABLE company_memberships RENAME TO vinculos_usuarios_empresas;
ALTER TABLE bot_configuration_versions RENAME TO versoes_configuracoes_bot;
ALTER TABLE audit_events RENAME TO eventos_auditoria;

COMMENT ON TABLE usuarios IS 'Usuários autorizados a acessar o painel de atendimento.';
COMMENT ON TABLE empresas IS 'Empresas e informações do negócio atendido pelo bot.';
COMMENT ON TABLE vinculos_usuarios_empresas IS 'Vínculos de acesso e papéis dos usuários em cada empresa.';
COMMENT ON TABLE versoes_configuracoes_bot IS 'Versões dos rascunhos e configurações publicadas do bot por empresa.';
COMMENT ON TABLE eventos_auditoria IS 'Histórico de ações realizadas pelos usuários nas configurações.';

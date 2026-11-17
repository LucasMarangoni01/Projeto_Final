# Backup e recuperação de dados — FlangeGuard

**Referência:** TAP, item "Banco de Dados — Backup e Recuperação", e RNF04/RN08 do TCC.
**Estado:** procedimento e scripts disponibilizados; teste real de restauração **ainda pendente**.

## Objetivos e limites

- Evitar perda de cadastros e históricos de flanges, funcionários, manutenções, leituras e alertas.
- Salvar arquivos fora do GitHub e fora da pasta de recursos públicos do Spring Boot.
- Recuperar primeiro em um **banco de testes separado**; nunca executar o script de teste no banco de produção.
- Os scripts não substituem backups gerenciados pelo provedor de banco; requerem agendamento, armazenamento externo e testes periódicos.

## Windows + MySQL Workbench / MySQL CLI

1. Instale o MySQL client, `mysqldump` e `mysql_config_editor` e adicione ao `PATH`.
2. No **computador autorizado**, configure um login path local protegido:

   ```cmd
   mysql_config_editor set --login-path=soda_backup --host=localhost --user=SEU_USUARIO_BACKUP --password
   ```

   A ferramenta pedirá a senha interativamente. Não grave senha em script, chat ou Git.
   A conta precisa das permissões adequadas para backup do banco específico.
3. Execute `scripts\backup-mysql.cmd` na raiz do projeto.
4. O resultado será criado como `backups\flangeguard_YYYYMMDD_HHMMSS.sql`, somente
   após o `mysqldump` terminar com sucesso. O script mostra o SHA-256 para conferência.
5. Mantenha uma cópia **criptografada e externa** aos computadores de operação, com acesso
   restrito e política de retenção definida pela equipe.

A pasta `backups/` deve permanecer excluída do versionamento.

## Restauração de teste

1. Em MySQL separado, crie o banco **soda_caustica_flanges_teste** com codificação
   compatível e configure outro login path de testes:
   ```cmd
   mysql_config_editor set --login-path=soda_backup_teste --host=localhost --user=SEU_USUARIO_TESTES --password
   ```
2. Em ambiente isolado, execute:
   ```cmd
   scripts\restaurar-mysql.cmd "backups\flangeguard_YYYYMMDD_HHMMSS.sql" --confirmar
   ```
3. Confirme com a palavra solicitada e revise os resultados.
4. Compare contagens de flanges, sensores, leituras, incidentes, usuários e manutenções
   entre origem e banco de testes. Verifique vínculos e amostras do histórico.
5. Registre data, versão, tempo de recuperação, erros e responsável pelo teste.

**Importante:** o script de restauração opera exclusivamente no nome de banco
`soda_caustica_flanges_teste` com o login path `soda_backup_teste`;
um SQL exportado pode incluir comandos destrutivos, então **inspecione o arquivo
antes da restauração** e use instância isolada.

## Antes de considerar o requisito concluído

- [ ] Validar a execução do backup com dados reais no MySQL de testes.
- [ ] Verificar integridade do arquivo e SHA-256.
- [ ] Restaurar em outra instância isolada.
- [ ] Comparar dados e relacionamentos.
- [ ] Habilitar agendamento e cópia externa criptografada.
- [ ] Testar recuperação periodicamente.

O código do projeto **não garante** que backups estejam agendados ou que a recuperação
tenha sido validada na instalação do usuário.

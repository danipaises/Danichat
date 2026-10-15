# Conta online e compatibilidade BitChat

## Incremento 0.2.0-dev

A aba **Rede BitChat** abre o onboarding e as conversas reais herdadas.
A aba **Conta DaniChat** adiciona cadastro/login por e-mail e senha,
confirmação de e-mail exigida pelo servidor, renovação de sessão,
logout e edição de nome/descrição persistidos em PostgreSQL pelo Supabase.
A conta é opcional e não depende de chip, telefone ou permissões de Bluetooth.
O onboarding de rádio existente continua com seu ciclo de inicialização herdado;
essa separação de telas não constitui uma revisão desse ciclo.

São dois módulos novos: `:data:account` (Auth/PostgREST e sessão protegida)
e `:feature:account` (Compose Material 3 e ViewModel). O cliente usa as APIs
HTTP públicas do Supabase com OkHttp/Gson já presentes, sem adicionar SDKs,
provedores da Meta ou nova biblioteca criptográfica.

O servidor guarda somente o perfil online criado por seu proprietário. O e-mail
fica no Supabase Auth, fora da tabela de perfis. Nenhuma agenda, chave privada,
identidade Noise/Nostr ou mensagem é sincronizada por essa camada.
O perfil online não altera automaticamente o apelido anunciado na mesh.

## Preparar o projeto

1. Criar um projeto exclusivo no Supabase e conferir o plano/custo no painel.
   Não reaproveitar tabelas de outro aplicativo nem excluir projetos por scripts
   de migração. A exclusão de um projeto é uma operação administrativa separada.
2. Aplicar `supabase/migrations/20261015174601_danichat_private_profiles.sql`
   pelo SQL Editor ou pela CLI autenticada, após revisar o SQL.
3. Manter a confirmação de e-mail habilitada em Authentication / Providers.
   Configurar Site URL para uma página HTTPS controlada pelo operador, capaz
   de informar que a confirmação terminou. O usuário confirma no navegador e
   volta ao aplicativo para entrar com a senha; não há importação de tokens
   de redirecionamentos nem deep link de autenticação nesta entrega.
4. Configurar SMTP próprio antes de abrir cadastros ao público. O remetente
   padrão do Supabase limita destinatários e volume; a criação de uma conta
   não comprova que o e-mail foi entregue. Rate limits permanecem ativos.
5. Obter **somente** a chave publicável `sb_publishable_...` e a URL HTTPS.
   Configurar ambas como variáveis de ambiente ou propriedades Gradle:

   ```text
   DANICHAT_SUPABASE_URL=https://SEU-PROJETO.supabase.co
   DANICHAT_SUPABASE_PUBLISHABLE_KEY=sb_publishable_SUA_CHAVE_PUBLICA
   ```

   Para Actions, usar repository variables com esses mesmos nomes. Elas são
   configuração pública do cliente; a autorização depende de Auth e RLS.
   Nunca usar `service_role`, `sb_secret_`, senha de banco ou token de gestão
   no APK, Git ou essas variáveis. O build e o cliente rejeitam chaves secretas.
6. Compilar novamente e testar cadastro com confirmação real, login, salvar
   perfil, reiniciar o processo, renovação e logout no projeto escolhido.
   Execute os advisors do Supabase após a migração.

Sem configuração, o APK continua abrindo a rede BitChat e explica que a conta
online ainda não foi ativada. Não se conecta a um projeto de demonstração nem
aceita cadastros ou salvamentos fictícios.

## Banco e autorização

`public.danichat_profiles` tem chave primária/FK `user_id → auth.users.id`,
`display_name` (1–80 caracteres), `bio` (até 500), datas geradas no servidor,
RLS habilitada e políticas de SELECT/INSERT/UPDATE limitadas a `auth.uid()`.
Não há diretório público, SELECT anônimo ou autorização por `user_metadata`.
O cliente não pode modificar datas, trocar o proprietário nem acessar perfis
alheios. O identificador da conta não é uma prova de identidade criptográfica.
O trigger usa `SECURITY INVOKER` e `search_path` vazio, sem funções privilegiadas.

Tokens ficam em EncryptedSharedPreferences protegidas pelo Android Keystore,
em armazenamento separado do BitChat e associado à origem do projeto. Falha
no Keystore interrompe a operação; não existe fallback em texto simples.
A renovação é serializada para evitar reutilização concorrente de refresh tokens.
Redirects HTTP são recusados, e o cliente usa o provedor de rede herdado para
respeitar a seleção de Tor em cada solicitação. Sair da conta não apaga chaves
nem histórico offline. Se a revogação remota falhar, a UI informa que saiu
somente neste aparelho. JWTs já emitidos podem continuar válidos até expirar.
O apagamento de emergência herdado agora cancela operações de conta e remove a
sessão local sem esperar rede. Falha de remoção é tratada como apagamento
incompleto; não há promessa de exclusão de dados no servidor nem revogação
remota durante esse fluxo offline.

O provedor pode observar dados de conta/perfil, horários, IP de origem ou de saída
Tor e operações da API. RLS não oculta dados dos administradores do projeto.
E-mail, armazenamento, compute, banda, backups e SMS futuro podem gerar custos,
mesmo quando há franquia gratuita. Não há SMS nesta entrega.

## Compatibilidade e próximos passos

Os protocolos, tipos de mensagem, UUIDs de descoberta, mesh, Noise, identidade e
Nostr herdados não foram substituídos por chamadas ao banco. A referência auditada
é o commit BitChat `6ade68256cf32ca327047ba7f618ac6514f205ff`.
O applicationId continua próprio, permitindo instalar os dois aplicativos juntos.
Supabase é uma extensão do DaniChat: o BitChat original não acessa esse backend.

Preservar código e passar vetores JVM não substitui testar DaniChat ↔ BitChat
em aparelhos físicos, incluindo anúncio, privadas, reconexão e encaminhamento.
Não há confirmação de interoperabilidade por rádio desta versão sem esse teste.

Ainda faltam: recuperação de senha integrada ao app, foto, vínculo autenticado
conta/Noise/Nostr, histórico comum, Room/outbox durável, transporte de envelopes
online cifrados, mídia e grupos DaniChat. Recuperar o login não recupera chaves
nem histórico. O codec Nostr legado continua sujeito às ressalvas da auditoria;
esta etapa não o declara NIP-44 padrão nem criptografia auditada.

A próxima mudança deve introduzir um vínculo com prova de posse, consentimento e
verificação das chaves, antes de fundir contatos/históricos. O novo transporte
online deve armazenar somente envelopes cifrados com protocolo consolidado e
MessageId estável, mantendo o adapter BitChat interoperável. Não criar uma tabela
de mensagens em texto simples para antecipar essa integração.

## Testes reproduzíveis

```sh
python3 tools/danichat/test_profile_rls.py
./gradlew :data:account:testDebugUnitTest :data:account:lintDebug :feature:account:lintDebug :app:assembleDebug --no-daemon --max-workers=2
```

O primeiro comando cria e remove PostgreSQL 17 isolado em Docker, sem portas
publicadas nem rede, com fixtures sintéticas e representação mínima de
`auth.users`/`auth.uid()`. Executa a migração real e verifica upsert, grants,
isolamento, limites e tentativa de transferência de proprietário.
Não é um teste do Supabase Auth hospedado. Os testes HTTP usam apenas servidor
loopback de teste para conferir sessões e credenciais; não simulam envios de chat.

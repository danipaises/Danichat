# Verificação do incremento de conta online

Versão `0.2.0-dev`, applicationId `com.danichat.android`, APK debug universal.
Esta entrega adiciona a camada opcional de conta; não conclui o mensageiro
online com histórico unificado.

## Escopo implementado

- Dois módulos: `:data:account` e `:feature:account`.
- Cliente real de cadastro/login Supabase Auth, confirmação exigida pelo servidor,
  renovação serializada, logout e perfil via PostgREST.
- Sessão protegida pelo Keystore, vinculada à origem do servidor, sem fallback
  em texto simples; erros exibidos sem respostas cruas ou tokens.
- Migração PostgreSQL com RLS e grants por proprietário, sem diretório público,
  telefones, contatos ou tabela de mensagens em texto simples.
- Aba de conta independente da navegação BitChat. Apagamento de emergência
  cancela operações da conta e remove sua sessão local, sem depender de rede.
- Configuração pública opcional por build/Actions; não há projeto de demonstração
  usado automaticamente nem aceitação fictícia de cadastro/perfil.

## Evidências locais

| Verificação | Resultado |
| --- | --- |
| Compilação final `:app:assembleDebug` | `BUILD SUCCESSFUL`, locks e verificação de dependências normais |
| Manifesto do APK | Pacote próprio, versionCode 2, versionName `0.2.0-dev` |
| Assinatura/alinhamento | Debug v2 e alinhamento 16 KB conferidos; mesmo certificado do APK `0.1.0-dev` |
| Testes JVM de `:data:account` | 16 casos, zero falhas/erros |
| Testes JVM de `:feature:account` | 3 casos, zero falhas/erros |
| Suíte JVM herdada do app | 613 casos, 3 ignorados, zero falhas/erros |
| Testes de ferramentas do host | 14 aprovados |
| PostgreSQL 17 isolado | Migração, upsert, isolamento entre usuários, proibição anônima, troca de proprietário, datas e limites verificados |
| Lint dos módulos novos | Sem problemas |
| Lint geral do app | 305 erros, 352 avisos e 17 sugestões; não aprovado como gate de qualidade |
| Checksums nativos Arti | Quatro ABIs conferidas; entradas nativas preservadas |
| Hashes de dependências | 1.330 entradas anteriores preservadas; 10 artefatos adicionais fixados |

O teste PostgreSQL executa a migração real com representação mínima do schema
Auth e JWTs sintéticos em um container sem rede/portas públicas. Não é um teste
do Supabase Auth hospedado. Os testes HTTP usam servidor loopback para conferir
contratos, tokens e falhas; nenhum envio de mensagem de chat foi simulado.

O lint geral do app tem erros herdados de permissões/APIs, recursos de idioma,
pluralização e outros pontos. `abortOnError=false` é configuração herdada:
`BUILD SUCCESSFUL` não equivale a lint geral aprovado. Os problemas novos de
tradução dos rótulos DaniChat foram corrigidos; a dívida restante não foi
ocultada por um novo baseline. Erros e avisos herdados continuam registrados.

APK universal: 79.811.213 bytes. SHA-256:
`461baa0d5b3f15131d4d948e6fdf7193bd8d5dd56df0b22a401c8f7bcadd53ee`.
A conferência do arquivo não encontrou o caminho do ambiente de compilação.
A assinatura debug não é uma assinatura de distribuição; o APK contém os hooks
ADB de laboratório herdados e se destina a aparelhos de teste.

## Compatibilidade preservada e limites

Comparação com o commit auditado BitChat
`6ade68256cf32ca327047ba7f618ac6514f205ff`: os diretórios de protocolo, modelos,
mesh, Wi-Fi Aware, Noise, criptografia, identidade, Nostr e sincronização, além
do serviço de bridge, permanecem sem alterações. A suíte herdada inclui seus
contratos binários e vetores externos Noise. A mudança no ChatViewModel limita-se
ao hook de limpeza da conta durante o apagamento de emergência.

Nenhum Android físico esteve disponível. Não foram validados instalação,
inicialização, navegação em aparelho, interoperabilidade DaniChat ↔ BitChat,
reconexão por rádio, bateria ou cenário mesh multissalto. Equivalência de fonte
e testes JVM não substituem esses ensaios.

## Ativação remota pendente

O projeto hospedado DaniChat ainda não foi criado/configurado nesta entrega.
Nenhum projeto existente foi excluído ou alterado. O APK sem URL/chave publicável
exibe a conta como não ativada e mantém o acesso à rede BitChat.

Faltam aplicar a migração no projeto escolhido, executar advisors, configurar
confirmação/SMTP e testar cadastro/login/perfil com o serviço real. Procedimento
em [SUPABASE.md](SUPABASE.md). Esse bloqueio não é tratado como uma implantação
remota bem-sucedida.

Continuam pendentes mensagens cifradas pelo novo backend, vínculo autenticado
conta/Noise/Nostr, recuperação de identidade, Room/outbox durável, histórico
unificado, mídia e grupos DaniChat. O codec Nostr legado e a outbox em memória
mantêm as limitações da auditoria. Não há declaração de segurança de produção
nem assinatura de distribuição nesta versão.

O resultado histórico da Fase 1 está em [VERIFICACAO.md](VERIFICACAO.md).

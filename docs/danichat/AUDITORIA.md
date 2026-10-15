# Auditoria inicial — DaniChat

Data: 2026-10-15. Inspeção de código, não auditoria criptográfica independente.
Base: `permissionlesstech/bitchat-android`, commit
`6ade68256cf32ca327047ba7f618ac6514f205ff` (2026-10-11).
Não foi usado REA MCP: não há necessidade de engenharia reversa para ler este
código aberto. Não foram analisados aplicativos ou protocolos privados da Meta.

## Licença e procedência

O README original declara domínio público, mas `LICENSE.md` contém GNU GPLv3.
Este fork preserva a GPLv3, os avisos existentes e o histórico Git. O README
original foi guardado em `docs/upstream/README.md`. A distribuição de binários
deve acompanhar o código-fonte correspondente, instruções de compilação e
licenças dos componentes. O nome DaniChat identifica uma versão modificada,
sem sugerir endosso dos mantenedores do BitChat.

## O que existe na base

| Área | Evidência no código | Limite observado |
| --- | --- | --- |
| BLE | `mesh/BluetoothMeshService`, gerentes GATT, descoberta, fragmentação | Mesh própria sobre BLE/GATT; não é o padrão Bluetooth SIG Mesh |
| Encaminhamento | `mesh/MeshCore`, `PacketRelayManager`, `services/meshgraph`, `StoreForwardManager` | TTL, deduplicação e limites de rádio exigem ensaio físico |
| Wi-Fi Aware | `wifi-aware/WifiAwareController`, `WifiAwareMeshService`, `UnifiedMeshService` | Opcional, desativado por padrão; depende do chipset, permissões e disponibilidade NAN |
| Wi-Fi Direct | `hotspot/HotspotManager` usa `WifiP2pManager` | Compartilhamento do APK por hotspot; não é transporte de mensagens |
| Privadas locais | `noise/NoiseSession`, `NoiseSessionManager`, `NoiseEncryptionService` | Noise XX/X25519/ChaChaPoly; verificação de identidade necessária contra MITM na primeira associação |
| Identidade | `identity/SecureIdentityStateManager`, `nostr/NostrIdentity`, `services/ContactDirectory` | Chaves persistentes e vínculo Noise/Nostr existentes; não há cadastro/recuperação completos do DaniChat |
| Proteção local | `services/ConversationStorageCipher`, `ConversationRepository` | Payloads privados cifrados com AES-GCM/Keystore; metadados e arquivos precisam de revisão própria |
| Histórico | `services/ConversationRepository` usa `SQLiteOpenHelper` | Não usa Room; migração deve preservar banco, exclusões e aliases |
| Internet | `nostr/NostrTransport`, `NostrClient`, `NostrRelayManager` | Relays e reconexão existentes; descoberta por geohash e favoritos mútuos, não um cadastro universal |
| Criptografia Nostr | `nostr/NostrCrypto.encryptNIP44`, `NostrProtocol` | Formato `v2:` + base64url de XChaCha20-Poly1305 não corresponde ao NIP-44 v2 padrão |
| Roteamento | `services/MessageRouter`, `UnifiedMeshService` | Seleção mesh/Nostr existente; prioridade exata solicitada ainda precisa de especificação e testes |
| Fila | `MessageRouter.outbox` é `ConcurrentHashMap` | Perdida com encerramento do processo; não equivale a outbox durável |
| Mídia | `ui/MediaSendingManager`, `mesh/PrivateMediaTransfer`, pacote de arquivos e voz | Caminhos locais existentes; paridade de mídia/grupos privados online não foi demonstrada |
| Execução Android | `service/MeshForegroundService`, `BootCompletedReceiver`, WorkManager | Restrições de início, bateria e permissões variam por versão e fabricante |
| UI | Compose, Material 3, `ChatViewModel`, conversas, contatos e configurações | Tradução pt-BR incompleta e várias telas herdadas; redesign completo é a Fase 2 |
| Testes | `app/src/test`, contratos de protocolo e `tools/release_gate/mesh_lab.py` | Testes de JVM não comprovam descoberta, alcance ou entrega por rádio |

Os caminhos da tabela são relativos a `app/src/main/java/com/bitchat/android/`.
O relatório `docs/security-review-jul-27.md` herdado já registra a divergência
NIP-44 (M13). Outros achados daquele relatório devem ser reavaliados contra o
commit atual; a presença de um relatório não prova correção nem vulnerabilidade
atual de todos os itens.

## Decisões da Fase 1

1. Fazer fork integral com identidade de instalação própria (`com.danichat.android`)
   e manter o namespace Kotlin herdado para reduzir mudanças de protocolo.
2. Preservar o núcleo BLE, NAN, Noise, pacotes e roteador. Não introduzir uma
   segunda pilha de mensagens nem envios simulados.
3. Iniciar a separação Gradle por uma feature real de boas-vindas, desacoplada
   dos serviços. Extrair transportes depois de estabelecer testes de regressão.
4. Desabilitar fontes de atualização do BitChat no fork e retirar seu certificado
   de distribuição. Uma versão DaniChat não deve oferecer APKs de outro produto.
5. Manter configurações e armazenamento internos herdados nesta etapa. A nova
   applicationId fornece sandbox/Keystore separados e permite instalação lado
   a lado. Isso não importa automaticamente o histórico de uma instalação BitChat.

## Riscos que bloqueiam uma versão de produção

- Substituir o codec Nostr legado por implementação consolidada e validar
  vetores oficiais NIP-44/NIP-17, com versão explícita e estratégia de migração.
  Não mudar o codec silenciosamente nem fazer downgrade automático.
- Revisar logs herdados que contêm identificadores e metadados. Não enviar
  logcat ou dados de usuários a serviços de diagnóstico.
- Persistir outbox, tentativas e confirmações de forma transacional; impedir
  reenvio indevido após morte de processo ou mudança de transporte.
- Revisar a exposição de geohash, filtros Nostr e chaves/horários aos relays.
  Mensagens de canais públicos são públicas e não recebem a garantia de privadas.
- Concluir descoberta por consentimento, bloqueio/denúncia, recuperação,
  perfis e verificação de telefone. Não existe serviço SMS nesta entrega.
- Validar rádio, serviços e permissões em dois aparelhos Android físicos.

Nenhum destes pontos autoriza declarar o produto seguro, interoperável com
clientes Nostr genéricos ou pronto para produção.

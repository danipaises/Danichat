# Verificação da primeira entrega

Data: 2026-10-15. Artefato: DaniChat `0.1.0-dev`, applicationId
`com.danichat.android`. Esta é uma versão debug de desenvolvimento.

## Resultados locais

| Verificação | Resultado |
| --- | --- |
| Compilação Android inicial `:app:assembleDebug` | `BUILD SUCCESSFUL` |
| Recompilação com locks/hashes ativos e cache offline | `BUILD SUCCESSFUL`; 78 tarefas, sem desativar verificação de dependências |
| Lint `:feature:welcome:lintDebug` | Sem erros ou avisos |
| Testes das ferramentas `tools/release_gate` | 14 testes aprovados |
| Testes JVM do app | Bloqueados antes da execução: HTTP 503 persistente/intermitente nos repositórios Google/Maven ao resolver dependências do Robolectric; houve retentativas |
| Lint do app completo | Não concluído: resolução das dependências dos modelos de teste bloqueada; não é um resultado aprovado de lint do app |
| Bibliotecas Arti nativas | Checksums fixados conferidos nas quatro ABIs |
| Instalador SDK fixado | Executado em raiz vazia; três arquivos oficiais conferidos por SHA-256 e instalados |
| Recursos XML, YAML dos workflows e sintaxe dos scripts | Conferidos |
| Assinatura do APK universal | Verificada com `apksigner`, esquema v2, chave debug |
| Alinhamento do APK | Verificado com `zipalign -c -P 16 4` |
| Identidade no manifesto | Nome, pacote, Activity, FileProvider, permissão interna e action do hook debug conferidos |

A geração de hashes adicionou artefatos transitivos necessários à feature
Compose. Nenhum hash de artefato anteriormente fixado foi substituído e os
locks do app/Wear não tiveram versões alteradas. A recompilação final usa a
verificação estrita normal, sem gerar ou relaxar hashes.

## APK

Arquivo: `app/build/outputs/apk/debug/app-universal-debug.apk`.
SHA-256: `b83771cdcc5099199d98decdbd148f5817c78519948ee1505ce56895a7e02e68`.

Android mínimo API 26; target/compile API 37.
ABIs: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.

O pacote é independente do BitChat. O namespace das classes e o formato de
pacotes de rádio foram mantidos. A permissão interna foi isolada para evitar
conflito de instalação com o aplicativo original. Não há migração automática
de dados entre sandboxes.

O build debug teve avisos de APIs depreciadas herdadas e bibliotecas nativas
empacotadas sem remoção de símbolos; não houve erro de compilação. Os bloqueios
de testes/lint completo são de resolução de dependências, anteriores à execução
da suíte JVM. Nenhum teste Android foi marcado como aprovado por esse motivo.

## O que não foi validado

- Nenhum aparelho Android estava disponível via ADB. Instalação, inicialização
  em Android real, descoberta, mensagens, reconexão, rádio e bateria não foram
  testados fisicamente. Não foi executado emulador ou teste instrumentado.
- Não foi enviada mensagem real entre dois aparelhos. Não há evidência de
  alcance, entrega, multissalto ou desempenho de Wi-Fi Aware desta compilação.
- Não foram usados contatos, telefone, GPS, mensagens ou contas reais.
- Workflows foram configurados e inspecionados, mas não executados no GitHub.
- Não foi gerado release assinado nem fornecido certificado de distribuição.
  A assinatura debug não deve ser confundida com assinatura release.
- Isto não é auditoria criptográfica. O codec Nostr herdado diverge do NIP-44
  oficial e a outbox em memória ainda não atende à durabilidade solicitada.

## Estado das fases

Fase 1 implementada com auditoria, origem/licença preservadas, arquitetura e
primeiro módulo de apresentação separado. Primeira compilação debug entregue.
A Fase 2 começou pela entrada e pelo tema próprios, com integração ao app real;
o redesign completo e a revisão de tradução ainda estão pendentes.

As Fases 3–6 ainda exigem os incrementos descritos em `FASES.md`. A Fase 7
depende de aparelhos físicos. A configuração inicial da Fase 8 está no código;
sua validação remota e a assinatura release dependem do repositório e de
credenciais seguras. Nenhuma fase foi marcada concluída por simulação de envio.

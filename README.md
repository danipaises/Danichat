# DaniChat Android

Mensageiro Android independente da Meta, derivado do BitChat Android. Kotlin,
Jetpack Compose e Material 3, com a pilha real de comunicação da base preservada.

**Marco atual: Fase 1 e primeira compilação do fork. Produto em desenvolvimento.**
Não é uma implementação completa de todas as oito fases, nem uma versão com
segurança validada. Os resultados de build e testes estão em
[Verificação](docs/danichat/VERIFICACAO.md).

- Identidade de instalação DaniChat: `com.danichat.android`, versão `0.1.0-dev`.
- Boas-vindas próprias em módulo Gradle, tema e marca originais; idioma inicial pt-BR.
- Descoberta BLE, encaminhamento, Noise, Wi-Fi Aware opcional e integração Nostr
  herdados, sem substituir comunicação por simulações.
- Atualizações do BitChat desabilitadas para não oferecer APKs de outro produto.
- Telas e tradução herdadas ainda precisam da revisão completa da Fase 2.

A outbox atual não é durável, o codec Nostr legado diverge do NIP-44 padrão,
e não há telefone verificado, recuperação completa, Room ou paridade de mídia
online/grupos privados do projeto final. Wi-Fi Direct compartilha APK; não é
um transporte de chat. Testes em aparelhos físicos continuam pendentes.

## Documentação

- [Auditoria do código e limitações](docs/danichat/AUDITORIA.md)
- [Arquitetura e decisões](docs/danichat/ARQUITETURA.md)
- [Fases e critérios de aceite](docs/danichat/FASES.md)
- [Compilação, instalação e Actions](docs/danichat/COMPILACAO.md)
- [Resultados da verificação](docs/danichat/VERIFICACAO.md)

```sh
# JDK 21 completo, SDK 37 e Build Tools 37.0.0
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk
```

APK debug serve para desenvolvimento e inclui hooks de laboratório por ADB.
Relays podem observar metadados; canais públicos expõem conteúdo. Leia as
limitações antes de usar dados sensíveis. Não foram usados serviços da Meta,
protocolos privados ou engenharia reversa do WhatsApp.

## Origem e licença

Base: [BitChat Android](https://github.com/permissionlesstech/bitchat-android),
commit `6ade68256cf32ca327047ba7f618ac6514f205ff`.
Fork modificado a partir de 2026-10-15, sob [GNU GPLv3](LICENSE.md), sem garantia.
Veja [NOTICE](NOTICE.md) e o [README upstream preservado](docs/upstream/README.md).
Ao distribuir APK, disponibilize também o código-fonte correspondente e os
avisos de licença. DaniChat não é uma versão oficial do BitChat.

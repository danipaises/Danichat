# Compilar, instalar e configurar CI

## Compilação local

Requer JDK completo 21 (com `javac`), Gradle Wrapper 9.6.1, SDK Android 37 e
Build Tools 37.0.0. Versões e SHA-256 dos pacotes SDK ficam em
`tools/reproducible-builds/TOOLCHAIN.env`. O módulo de celular mantém API mínima
26. Use Android Studio ou instale os pacotes fixados em um diretório vazio:

```sh
python3 tools/danichat/install_sdk.py --sdk-root <sdk-vazio> --accept-license
```

O parâmetro confirma a aceitação da licença do SDK Android. Configure
`ANDROID_HOME`/`ANDROID_SDK_ROOT`, ou `sdk.dir` no `local.properties` não versionado.
Ambientes com proxy devem configurar o proxy JVM e a CA confiável conforme seu
administrador; não desative TLS nem a verificação de dependências.

```sh
tools/arti-build/verify-checksums.sh
python3 -m unittest discover -s tools/release_gate -p 'test_*.py'
./gradlew :app:testDebugUnitTest :app:lintDebug :feature:welcome:lintDebug :app:assembleDebug --no-daemon --max-workers=2
python3 tools/danichat/test_profile_rls.py
./gradlew :data:account:testDebugUnitTest :feature:account:testDebugUnitTest :data:account:lintDebug :feature:account:lintDebug --no-daemon --max-workers=2
```

APK universal: `app/build/outputs/apk/debug/app-universal-debug.apk`.
Também são geradas variantes por ABI. Instale o universal:

```sh
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk
```

Pacote: `com.danichat.android`. Pode coexistir com BitChat. A assinatura debug é
somente para desenvolvimento. Não equivale a assinatura de distribuição nem
permite atualizar instalações assinadas com outro certificado. O APK debug
contém os hooks ADB de laboratório herdados; use apenas em aparelhos de teste.

## Actions

- `android-build.yml`: verifica bibliotecas nativas, testes de host/JVM, lint e
  gera APK debug do celular em push/PR ou acionamento manual.
- `release.yml`: manual; verifica e compila APK release **sem assinatura**.
  O job de assinatura roda somente em `main`, com `sign_release=true`, no
  environment `release`, e falha se os segredos estiverem ausentes.
- `fetch-georelays.yml`: atualização herdada dos relays somente manual.

A compilação CI do marco `0.1.0-dev` passou no GitHub; isso não comprova a
validação de versões posteriores. Os workflows de build não publicam uma
release automaticamente. O workflow manual `publish-prebuilt-debug.yml` envia
arquivos previamente compilados/conferidos a uma pré-release existente como
rascunho; confere hashes e o commit correspondente à tag, e mantém o rascunho
até a publicação explícita. Configure o environment `release` com revisores e
restrição de branch antes de habilitar assinatura. Declarar o environment no
YAML não configura essas proteções no GitHub.

Secrets do environment:

| Nome | Conteúdo |
| --- | --- |
| `DANICHAT_KEYSTORE_BASE64` | Keystore de distribuição codificado em base64 |
| `DANICHAT_KEY_ALIAS` | Alias da chave |
| `DANICHAT_STORE_PASSWORD` | Senha do keystore |
| `DANICHAT_KEY_PASSWORD` | Senha da chave |

Variable `DANICHAT_RELEASE_CERT_SHA256`: SHA-256 público do certificado esperado,
64 caracteres hexadecimais. Não reutilizar a chave/certificado de outro produto.
O script `tools/danichat/sign-release.sh` usa arquivo temporário privado,
senhas por ambiente, limpa a chave no término e confere certificado/assinatura
e alinhamento antes de produzir o artefato. Nunca guardar esses secrets no Git,
em argumentos de senha ou em logs. Não há credenciais de release neste código.

As fontes de download/atualização dentro do app permanecem vazias até existir
uma distribuição DaniChat definida. Compartilhar o próprio APK instalado
continua disponível pelos caminhos herdados. Certificados e endpoints de
atualização precisam de revisão conjunta antes de habilitar downloads.

Os scripts de release reproduzível em `tools/reproducible-builds/` e o módulo
Wear são referências upstream. Os workflows DaniChat acima não reivindicam
reprodutibilidade byte a byte nem distribuição Wear. Não usar o antigo script
de assinatura BitChat para assinar DaniChat.

## Custos

Ferramentas de build são abertas/gratuitas; GitHub Actions e armazenamento de
artefatos estão sujeitos às franquias e regras do plano do repositório. Nada
foi provisionado em provedores pagos. SMS, hospedagem, relays e mídia de fases
futuras podem gerar custos, conforme `ARQUITETURA.md`.

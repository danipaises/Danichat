# Entregas e critérios de aceite

O primeiro marco desta execução é Fase 1 + compilação debug do fork. As demais
fases têm critérios explícitos; código herdado não comprova conclusão delas.

O incremento `0.2.0-dev` adiciona conta/perfil Supabase opcional conforme
[SUPABASE.md](SUPABASE.md). Isso inicia a camada online solicitada sem concluir
a Fase 3: vínculo criptográfico, recuperação e histórico unificado permanecem
pendentes. A compatibilidade de protocolo BitChat é uma restrição permanente.

| Fase | Entrega | Verificação exigida |
| --- | --- | --- |
| 1 | Auditoria, origem/licença, arquitetura, identidade de instalação própria, primeira feature modular e compilação | Build debug real, contratos/JVM disponíveis, conferência do manifesto e assinatura debug |
| 2 | UI pt-BR própria, perfil, conversas, proximidade, privacidade/conectividade e permissões progressivas | Fluxos reais sem mensagens fictícias; acessibilidade, rotação, processo restaurado e rádio preservado |
| 3 | Identidade persistente unificada e Nostr privado padronizado | Vetores externos, vínculo autenticado Noise/Nostr, recuperação cifrada e teste com relay controlado |
| 4 | Número opcional E.164 + verificador real | OTP expirado/reutilizado/incorreto, limites de abuso, assinatura do vínculo e uso completo sem SIM |
| 5 | Roteador e outbox Room duráveis | Morte do processo, ACK perdido, offline/online, duplicatas e prioridade de rotas sem fan-out |
| 6 | Fotos, arquivos, voz e grupos privados possíveis | Blobs cifrados, limites, retomada, troca/revogação de membros e permissões sob demanda |
| 7 | Dois aparelhos físicos | BLE direto/relay, privadas, NAN compatível, reconexão, tela apagada e queda de rede; nenhum resultado inventado |
| 8 | GitHub Actions debug e release com credenciais seguras | CI verde no repositório do fork, artefatos; release somente com secrets e ambiente protegido |

## Matriz física a executar

- Instalar o mesmo APK debug em dois aparelhos, sem restaurar identidades alheias.
- Conceder permissões gradualmente; negar cada opcional e verificar comportamento.
- Desligar internet: descoberta BLE, verificação mútua, privadas nos dois sentidos,
  voz/arquivo local e recibos disponíveis. Registrar duplicatas e falhas.
- Alternar Bluetooth, bloquear a tela, encerrar processo e reabrir; verificar
  histórico e documentar a limitação atual da outbox em memória.
- Habilitar Wi-Fi Aware em hardware compatível e repetir sem confundir ausência
  de suporte com defeito do transporte. Não usar localização real como fixture.
- Usar terceiro aparelho para demonstrar encaminhamento multissalto; dois
  aparelhos apenas não demonstram uma topologia com relay intermediário.
- Testar internet com contas sintéticas e relay controlado após revisão do codec.

O Mesh Lab herdado continua disponível em `tools/release_gate`. Seus hooks são
exclusivos de debug e podem executar operações por ADB; não distribuir esse APK
como release de produção. Nenhum teste físico foi executado nesta fase.

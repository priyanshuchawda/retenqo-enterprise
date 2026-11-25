# Performance and resource engineering

| Design choice | Evidence |
| --- | --- |
| Rule-first extraction | [RulesExtract.kt](../android/mneme/app/src/main/java/com/mneme/app/ai/RulesExtract.kt) limits unnecessary inference |
| Bounded queue | [NotifIngestQueue.kt](../android/mneme/app/src/main/java/com/mneme/app/notif/NotifIngestQueue.kt): pending capacity 500; overflow behavior needs reliability review |
| Power-aware work | [MnemePowerGates.kt](../android/mneme/app/src/main/java/com/mneme/app/media/MnemePowerGates.kt) |
| Bounded retrieval | [NoteRepository.kt](../android/mneme/app/src/main/java/com/mneme/app/data/NoteRepository.kt): recent-candidate limit trades recall for bounded work |
| Quantized model | Qwen3.5-0.8B Q4_0, 563,036,064-byte separate download |
| Static website | [astro.config.mjs](../web/astro.config.mjs): lightweight self-hosted scripts |

## Recorded measurements

| Metric | Result and scope |
| --- | --- |
| Published v0.1.4 APK | 39,374,307 bytes (~37.55 MiB) |
| Historical Galaxy M32 model run | About 831 MB process PSS, not remeasured for this revision |
| Website Lighthouse 13.6.0, one mobile-simulated production run | Performance 92, LCP 1.263 s, CLS 0, TBT 244 ms, Speed Index 4.971 s |

Sources: [release](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.4), [implementation history](IMPLEMENTATION_STATUS.md), [website audit](../web/docs/ENGINEERING_AUDIT.md). The website performance target is 95; current field INP, battery and thermal measurements are not established. Future benchmarks should record device, model, revision, cold/warm state, workloads, latency distribution, peak memory and failures.

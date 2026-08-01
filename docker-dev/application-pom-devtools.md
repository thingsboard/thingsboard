# Spring DevTools w module `application` — hot-reload bez restartu procesu

Moduł `application` nie ma zależności `spring-boot-devtools`. Bez niej po zmianie
w kontrolerze trzeba zrestartować całą aplikację (30–60 s). Z nią Spring podnosi
tylko kontekst — zwykle **3–8 s**.

Watcher w kontenerze działa w obu wariantach (`TB_DEV_RELOAD=auto` sam wykrywa,
co jest dostępne), więc ta zmiana jest **opcjonalna, ale mocno zalecana**.

## Zmiana w `application/pom.xml`

Dodaj profil na końcu pliku, tuż przed zamykającym `</project>`:

```xml
    <profiles>
        <profile>
            <id>dev</id>
            <dependencies>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-devtools</artifactId>
                    <optional>true</optional>
                </dependency>
            </dependencies>
        </profile>
    </profiles>
```

Jeśli `<profiles>` już istnieje, dołóż samą sekcję `<profile>` do środka.

## Włączenie profilu w kontenerze

W `dev/.env.dev` dopisz:

```properties
TB_DEV_RELOAD=devtools
```

i w `dev/docker-compose.dev.yml`, w usłudze `tb-node-dev`, dodaj do `environment`:

```yaml
      MAVEN_ARGS: "-Pdev"
```

Potem:

```bat
dev\dev.cmd rebuild
```

## Dlaczego `optional` i osobny profil

`spring-boot-devtools` nigdy nie może trafić do artefaktu produkcyjnego —
wyłącza cache szablonów, otwiera endpoint restartu i wydłuża start. Profil `dev`
uruchamiany tylko przez kontener dev gwarantuje, że build produkcyjny
(`mvn install` bez `-Pdev`) jest bajt w bajt taki jak dziś.

# StartRescue 🚑

Aplicativo Android para **simulação do protocolo START** (triagem em múltiplas vítimas) — focado em **educação e treinamento**.  
Feito em **Kotlin + Jetpack Compose**.

## ✨ Funcionalidades

- Fluxo START completo (deambula → respiração → frequência respiratória → TEC/CRT → obediência a comandos)
- Hemorragia exsanguinante com torniquete obrigatório
- Abertura de vias aéreas (VA) com lógica de respiração pós-VA
- **TEC “Ausente/Absent”** quando não respira / sem circulação efetiva
- 20 questões com cronômetro e feedback
- Exportação de **PDF** e **CSV**
- Tema claro/escuro e **i18n (PT/EN)**

## 🖼️ Screenshots

> Coloque as imagens em `docs/` com os nomes abaixo.

![Home](docs/home.png)
![Exam](docs/exam.png)
![Result](docs/result.png)

## 🔗 Política de Privacidade

- Português: https://SEU-USUARIO.github.io/startrescue-privacy/politica.html
- English: https://SEU-USUARIO.github.io/startrescue-privacy/privacy.html

> No app (Gradle), a URL é lida de `BuildConfig.PRIVACY_URL`.

## 🛠️ Tech

- Kotlin • Jetpack Compose (Material 3)
- ViewModel/State
- Geração de PDF/CSV

## 📦 Build / Versão

Edite em `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.devstart.startrescue"
    minSdk = 24
    targetSdk = 35
    versionCode = 1        // ↑ aumente a cada release
    versionName = "1.0.0"  // exibido ao usuário

    // Política de Privacidade (GitHub Pages)
    buildConfigField("String", "PRIVACY_URL", "\"https://SEU-USUARIO.github.io/startrescue-privacy/politica.html\"")
}
```

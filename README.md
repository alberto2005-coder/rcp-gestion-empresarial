# RCP Gestión Empresarial

[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/SQLite-embebido-003B57?logo=sqlite&logoColor=white)](https://sqlite.org/)
[![GPL-3.0](https://img.shields.io/badge/Licencia-GPL--3.0-blue)](LICENSE)

[Español](#español) · [English](#english)

---

## Español

### Índice

- [Descripción](#descripción)
- [Módulos](#módulos)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Requisitos](#requisitos)
- [Compilación y ejecución](#compilación-y-ejecución)
- [Configuración SMTP](#configuración-smtp)
- [Prototipo web](#prototipo-web)
- [Stack tecnológico](#stack-tecnológico)
- [Seguridad y límites](#seguridad-y-límites)
- [Licencia](#licencia)

### Descripción

Aplicación de escritorio **Rich Client Platform (RCP)** para la gestión integral de una empresa: panel de control, clientes, empleados, pedidos, productos, informes y auditoría. Está construida con **JavaFX 21** y **SQLite**, con una interfaz adaptable (temas claro y oscuro) y un **prototipo web SPA** que replica el flujo y funciona sin conexión.

### Módulos

| Módulo | Función |
|---|---|
| **Dashboard** | Resumen del negocio con gráficas de barras, líneas y sectores |
| **Clientes** | Alta, edición y consulta de clientes |
| **Empleados** | Gestión de plantilla |
| **Pedidos** | Registro y seguimiento de pedidos |
| **Productos** | Catálogo de artículos |
| **Reportes** | Informes y exportación |
| **Configuración** | Preferencias, temas y credenciales |
| **Autenticación** | Login, registro y recuperación de contraseña con OTP |

Cada módulo implementa la interfaz `RCPModule` y se descubre en tiempo de ejecución mediante `ServiceLoader`, de modo que se pueden añadir o quitar complementos sin tocar el núcleo.

### Arquitectura

**Modularidad dinámica (SPI)** — `ServiceLoader` descubre las clases que implementan `RCPModule`. Los botones de navegación, sus iconos (Ikonli) y las vistas se registran de forma dinámica.

**Concurrencia sin bloquear la UI** — las consultas y la generación de informes se ejecutan en tareas `DbTask` que heredan de `javafx.concurrent.Task`, en hilos daemon, para que el hilo de JavaFX no se congele.

**Credenciales protegidas con DPAPI** — en Windows, JNA invoca `Crypt32.dll`: la contraseña SMTP en texto plano se cifra con `CryptProtectData` a nivel de usuario y se reescribe con el prefijo `{DPAPI}`. En otros sistemas el cifrado se omite de forma segura.

**Offline-first** — cada escritura se guarda en local y se encola en `sync_queue` (SQLite en Java, `localStorage` en la web). Un sincronizador en segundo plano procesa la cola en orden FIFO cuando vuelve la conexión.

### Estructura del proyecto

```text
rcp-gestion-empresarial/
├── pom.xml                       # Dependencias Maven y configuración del empaquetado
├── package.bat                   # Automatización del build nativo en Windows
├── src/main/java/
│   ├── module-info.java          # Módulos JPMS
│   └── com/empresa/rcp/
│       ├── App.java              # Arranque y estado de la ventana
│       ├── Main.java             # Launcher compatible con empaquetadores
│       ├── MainController.java   # Menú, inactividad y plugins
│       ├── chart/                # Gráficas sobre Canvas de JavaFX
│       ├── db/DatabaseManager.java
│       ├── sync/                 # SyncManager y SyncProvider
│       ├── util/                 # DbTask, PreferencesManager, SecurityUtil
│       └── module/               # RCPModule + módulos de negocio
├── src/main/resources/
│   ├── logback.xml
│   ├── css/                      # Tema base, oscuro y claro
│   ├── fxml/                     # Vistas (login, registro, dashboard…)
│   └── images/
├── web-app/                      # Prototipo SPA (HTML, CSS, JS, EmailJS)
├── LICENSE
└── README.md
```

### Requisitos

- **JDK 21** o superior.
- **Apache Maven 3.8** o superior.
- **Windows** para el cifrado DPAPI; en macOS o Linux la app funciona, pero sin esa protección de credenciales.

### Compilación y ejecución

```bash
mvn clean compile
mvn javafx:run                 # desarrollo
mvn clean package -DskipTests  # JAR + ejecutable nativo
```

El ejecutable queda en `target/RCPGestion.exe` (Windows). `package.bat` automatiza el empaquetado nativo.

### Configuración SMTP

Para enviar códigos OTP reales en la recuperación de contraseña, crea `config.properties` en la raíz:

```properties
smtp.host=smtp.gmail.com
smtp.port=587
smtp.user=tu_correo@gmail.com
smtp.password=tu_contraseña_de_aplicacion
```

En el primer arranque, la contraseña se sustituye por un valor `{DPAPI}…`. Si no creas el archivo, la aplicación **imprime los OTP por consola** para desarrollo local.

> [!IMPORTANT]
> `config.properties` está en `.gitignore`. No lo subas nunca: contiene la contraseña de aplicación del correo.

### Prototipo web

`web-app/` es una SPA independiente que replica el flujo del escritorio:

```bash
cd web-app
# copia .env.example a .env y rellena los valores
npx serve .
```

```env
EMAILJS_PUBLIC_KEY=user_xxxxxxxx
EMAILJS_SERVICE_ID=service_gmail
EMAILJS_TEMPLATE_ID=template_rcp_recovery
DATABASE_API_URL=https://api.tuempresa.com/v1
DATABASE_API_KEY=token_de_api
```

Sin `DATABASE_API_URL`, todas las operaciones se guardan en el `localStorage` del navegador. La exportación a PDF se hace con jsPDF.

### Stack tecnológico

| Capa | Tecnología |
|---|---|
| UI | JavaFX 21 + AtlantaFX (Nord Dark / Nord Light) |
| Base de datos | SQLite JDBC con pool HikariCP |
| Contraseñas | jBCrypt |
| Seguridad nativa | JNA / JNA Platform (DPAPI en Windows) |
| Logs | SLF4J + Logback con rotación |
| Iconos | Ikonli (Material Design Icons) |
| Web | HTML5, CSS3, JavaScript, EmailJS, jsPDF |

### Seguridad y límites

- **DPAPI es específico de Windows**: en otros sistemas la contraseña SMTP queda en texto plano en `config.properties`.
- **SQLite local**: no hay servidor central; la sincronización con la nube es un prototipo (interfaz `SyncProvider`) y no está conectada a un backend real.
- **OTP por consola**: el modo simulador imprime los códigos en la terminal; úsalo solo en desarrollo.
- **Prototipo web**: guarda datos en `localStorage`, sin cifrado ni autenticación de servidor. No lo uses con datos reales de producción.
- **Credenciales de ejemplo**: los valores de `.env.example` son ficticios.

### Licencia

**GNU General Public License v3.0 (GPL-3.0)** — consulta [LICENSE](LICENSE). Uso libre, incluido el comercial; las versiones modificadas deben publicarse bajo la misma licencia. © 2026 Alberto Ortiz.

---

## English

### Table of contents

- [Overview](#overview)
- [Modules](#modules)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Requirements](#requirements)
- [Build and run](#build-and-run)
- [SMTP configuration](#smtp-configuration)
- [Web prototype](#web-prototype)
- [Tech stack](#tech-stack)
- [Security and limits](#security-and-limits)
- [License](#license)

### Overview

A **Rich Client Platform (RCP)** desktop application for end-to-end company management: dashboard, customers, employees, orders, products, reports and auditing. Built with **JavaFX 21** and **SQLite**, with an adaptive interface (light and dark themes) and a **SPA web prototype** that mirrors the flow and works offline.

### Modules

| Module | Purpose |
|---|---|
| **Dashboard** | Business overview with bar, line and pie charts |
| **Customers** | Create, edit and query customers |
| **Employees** | Staff management |
| **Orders** | Order entry and tracking |
| **Products** | Item catalogue |
| **Reports** | Reports and export |
| **Settings** | Preferences, themes and credentials |
| **Authentication** | Login, sign-up and OTP password recovery |

Every module implements the `RCPModule` interface and is discovered at runtime through `ServiceLoader`, so plugins can be added or removed without touching the core.

### Architecture

**Dynamic modularity (SPI)** — `ServiceLoader` discovers classes implementing `RCPModule`. Navigation buttons, their Ikonli icons and the views register dynamically.

**Non-blocking concurrency** — queries and report generation run in `DbTask` classes extending `javafx.concurrent.Task` on daemon threads, keeping the JavaFX thread responsive.

**DPAPI-protected credentials** — on Windows, JNA calls `Crypt32.dll`: the plaintext SMTP password is encrypted with `CryptProtectData` at user level and rewritten with the `{DPAPI}` prefix. Other systems skip encryption safely.

**Offline-first** — every write is stored locally and queued in `sync_queue` (SQLite on Java, `localStorage` on the web). A background synchroniser drains the queue in FIFO order once connectivity returns.

### Project structure

```text
rcp-gestion-empresarial/
├── pom.xml                       # Maven dependencies and packaging config
├── package.bat                   # Windows native build automation
├── src/main/java/
│   ├── module-info.java          # JPMS modules
│   └── com/empresa/rcp/
│       ├── App.java              # Entry point and window state
│       ├── Main.java             # Bundler-compatible launcher
│       ├── MainController.java   # Menu, idle timeout and plugins
│       ├── chart/                # JavaFX Canvas charts
│       ├── db/DatabaseManager.java
│       ├── sync/                 # SyncManager and SyncProvider
│       ├── util/                 # DbTask, PreferencesManager, SecurityUtil
│       └── module/               # RCPModule + business modules
├── src/main/resources/
│   ├── logback.xml
│   ├── css/                      # Base, dark and light themes
│   ├── fxml/                     # Views (login, sign-up, dashboard…)
│   └── images/
├── web-app/                      # SPA prototype (HTML, CSS, JS, EmailJS)
├── LICENSE
└── README.md
```

### Requirements

- **JDK 21** or later.
- **Apache Maven 3.8** or later.
- **Windows** for DPAPI encryption; macOS and Linux run the app but without that credential protection.

### Build and run

```bash
mvn clean compile
mvn javafx:run                 # development
mvn clean package -DskipTests  # JAR + native executable
```

The executable lands in `target/RCPGestion.exe` (Windows). `package.bat` automates the native packaging.

### SMTP configuration

To send real OTP codes during password recovery, create `config.properties` at the project root:

```properties
smtp.host=smtp.gmail.com
smtp.port=587
smtp.user=your_email@gmail.com
smtp.password=your_app_password
```

On first launch the password is replaced with a `{DPAPI}…` value. If the file is missing, the app **prints OTP codes to the console** for local development.

> [!IMPORTANT]
> `config.properties` is git-ignored. Never commit it: it holds your email app password.

### Web prototype

`web-app/` is a standalone SPA mirroring the desktop flow:

```bash
cd web-app
# copy .env.example to .env and fill it in
npx serve .
```

```env
EMAILJS_PUBLIC_KEY=user_xxxxxxxx
EMAILJS_SERVICE_ID=service_gmail
EMAILJS_TEMPLATE_ID=template_rcp_recovery
DATABASE_API_URL=https://api.yourcompany.com/v1
DATABASE_API_KEY=your_api_token
```

Without `DATABASE_API_URL`, every operation is stored in browser `localStorage`. PDF export uses jsPDF.

### Tech stack

| Layer | Technology |
|---|---|
| UI | JavaFX 21 + AtlantaFX (Nord Dark / Nord Light) |
| Database | SQLite JDBC with a HikariCP pool |
| Passwords | jBCrypt |
| Native security | JNA / JNA Platform (DPAPI on Windows) |
| Logging | SLF4J + Logback with rotation |
| Icons | Ikonli (Material Design Icons) |
| Web | HTML5, CSS3, JavaScript, EmailJS, jsPDF |

### Security and limits

- **DPAPI is Windows-only**: elsewhere the SMTP password stays in plaintext in `config.properties`.
- **Local SQLite**: there is no central server; cloud sync is a prototype (`SyncProvider` interface) and is not wired to a real backend.
- **Console OTP**: the simulator prints codes to the terminal; use it for development only.
- **Web prototype**: stores data in `localStorage`, with no encryption or server-side authentication. Do not use it with real production data.
- **Sample credentials**: the `.env.example` values are fictional.

### License

**GNU General Public License v3.0 (GPL-3.0)** — see [LICENSE](LICENSE). Free use, including commercial; modified versions must be published under the same license. © 2026 Alberto Ortiz.

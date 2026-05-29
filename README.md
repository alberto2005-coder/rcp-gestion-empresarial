# 🏢 RCP Gestión Empresarial — Enterprise Architecture

[![Java Version](https://img.shields.io/badge/Java-21%2B-orange.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue.svg?style=for-the-badge&logo=oracle)](https://openjfx.io/)
[![Build](https://img.shields.io/badge/Maven-3.8%2B-red.svg?style=for-the-badge&logo=apache-maven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Custom--Non--Commercial-red.svg?style=for-the-badge)](./LICENSE)

Una plataforma de cliente rico (**Rich Client Platform - RCP**) premium para la administración empresarial (Dashboard, Clientes, Empleados, Pedidos, Auditoría de logs y Reportes). Diseñada siguiendo patrones arquitectónicos empresariales, concurrencia avanzada, modularidad dinámica y seguridad nativa a nivel del sistema operativo.

---

## 🎯 Arquitectura y Patrones de Diseño

El sistema está dividido en dos partes integradas:
1. **Cliente Natico RCP (JavaFX 21 + SQLite)**: Una aplicación robusta de escritorio orientada a alto rendimiento.
2. **Prototipo Web SPA (HTML5 + CSS3 + JS)**: Un gemelo digital modular diseñado con persistencia local y soporte híbrido de nube.

### 🧬 Principales Patrones Implementados

#### A. Modularidad Dinámica (Dynamic Plugin Framework)
Mediante la interfaz `RCPModule`, la aplicación de escritorio escanea en tiempo de ejecución las clases que implementan dicha interfaz a través de `ServiceLoader` (patrón *Service Provider Interface - SPI*).
- Permite añadir o remover módulos (plugins) de negocio compilados de forma independiente sin modificar el núcleo de la aplicación.
- Los botones de navegación lateral, sus iconos asociados (`Ikonli` / `FontIcon`) y los eventos de carga de vista se registran de forma dinámica.

#### B. Concurrencia y UI No Bloqueante (`DbTask`)
Para asegurar una experiencia de usuario fluida, las consultas complejas y la generación de reportes se ejecutan asíncronamente heredando de `javafx.concurrent.Task`.
- `DbTask` encapsula la ejecución en hilos daemon controlados.
- Previene que el hilo principal de renderizado (JavaFX Application Thread) se congele ante retrasos del disco o de red.

#### C. Seguridad de Credenciales (DPAPI Nativo de Windows)
Utiliza la interfaz JNA (Java Native Access) para invocar de forma directa la biblioteca `Crypt32.dll` del sistema operativo Windows:
- **Cifrado en primer inicio**: Al ingresar contraseñas SMTP por primera vez en `config.properties`, el sistema detecta que están en texto plano, las encripta usando la API **DPAPI** (`CryptProtectData`) a nivel de usuario del sistema operativo y reescribe el archivo con el prefijo `{DPAPI}`.
- **Acceso seguro**: En ejecuciones posteriores se desencripta en memoria activa únicamente para el envío asíncrono, protegiendo las credenciales contra accesos no autorizados al disco duro.

#### D. Sincronización Fuera de Línea (Offline-First Core)
- **Cola Transaccional**: Ambos clientes (Java y Web) poseen una cola persistente independiente (`sync_queue` en SQLite para Java; y `sync_queue` en `localStorage` para JS).
- **Productor-Consumidor**: Cada operación de escritura se almacena localmente y a su vez encola un log de sincronización.
- **Sincronizador en Segundo Plano**: Un servicio independiente monitorea el estado de la red. Al restablecerse la conexión, procesa la cola de manera secuencial (FIFO) garantizando consistencia semántica.

---

## 📁 Estructura Detallada del Proyecto

A continuación se detalla la estructura física y lógica de los archivos de código fuente:

```
rcp-java/
├── .gitignore                   # Exclusión de credenciales (config.properties, .env, target/)
├── pom.xml                      # Descriptor de dependencias Maven (HikariCP, JNA, AtlantaFX, etc.)
├── package.bat                  # Script de automatización de compilación nativa en Windows
├── README.md                    # Documentación principal de la arquitectura
│
├── src/
│   └── main/
│       ├── java/
│       │   ├── module-info.java # Definiciones de módulos de Java (JPMS), exportaciones y dependencias
│       │   └── com/empresa/rcp/
│       │       ├── App.java                 # Clase principal. Arranca y guarda estado de ventana
│       │       ├── Main.java                # Launcher compatible con empaquetadores
│       │       ├── MainController.java      # Controlador principal, maneja menú, inactividad y plugins
│       │       │
│       │       ├── chart/                   # Generación y dibujo de gráficas con Canvas de JavaFX
│       │       │   ├── BarChartCanvas.java
│       │       │   ├── LineChartCanvas.java
│       │       │   └── PieChartCanvas.java
│       │       │
│       │       ├── db/                      # Capa de Acceso a Datos
│       │       │   └── DatabaseManager.java # HikariCP pool, inicialización de SQLite y logs de auditoría
│       │       │
│       │       ├── sync/                    # Motor de sincronización
│       │       │   ├── SyncManager.java     # Ciclo periódico asíncrono de reintentos
│       │       │   └── SyncProvider.java    # Interfaz para conexión con APIs en la nube
│       │       │
│       │       ├── util/                    # Utilidades transversales
│       │       │   ├── DbTask.java          # Wrapper para hilos esclavos de base de datos
│       │       │   ├── PreferencesManager.java # Serializador ligero del espacio de trabajo
│       │       │   └── SecurityUtil.java    # Enlace nativo DPAPI mediante JNA
│       │       │
│       │       └── module/                  # Módulos de lógica empresarial de la App
│       │           ├── RCPModule.java       # Interfaz core para plugins
│       │           ├── auth/
│       │           │   ├── AuthController.java
│       │           │   └── EmailService.java# Gestión SMTP y auto-encriptación
│       │           ├── clientes/
│       │           │   └── ClientesController.java
│       │           ├── empleados/
│       │           │   └── EmpleadosController.java
│       │           ├── pedidos/
│       │           │   └── PedidosController.java
│       │           ├── configuracion/
│       │           │   └── ConfiguracionController.java
│       │           └── reportes/
│       │               └── ReportesController.java
│       │
│       └── resources/
│           ├── logback.xml              # Configuración rotativa y tamaños límite de logs
│           ├── css/                     # Hojas de estilo CSS personalizadas
│           │   ├── styles.css
│           │   ├── dark-theme.css
│           │   └── light-theme.css
│           ├── fxml/                    # Vistas estructuradas FXML para JavaFX
│           │   ├── main.fxml
│           │   ├── login.fxml
│           │   ├── registro.fxml
│           │   ├── recuperar_pass.fxml
│           │   └── ... (vistas secundarias)
│           └── images/
│               ├── logo.ico             # Icono de app moderna
│               └── logo.png             # Logo squircle de bordes redondeados
│
└── web-app/                             # Prototipo SPA Web del cliente
    ├── index.html                       # Estructura visual de la SPA
    ├── styles.css                       # Estilización premium (Glassmorphism, variables CSS)
    ├── app.js                           # Control lógico, EmailJS, syncQueue y tema persistente
    ├── logo.png                         # Logotipo squircle adaptado
    └── .env.example                     # Variables de entorno de EmailJS y API Cloud
```

---

## 🚀 Guía de Instalación y Ejecución

### Requisitos Técnicos
- **Java Development Kit (JDK) 21** o superior.
- **Apache Maven 3.8** o superior.
- Sistema Operativo Windows (necesario para el cifrado DPAPI nativo; en otros sistemas operativos el módulo omite el cifrado de forma segura).

### 1. Construcción de la Aplicación de Escritorio
En tu terminal de desarrollo ejecuta:
```bash
# Compilar clases y descargar dependencias
mvn clean compile

# Iniciar la aplicación JavaFX en modo de desarrollo
mvn javafx:run
```

Para generar el empaquetado final (`.jar` unificado y ejecutable `.exe` nativo):
```bash
# Empaquetar todo el proyecto
mvn clean package -DskipTests
```
- El ejecutable independiente con soporte de icono nativo y metadatos del sistema se creará en [target/RCPGestion.exe](./target/RCPGestion.exe).

---

## ⚙️ Configuración de Credenciales y Entorno

### A. Correo Electrónico y SMTP (Aplicación Java)
Para habilitar el envío real de códigos de verificación OTP durante la recuperación de contraseñas, crea el archivo `config.properties` en la raíz del proyecto:
```properties
smtp.host=smtp.gmail.com
smtp.port=587
smtp.user=tu_correo_corporativo@gmail.com
# Al arrancar, el texto plano se reemplazará automáticamente por una versión cifrada {DPAPI}xxxx
smtp.password=tu_contrase単a_de_aplicacion_google
```
> [!TIP]
> **Modo Simulador de Consola**: Si no creas este archivo, la aplicación imprime directamente los códigos OTP por consola para facilitar el desarrollo local ágil.

### B. Aplicación Web (EmailJS y API REST)
Para configurar el prototipo web, renombra el archivo [web-app/.env.example](./web-app/.env.example) a `web-app/.env` y configúralo:
```env
# Conexión SMTP dinámica vía cliente EmailJS
EMAILJS_PUBLIC_KEY=user_abcdefg1234567890
EMAILJS_SERVICE_ID=service_gmail
EMAILJS_TEMPLATE_ID=template_rcp_recovery

# API REST Remota para almacenamiento centralizado en la nube (Opcional)
DATABASE_API_URL=https://api.tuempresa.com/v1
DATABASE_API_KEY=rcp_api_token_secure_123456789
```
> [!IMPORTANT]
> **Autonomía Offline**: Si no configuras una base de datos REST externa, la SPA redirigirá automáticamente todas las transacciones a su motor persistente en el **LocalStorage del navegador**.

---

## 🛠️ Stack Tecnológico Utilizado

- **JavaFX 21**: Framework base de UI de escritorio.
- **AtlantaFX (Nord Dark / Nord Light)**: Hoja de estilos moderna y elegante inspirada en sistemas operativos modernos.
- **SQLite JDBC**: Motor de base de datos relacional ligero, embebido y de alta velocidad.
- **HikariCP**: Pool de conexiones de alto rendimiento para bases de datos SQLite.
- **jBCrypt**: Algoritmo seguro de hashing para almacenamiento de contraseñas de usuario.
- **JNA & JNA Platform**: Enlace nativo a librerías y APIs de Windows.
- **SLF4J + Logback**: Logger e histórico de rotaciones del sistema.
- **Ikonli (Material Design Icons)**: Paquete de iconografía vectorial premium.
- **jsPDF**: Motor de exportación PDF en el cliente web.

---

## 📝 Licencia

Este proyecto está bajo la **Licencia Personalizada de Alberto Ortiz**.
Consulta el archivo [LICENSE](./LICENSE) para más detalles.

**Resumen:**
- ✅ Uso no comercial permitido
- ❌ No se permite comercialización sin autorización
- ⚠️ Los forks deben mantener atribución visible
- 📌 Debe incluir marca de agua del original

# RCP Gestión Empresarial (Enterprise Architecture)

**Rich Client Platform** (RCP) premium e integral para la administración empresarial: gestión de Empleados, Clientes, Pedidos, Auditoría y Reportes. Diseñada con patrones avanzados de arquitectura empresarial.

---

## 🎯 Características Enterprise

### 1. Sistema de Plugins Dinámicos (Modularidad RCP)
- **Carga Dinámica**: Soporte para la interfaz `RCPModule`. La aplicación escanea e inicializa de forma dinámica nuevos módulos/plugins de negocio utilizando `ServiceLoader` en tiempo de ejecución.
- **Sidebar Dinámico**: Los módulos encontrados se acoplan automáticamente en el panel de navegación lateral.

### 2. Gestión Asíncrona Resiliente (`DbTask`)
- **UI Fluida**: Ejecución asíncrona mediante `DbTask` (extiende `javafx.concurrent.Task`) para realizar operaciones de bases de datos pesadas (reportes, KPIs) en segundo plano, evitando que la interfaz gráfica de usuario (JavaFX Application Thread) se congele.

### 3. Cifrado Nativo de Credenciales (DPAPI Windows)
- **Protección a Nivel SO**: Integración con la API DPAPI nativa de Windows (`CryptProtectData`/`CryptUnprotectData`) mediante JNA (Java Native Access).
- **Autocifrado**: Al definir la contraseña SMTP de recuperación de contraseña en texto plano en `config.properties`, la aplicación la encripta en el primer inicio de forma segura bajo el prefijo `{DPAPI}` y la lee transparentemente en adelante, asegurando que las credenciales locales nunca se expongan en texto plano.

### 4. Sincronización Fuera de Línea (Offline-First Engine)
- **Cola de Sincronización Local**: Almacenamiento de transacciones modificadas localmente en la tabla SQLite `sync_queue` (Java) y en la cola `sync_queue` del `localStorage` (Web App).
- **Ciclo de Envío**: `SyncManager` y `syncQueue` (JS) monitorean la conectividad a Internet. Al detectar conexión en línea, las transacciones pendientes se empujan en lote de forma secuencial hacia el servidor REST remoto.

### 5. Persistencia del Espacio de Trabajo (Workspace State)
- **Restauración de Ventana (Java)**: Guarda y restaura de forma automática el tamaño de la ventana principal, posición (X, Y) y si estaba o no maximizada.
- **Persistencia de Navegación y Tema (Web App)**: Guarda en `localStorage` el tema visual activo (oscuro/claro) y la última vista/pestaña activa en la que trabajó el usuario, restaurándolos al iniciar sesión.

### 6. Logging Estructurado (SLF4J + Logback)
- **Trazabilidad Profesional**: logs detallados con rotación de archivos diaria y límite de tamaño, escritos en `${user.home}/.rcpgestion/logs/app.log` y por consola.

---

## 📋 Requisitos Previos

- **Java 21+** instalado
- **Maven 3.8+** instalado
- **Git**

---

## 🚀 Instalación y Ejecución

### 1. Clonar el proyecto e instalar dependencias
```bash
git clone https://github.com/alberto2005-coder/rcp-gestion-empresarial.git
cd rcp-gestion-empresarial
mvn clean install
```

### 2. Ejecutar en modo desarrollo
```bash
mvn javafx:run
```

### 3. Compilar ejecutable fat JAR y .exe (Windows)
```bash
mvn clean package -DskipTests
```
El instalador wrapped de Windows se generará en `target/RCPGestion.exe` utilizando Launch4j.

---

## 🔧 Configuración del Entorno

### Configuración SMTP (Recuperación de Contraseñas por Correo)
Crea un archivo llamado `config.properties` en la carpeta raíz del proyecto:
```properties
smtp.host=smtp.gmail.com
smtp.port=587
smtp.user=tu_correo@gmail.com
smtp.password=tu_contraseña_plana (se cifrará automáticamente como {DPAPI}xxxx en el primer inicio)
```
*Nota:* Si el archivo no existe, el sistema operará en **Modo Local de Pruebas**, imprimiendo el código aleatorio de 6 dígitos en la terminal.

### Configuración del Prototipo Web (`web-app`)
Crea un archivo `.env` dentro de `web-app/` basándote en [web-app/.env.example](file:///c:/Users/alors/Downloads/rcp-java/web-app/.env.example):
```env
EMAILJS_PUBLIC_KEY=user_abcdefg1234567890
EMAILJS_SERVICE_ID=service_gmail
EMAILJS_TEMPLATE_ID=template_rcp_recovery

DATABASE_API_URL=https://api.tuempresa.com/v1
DATABASE_API_KEY=rcp_api_token_secure_123456789
```

---

## 📁 Estructura Principal del Proyecto

```
rcp-java/
├── src/main/java/com/empresa/rcp/
│   ├── App.java                 # Punto de entrada y persistencia de posición
│   ├── MainController.java      # Controlador principal y cargador ServiceLoader de plugins
│   ├── db/
│   │   └── DatabaseManager.java # SQLite y auditoría
│   ├── module/
│   │   ├── RCPModule.java       # Interfaz de módulo dinámico
│   │   └── auth/
│   │       ├── AuthController.java
│   │       └── EmailService.java# Envío de código e integración DPAPI
│   ├── sync/
│   │   ├── SyncManager.java     # Manejador del ciclo de sincronización offline-first
│   │   └── SyncProvider.java    # Interfaz para REST API remota
│   └── util/
│       ├── DbTask.java          # Tarea asíncrona genérica
│       ├── PreferencesManager.java # Serializador de coordenadas y tamaño
│       └── SecurityUtil.java    # Cifrado DPAPI nativo (JNA)
├── src/main/resources/
│   └── logback.xml              # Configuración de logs rotativos
├── web-app/
│   ├── app.js                   # SPA con cola de sincronización offline-first y tema persistente
│   └── .env.example             # Ejemplo detallado de entorno web
└── pom.xml                      # Descriptores de dependencias Maven
```

---

## 📝 Licencia
Este proyecto está bajo la licencia **MIT**.

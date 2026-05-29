# RCP Gestión Empresarial

**Rich Client Platform** (RCP) para gestión integral de empresas: Empleados, Clientes, Pedidos y más.

## 🎯 Características

✅ **5 Módulos principales:**
- 📊 **Dashboard** - Métricas clave, gráficos y KPIs
- 👥 **Clientes** - Gestión completa (CRUD) de clientes
- 👨‍💼 **Empleados** - Gestión de empleados y nómina
- 📦 **Pedidos** - Control de pedidos y estado
- ⚙️ **Configuración** - Ajustes de la aplicación

✅ **Tecnología moderna:**
- JavaFX 21 (interfaz moderna)
- SQLite (base de datos local)
- Tema automático (oscuro/claro según SO)
- Iconografía con emojis
- Animaciones fluidas

## 📋 Requisitos Previos

- **Java 21+** instalado
- **Maven 3.8+** instalado
- **Git** (opcional)

### Verificar instalación:
```bash
java -version
mvn -v
```

## 🚀 Instalación y Ejecución

### 1. Clonar o descargar el proyecto

```bash
git clone <URL_DEL_REPO>
cd rcp-java-complete
```

### 2. Compilar el proyecto

```bash
mvn clean compile
```

### 3. Ejecutar en desarrollo

```bash
mvn javafx:run
```

### 4. Crear JAR ejecutable

```bash
mvn clean package
```

El JAR se generará en `target/rcp-gestion-1.0.0.jar`

### 5. Ejecutar el JAR

```bash
java -jar target/rcp-gestion-1.0.0.jar
```

## 📦 Empaquetar como EXE (Windows)

### Requisitos adicionales:
- **WiX Toolset 3.11+** instalado en Windows
- **JDK 21+** (incluye jpackage)

### Pasos:

1. **Compilar con Maven:**
```bash
mvn clean package
```

2. **Ejecutar el script de empaquetado:**
```bash
# En Windows:
.\package.bat

# O manualmente con jpackage:
jpackage --input target --name RCPGestion --main-jar rcp-gestion-1.0.0-all.jar --main-class com.gestionsistema.rcp.App --type exe --win-console --win-menu --win-menu-group "Aplicaciones"
```

3. El `.exe` se generará en el directorio actual.

## 📁 Estructura del Proyecto

```
rcp-java-complete/
├── src/
│   └── main/
│       ├── java/com/gestionsistema/rcp/
│       │   ├── App.java                        # Punto de entrada
│       │   ├── MainController.java             # Controlador principal
│       │   ├── module/
│       │   │   ├── dashboard/
│       │   │   ├── clientes/
│       │   │   ├── empleados/
│       │   │   ├── pedidos/
│       │   │   └── configuracion/
│       │   └── util/
│       │       ├── DatabaseManager.java        # Gestión de BD
│       │       └── ThemeManager.java           # Temas (claro/oscuro)
│       └── resources/
│           ├── fxml/
│           │   ├── main.fxml
│           │   ├── dashboard.fxml
│           │   ├── clientes.fxml
│           │   ├── empleados.fxml
│           │   ├── pedidos.fxml
│           │   └── configuracion.fxml
│           ├── css/
│           │   ├── styles.css
│           │   ├── dark-theme.css
│           │   └── light-theme.css
│           └── images/
│               └── logo.png
├── pom.xml                                     # Configuración Maven
├── package.bat                                 # Script empaquetado
└── README.md
```

## 🔧 Configuración

### Recuperación de Contraseña por Correo (SMTP)

Para poder enviar correos reales al recuperar contraseñas:

1. Crea un archivo llamado `config.properties` en la carpeta raíz del proyecto (este archivo está excluido en el `.gitignore` para proteger tus datos).
2. Rellena el archivo con la configuración de tu servidor de correo (ejemplo usando Gmail):
   ```properties
   smtp.host=smtp.gmail.com
   smtp.port=587
   smtp.user=tu_correo@gmail.com
   smtp.password=tu_contraseña_de_aplicacion_google
   ```
3. Alternativamente, puedes configurar las variables de entorno:
   - `SMTP_HOST`
   - `SMTP_PORT`
   - `SMTP_USER`
   - `SMTP_PASSWORD`

> [!TIP]
> **Modo Local de Pruebas (Sin Configurar):**
> Si el archivo `config.properties` no existe y tampoco hay variables de entorno, la aplicación entrará automáticamente en modo local de pruebas. El código aleatorio de recuperación de 6 dígitos se generará y se **imprimirá en la consola** (terminal de desarrollo), permitiéndote continuar las pruebas locales sin configurar credenciales de correo reales.

### Aplicación Web: Envío de Correo (EmailJS)

Para que el prototipo web (`web-app`) envíe correos electrónicos reales:

1. Crea una cuenta gratuita en [EmailJS](https://www.emailjs.com/).
2. Crea un archivo llamado `.env` dentro de la carpeta `web-app/` basándote en el archivo de plantilla `web-app/.env.example`.
3. Rellena las claves con tus credenciales de EmailJS:
   ```env
   EMAILJS_PUBLIC_KEY=tu_public_key
   EMAILJS_SERVICE_ID=tu_service_id
   EMAILJS_TEMPLATE_ID=tu_template_id
   ```
4. Configura tu plantilla de EmailJS para que reciba las siguientes variables:
   - `{{to_name}}`: Nombre del destinatario.
   - `{{to_email}}`: Correo electrónico del destinatario.
   - `{{message}}`: Código de verificación aleatorio generado.

*Nota:* Si no creas el archivo `.env`, la aplicación web funcionará en **modo simulador local**, mostrando el código de verificación directamente en una alerta (`alert()`) del navegador para facilitar las pruebas.

### Aplicación Web: Base de Datos (Local, Nube o LocalStorage)

Para sincronizar los datos del prototipo web con una base de datos externa (ya sea local o alojada en la nube mediante una API REST):

1. En el archivo `.env` de tu carpeta `web-app/`, configura las siguientes propiedades:
   ```env
   DATABASE_API_URL=https://api.tu-servidor.com/v1
   DATABASE_API_KEY=tu_token_de_autorizacion_jwt_u_otro
   ```
2. La aplicación web sincronizará automáticamente los datos en segundo plano mediante peticiones HTTP `GET` y `POST` para las tablas principales (`usuarios`, `empleados`, `clientes`, `pedidos`, `logs`).

> [!IMPORTANT]
> **Persistencia Local (Fallback):**
> Si estas propiedades no están configuradas en el archivo `.env`, la aplicación utilizará automáticamente el **LocalStorage del navegador** para guardar toda la información de forma local y 100% persistente en el dispositivo del usuario, sin necesidad de configurar ningún servidor externo.

### Base de Datos (SQLite - Aplicación Java)

La base de datos se crea automáticamente en `rcp_gestion.db` en el directorio raíz de la aplicación Java.

**Tablas creadas:**
- `empleados`
- `clientes`
- `pedidos`
- `productos`
- `detalle_pedidos`

### Tema Automático

El tema se detecta automáticamente según el sistema operativo:
- **Windows:** Detecta tema de Configuración de Windows
- **macOS:** Detecta desde preferencias del sistema
- **Linux:** Por defecto oscuro
- **Manual:** Botón "🌙 Cambiar Tema" en la barra superior

## 📊 Módulos

### 1️⃣ Dashboard
- 4 KPIs principales
- Gráfico de barras (ventas mensuales)
- Gráfico de líneas (tendencia de clientes)
- Gráfico circular (estado de pedidos)

### 2️⃣ Clientes
- Tabla de clientes con búsqueda
- Formulario para crear/editar
- Validación de datos
- Eliminación lógica

### 3️⃣ Empleados
- Gestión completa de empleados
- Campos: Nombre, email, teléfono, puesto, salario
- Búsqueda y filtrado
- CRUD funcional

### 4️⃣ Pedidos
- Pedidos vinculados a clientes
- Estados: Pendiente, Procesando, Completado, Cancelado
- Cálculo automático de totales
- Historial de cambios

### 5️⃣ Configuración
- Datos de la empresa
- Selección de tema
- Información de versión
- Restaurar valores por defecto

## 🎨 Personalización

### Cambiar colores (Dark Theme)

Edita `src/main/resources/css/dark-theme.css`:

```css
.root {
    -fx-background-color: #1e1e1e;      /* Cambia color de fondo */
    -fx-text-fill: #e0e0e0;              /* Cambia color de texto */
}

.button {
    -fx-background-color: #0078d4;       /* Cambia color de botones */
}
```

### Agregar un nuevo módulo

1. **Crear controlador:**
   ```java
   package com.gestionsistema.rcp.module.nuevo;
   public class NuevoController { ... }
   ```

2. **Crear FXML:**
   ```xml
   src/main/resources/fxml/nuevo.fxml
   ```

3. **Agregar botón en MainController:**
   ```java
   @FXML private Button btnNuevo;
   btnNuevo.setOnAction(e -> loadModule("/fxml/nuevo.fxml"));
   ```

## 🐛 Solución de Problemas

### Error: "Module not found: javafx"
- Asegúrate de tener JavaFX en Maven
- Ejecuta: `mvn clean install`

### La BD no se crea
- Verifica permisos de carpeta
- Elimina `rcp_gestion.db` y reinicia

### Tema no cambia
- Limpia caché de JavaFX: Elimina el JAR y vuelve a compilar

## 📝 Dependencias Principales

```xml
- javafx-controls 21.0.2
- javafx-fxml 21.0.2
- atlantafx-base 2.0.1 (Temas)
- sqlite-jdbc 3.44.2.1
- jfreechart 1.5.3 (Gráficos)
- gson 2.10.1 (JSON)
- slf4j 2.0.11 (Logging)
```

## 🚀 Próximas Mejoras

- [ ] Exportación a PDF/Excel
- [ ] Reportes avanzados
- [ ] Sincronización con base de datos remota
- [ ] Sistema de usuarios y permisos
- [ ] Backup automático
- [ ] Búsqueda avanzada
- [ ] Importación de datos

## 📞 Soporte

Para reportar bugs o solicitar features, crea un issue en el repositorio.

## 📄 Licencia

Este proyecto está bajo licencia **MIT**.

---

**Versión:** 1.0.0  
**Última actualización:** 2026  
**Autor:** Generado con JavaFX + Maven

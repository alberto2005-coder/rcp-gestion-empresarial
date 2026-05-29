// --- GESTIÓN DE BASE DE DATOS LOCAL (LOCALSTORAGE MOCK) ---
const mockData = {
    usuarios: [],
    empleados: [],
    clientes: [],
    pedidos: [],
    logs: []
};

// Inicializar base de datos en localStorage si no existe
function initDB() {
    for (let key in mockData) {
        if (!localStorage.getItem(key)) {
            localStorage.setItem(key, JSON.stringify(mockData[key]));
        }
    }
}
initDB();

// Helpers para consultar/guardar tablas
const db = {
    get: (table) => JSON.parse(localStorage.getItem(table)) || [],
    save: (table, data) => {
        localStorage.setItem(table, JSON.stringify(data));
        // Sincronizar en segundo plano con la base de datos externa si existe
        const apiUrl = window.app && window.app.env && window.app.env.DATABASE_API_URL;
        if (apiUrl) {
            fetch(`${apiUrl}/${table}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${window.app.env.DATABASE_API_KEY || ''}`
                },
                body: JSON.stringify(data)
            }).catch(e => console.error(`[Base de Datos] Error al sincronizar ${table}:`, e));
        }
    },
    log: (usuario, accion) => {
        const logs = db.get("logs");
        const nextId = logs.length > 0 ? Math.max(...logs.map(l => l.id)) + 1 : 1;
        logs.unshift({ id: nextId, usuario: usuario || "Sistema/Anónimo", accion: accion, fecha: new Date().toLocaleString() });
        db.save("logs", logs);
    }
};

// --- CONTROLADOR GENERAL DE LA APLICACIÓN (SPA) ---
class AppController {
    constructor() {
        this.session = null;
        this.inactivityTimer = null;
        this.inactivityLimit = 5 * 60 * 1000; // 5 Minutos (300,000 ms)
        
        // Historial de ventas de prueba para las gráficas
        this.ventasMensuales = {
            "Ene": 12000, "Feb": 15000, "Mar": 18000, "Abr": 14000, "May": 23000, "Jun": 29000
        };

        this.initEvents();
        this.loadEnv();
    }

    async loadEnv() {
        this.env = {
            EMAILJS_PUBLIC_KEY: "",
            EMAILJS_SERVICE_ID: "",
            EMAILJS_TEMPLATE_ID: "",
            DATABASE_API_URL: "",
            DATABASE_API_KEY: ""
        };
        try {
            const res = await fetch('.env');
            if (res.ok) {
                const text = await res.text();
                text.split('\n').forEach(line => {
                    const parts = line.split('=');
                    if (parts.length >= 2) {
                        const key = parts[0].trim();
                        const val = parts.slice(1).join('=').trim().replace(/^["']|["']$/g, '');
                        if (key && !key.startsWith('#')) {
                            this.env[key] = val;
                        }
                    }
                });
                
                // Inicializar EmailJS
                if (this.env.EMAILJS_PUBLIC_KEY && window.emailjs) {
                    window.emailjs.init({
                        publicKey: this.env.EMAILJS_PUBLIC_KEY
                    });
                    console.log("[EmailJS] Inicializado correctamente con clave pública.");
                }

                // Sincronizar datos iniciales desde base de datos externa si está configurada
                if (this.env.DATABASE_API_URL) {
                    this.syncFromDatabase();
                }
            }
        } catch (e) {
            console.warn("No se pudo cargar el archivo .env para EmailJS: " + e.getMessage());
        }
    }

    async syncFromDatabase() {
        const tables = ["usuarios", "empleados", "clientes", "pedidos", "logs"];
        console.log("[Base de Datos] Sincronizando datos desde: " + this.env.DATABASE_API_URL);
        for (const table of tables) {
            try {
                const res = await fetch(`${this.env.DATABASE_API_URL}/${table}`, {
                    headers: { 'Authorization': `Bearer ${this.env.DATABASE_API_KEY || ''}` }
                });
                if (res.ok) {
                    const data = await res.json();
                    localStorage.setItem(table, JSON.stringify(data));
                    console.log(`[Base de Datos] Tabla '${table}' sincronizada con éxito.`);
                }
            } catch(e) {
                console.warn(`[Base de Datos] No se pudo sincronizar la tabla ${table} desde la nube:`, e);
            }
        }
    }

    // Inicializar listeners globales de inactividad
    initEvents() {
        const resetTimer = () => this.resetInactivityTimer();
        window.addEventListener('mousemove', resetTimer);
        window.addEventListener('keypress', resetTimer);
        window.addEventListener('click', resetTimer);
    }

    // Cambiar vistas de autenticación
    showAuthView(viewName) {
        document.querySelectorAll('.auth-view').forEach(v => v.classList.remove('active'));
        document.getElementById(`${viewName}-form-view`).classList.add('active');
        
        const subtitle = document.getElementById("auth-subtitle");
        if (viewName === 'login') {
            subtitle.innerText = "Inicia sesión para acceder al sistema";
        } else if (viewName === 'register') {
            subtitle.innerText = "Crea un nuevo perfil de usuario";
        } else if (viewName === 'recover') {
            subtitle.innerText = "Recupera tu contraseña mediante un código de verificación por correo";
            document.getElementById("recover-step-1").style.display = "block";
            document.getElementById("recover-step-2").style.display = "none";
            document.getElementById("rec-username").value = "";
            document.getElementById("rec-answer").value = "";
            document.getElementById("rec-new-password").value = "";
        }
        
        // Limpiar errores
        document.querySelectorAll('.error-msg, .success-msg').forEach(el => el.innerText = "");
    }

    // --- AUTENTICACIÓN Y ROLES ---
    login() {
        const userVal = document.getElementById("login-username").value.trim();
        const passVal = document.getElementById("login-password").value.trim();
        const errorEl = document.getElementById("login-error");

        if (!userVal || !passVal) {
            errorEl.innerText = "Por favor, completa todos los campos.";
            return;
        }

        const usuarios = db.get("usuarios");
        const found = usuarios.find(u => u.username.toLowerCase() === userVal.toLowerCase());

        if (found) {
            if (found.passwordHash === passVal) {
                // Login exitoso
                this.session = { username: found.username, rol: found.rol };
                db.log(found.username, "Inicio de sesión exitoso");
                this.startSession();
            } else {
                db.log(userVal, "Intento fallido de inicio de sesión: contraseña incorrecta");
                errorEl.innerText = "Contraseña incorrecta.";
            }
        } else {
            db.log(userVal, "Intento fallido de inicio de sesión: usuario inexistente");
            errorEl.innerText = "El usuario no existe.";
        }
    }

    register() {
        const userVal = document.getElementById("reg-username").value.trim();
        const emailVal = document.getElementById("reg-email").value.trim();
        const passVal = document.getElementById("reg-password").value.trim();
        const roleVal = document.getElementById("reg-role").value;

        const errorEl = document.getElementById("reg-error");
        const successEl = document.getElementById("reg-success");

        if (!userVal || !emailVal || !passVal) {
            errorEl.innerText = "Por favor, completa todos los campos.";
            successEl.innerText = "";
            return;
        }

        if (passVal.length < 6) {
            errorEl.innerText = "La contraseña debe tener al menos 6 caracteres.";
            successEl.innerText = "";
            return;
        }

        const usuarios = db.get("usuarios");
        const empleados = db.get("empleados");

        // Validar si el usuario o email ya existe en cuentas
        if (usuarios.some(u => u.username.toLowerCase() === userVal.toLowerCase() || u.email.toLowerCase() === emailVal.toLowerCase())) {
            errorEl.innerText = "El nombre de usuario o correo electrónico ya está registrado.";
            successEl.innerText = "";
            return;
        }

        // Buscar si existe en la lista de empleados (vinculación)
        const empIndex = empleados.findIndex(e => e.nombre.toLowerCase() === userVal.toLowerCase() || e.email.toLowerCase() === emailVal.toLowerCase());
        
        let finalRol = "Empleado";
        let crearNuevoEmpleado = false;

        if (empIndex !== -1) {
            // Existe empleado: Vincular heredando el rol
            const empPuesto = empleados[empIndex].puesto;
            finalRol = (empPuesto && (empPuesto.toLowerCase() === "administrador" || empPuesto.toLowerCase().includes("admin"))) 
                       ? "Administrador" 
                       : "Empleado";
        } else {
            // No existe empleado pre-creado
            if (roleVal === "Administrador") {
                errorEl.innerText = "No puedes registrarte como Administrador sin pre-registro.";
                successEl.innerText = "";
                return;
            }
            finalRol = "Empleado";
            crearNuevoEmpleado = true;
        }

        // Registrar usuario
        const nextUserId = usuarios.length > 0 ? Math.max(...usuarios.map(u => u.id)) + 1 : 1;
        usuarios.push({
            id: nextUserId,
            username: userVal,
            passwordHash: passVal,
            email: emailVal,
            rol: finalRol
        });
        db.save("usuarios", usuarios);

        // Crear perfil empleado si es de cero
        if (crearNuevoEmpleado) {
            const nextEmpId = empleados.length > 0 ? Math.max(...empleados.map(e => e.id)) + 1 : 1;
            empleados.push({
                id: nextEmpId,
                nombre: userVal,
                apellido: "Usuario",
                email: emailVal,
                telefono: "",
                puesto: finalRol,
                salario: 0.00
            });
            db.save("empleados", empleados);
        }

        db.log(userVal, `Registro de usuario exitoso (Rol: ${finalRol})`);

        errorEl.innerText = "";
        successEl.innerText = "¡Cuenta creada y vinculada correctamente!";
        
        // Limpiar campos
        document.getElementById("reg-username").value = "";
        document.getElementById("reg-email").value = "";
        document.getElementById("reg-password").value = "";
    }

    loadRecoveryQuestion() {
        const userVal = document.getElementById("rec-username").value.trim();
        const errorEl = document.getElementById("rec-error");
        
        if (!userVal) {
            errorEl.innerText = "Ingresa tu nombre de usuario.";
            return;
        }

        const usuarios = db.get("usuarios");
        const found = usuarios.find(u => u.username.toLowerCase() === userVal.toLowerCase());

        if (found) {
            // Generar código aleatorio
            const code = Math.floor(100000 + Math.random() * 900000).toString();
            this.recoveryCode = code;
            this.recoveryUser = found.username;

            document.getElementById("rec-question-label").innerText = `Código enviado al correo: ${found.email}`;
            document.getElementById("recover-step-1").style.display = "none";
            document.getElementById("recover-step-2").style.display = "block";
            errorEl.innerText = "";

            // Si EmailJS está configurado, mandar correo real
            if (this.env && this.env.EMAILJS_SERVICE_ID && this.env.EMAILJS_TEMPLATE_ID && window.emailjs) {
                const templateParams = {
                    to_email: found.email,
                    to_name: found.username,
                    message: code
                };
                window.emailjs.send(this.env.EMAILJS_SERVICE_ID, this.env.EMAILJS_TEMPLATE_ID, templateParams)
                    .then(() => {
                        console.log("[EmailJS] Correo enviado correctamente a: " + found.email);
                    })
                    .catch((err) => {
                        console.error("[EmailJS] Error al enviar correo por EmailJS: ", err);
                        alert(`[Error EmailJS] No se pudo enviar el correo real.\n\nCódigo de recuperación local: ${code}`);
                    });
            } else {
                // Modo simulador local
                alert(`[MOCK EMAIL] Código de verificación enviado a ${found.email}:\n\nCÓDIGO: ${code}\n\n(Crea un archivo .env en la carpeta web-app para enviar correos reales con EmailJS)`);
            }
        } else {
            errorEl.innerText = "El usuario no existe.";
        }
    }

    resetPassword() {
        const userVal = this.recoveryUser;
        const codeVal = document.getElementById("rec-answer").value.trim();
        const passVal = document.getElementById("rec-new-password").value.trim();
        
        const errorEl = document.getElementById("rec-error");
        const successEl = document.getElementById("rec-success");

        if (!codeVal || !passVal) {
            errorEl.innerText = "Completa el código y la nueva contraseña.";
            successEl.innerText = "";
            return;
        }

        if (passVal.length < 6) {
            errorEl.innerText = "La contraseña debe tener al menos 6 caracteres.";
            successEl.innerText = "";
            return;
        }

        const usuarios = db.get("usuarios");
        const index = usuarios.findIndex(u => u.username.toLowerCase() === userVal.toLowerCase());

        if (index !== -1) {
            if (this.recoveryCode === codeVal) {
                usuarios[index].passwordHash = passVal;
                db.save("usuarios", usuarios);
                db.log(userVal, "Restableció su contraseña con éxito mediante código");
                
                errorEl.innerText = "";
                successEl.innerText = "¡Contraseña actualizada con éxito!";
                document.getElementById("rec-answer").value = "";
                document.getElementById("rec-new-password").value = "";
                this.recoveryCode = null;
                this.recoveryUser = null;
            } else {
                db.log(userVal, "Fallo al restablecer contraseña: código incorrecto");
                errorEl.innerText = "Código de verificación incorrecto.";
                successEl.innerText = "";
            }
        }
    }

    // --- INACTIVIDAD ---
    resetInactivityTimer() {
        if (!this.session) return;
        clearTimeout(this.inactivityTimer);
        this.inactivityTimer = setTimeout(() => this.logoutByInactivity(), this.inactivityLimit);
    }

    logoutByInactivity() {
        db.log(this.session?.username, "Sesión cerrada automáticamente por inactividad");
        this.logout(true);
    }

    // --- CONTROL DE SESIÓN ---
    startSession() {
        document.getElementById("auth-container").style.display = "none";
        document.getElementById("app-container").style.display = "grid";
        
        document.getElementById("session-username").innerText = this.session.username;
        document.getElementById("session-role").innerText = this.session.rol;
        
        // Bloquear configuración si no es admin
        const btnConfig = document.getElementById("btn-menu-config");
        if (btnConfig) {
            btnConfig.disabled = this.session.rol !== "Administrador";
            if (this.session.rol !== "Administrador") {
                btnConfig.style.opacity = "0.5";
                btnConfig.style.cursor = "not-allowed";
            } else {
                btnConfig.style.opacity = "1";
                btnConfig.style.cursor = "pointer";
            }
        }

        // Cargar vista por defecto
        this.navigateTo("dashboard");
        this.resetInactivityTimer();
    }

    logout(expired = false) {
        clearTimeout(this.inactivityTimer);
        this.session = null;
        
        document.getElementById("app-container").style.display = "none";
        document.getElementById("auth-container").style.display = "flex";
        this.showAuthView("login");
        
        // Limpiar inputs de login
        document.getElementById("login-username").value = "";
        document.getElementById("login-password").value = "";

        if (expired) {
            alert("⚠️ Tu sesión ha expirado por inactividad. Por favor, inicia sesión nuevamente.");
        }
    }

    // --- TEMAS ---
    toggleTheme() {
        const body = document.body;
        const icon = document.getElementById("theme-btn-icon");
        const text = document.getElementById("theme-btn-text");

        if (body.classList.contains("dark-mode")) {
            body.classList.remove("dark-mode");
            body.classList.add("light-mode");
            icon.innerText = "dark_mode";
            text.innerText = "Modo Oscuro";
            db.log(this.session?.username, "Cambió tema visual a Claro");
        } else {
            body.classList.remove("light-mode");
            body.classList.add("dark-mode");
            icon.innerText = "light_mode";
            text.innerText = "Modo Claro";
            db.log(this.session?.username, "Cambió tema visual a Oscuro");
        }
        
        // Redibujar gráficos para adaptar colores
        this.renderCharts();
        this.renderReportCharts();
    }

    // --- NAVEGACIÓN ENTRE VISTAS (SPA) ---
    navigateTo(viewName) {
        document.querySelectorAll('.app-view').forEach(v => v.classList.remove('active'));
        document.querySelectorAll('.menu-item').forEach(btn => btn.classList.remove('active'));
        
        document.getElementById(`view-${viewName}`).classList.add('active');
        
        // Colorear botón activo en sidebar
        const activeBtn = Array.from(document.querySelectorAll('.menu-item')).find(btn => btn.innerText.toLowerCase().includes(viewName));
        if (activeBtn) activeBtn.classList.add('active');

        // Cargar datos correspondientes
        if (viewName === 'dashboard') {
            this.loadDashboard();
        } else if (viewName === 'clientes') {
            this.loadCRUD('clientes');
        } else if (viewName === 'empleados') {
            this.loadCRUD('empleados');
        } else if (viewName === 'pedidos') {
            this.loadCRUD('pedidos');
        } else if (viewName === 'reportes') {
            this.loadReportes();
        } else if (viewName === 'configuracion') {
            this.loadConfiguracion();
        }
    }

    // --- 1. MÓDULO: DASHBOARD ---
    loadDashboard() {
        const clientes = db.get("clientes").filter(c => c.estado === 'Activo').length;
        const pedidos = db.get("pedidos").filter(p => p.estado === 'Pendiente').length;
        const ventas = db.get("pedidos").reduce((acc, p) => acc + p.total, 0);
        const empleados = db.get("empleados").length;

        // Animación progresiva (Count-Up)
        this.animateKPI("kpi-empleados", empleados, false);
        this.animateKPI("kpi-clientes", clientes, false);
        this.animateKPI("kpi-pedidos", pedidos, false);
        this.animateKPI("kpi-ventas", ventas, true);

        // Renderizar gráficos del Dashboard
        this.renderCharts();
    }

    animateKPI(elementId, targetValue, isCurrency) {
        const el = document.getElementById(elementId);
        if (!el) return;

        let start = 0;
        let duration = 800; // 0.8 segundos
        let startTime = null;

        const step = (timestamp) => {
            if (!startTime) startTime = timestamp;
            const progress = Math.min((timestamp - startTime) / duration, 1);
            const current = progress * targetValue;
            
            if (isCurrency) {
                el.innerText = `$${current.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
            } else {
                el.innerText = Math.round(current).toString();
            }

            if (progress < 1) {
                window.requestAnimationFrame(step);
            }
        };
        window.requestAnimationFrame(step);
    }

    // Renderizar gráficos usando Canvas
    renderCharts() {
        const isDark = document.body.classList.contains("dark-mode");
        const gridColor = isDark ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.08)";
        const labelColor = isDark ? "#94a3b8" : "#475569";
        const accentColor = "#6366f1";

        // Gráfico 1: Pie Chart (Estados de Pedidos)
        const canvasPie = document.getElementById("chart-pie");
        if (canvasPie) {
            const ctx = canvasPie.getContext("2d");
            ctx.clearRect(0, 0, canvasPie.width, canvasPie.height);

            const pedidos = db.get("pedidos");
            const counts = {};
            pedidos.forEach(p => counts[p.estado] = (counts[p.estado] || 0) + 1);

            const total = pedidos.length;
            let startAngle = 0;
            const colors = { "Completado": "#10b981", "Pendiente": "#f59e0b", "Procesando": "#6366f1", "Cancelado": "#ef4444" };

            // Dibujar rebanadas
            let yLegend = 240;
            ctx.font = "11px Inter";
            ctx.fillStyle = labelColor;
            ctx.textAlign = "left";

            let xCenter = canvasPie.width / 2;
            let yCenter = 100;
            let radius = 70;

            if (total === 0) {
                ctx.fillStyle = labelColor;
                ctx.textAlign = "center";
                ctx.fillText("Sin datos registrados", xCenter, yCenter);
                return;
            }

            Object.keys(counts).forEach(key => {
                const sliceAngle = (counts[key] / total) * 2 * Math.PI;
                
                ctx.beginPath();
                ctx.moveTo(xCenter, yCenter);
                ctx.arc(xCenter, yCenter, radius, startAngle, startAngle + sliceAngle);
                ctx.closePath();
                ctx.fillStyle = colors[key] || "#cbd5e1";
                ctx.fill();

                // Leyenda
                ctx.fillStyle = colors[key];
                ctx.fillRect(20, yLegend, 12, 12);
                ctx.fillStyle = labelColor;
                const percent = ((counts[key] / total) * 100).toFixed(0);
                ctx.fillText(`${key} (${percent}%)`, 38, yLegend + 10);
                yLegend += 20;

                startAngle += sliceAngle;
            });
        }

        // Gráfico 2: Line Chart (Historial de Ventas)
        const canvasLine = document.getElementById("chart-line");
        if (canvasLine) {
            const ctx = canvasLine.getContext("2d");
            ctx.clearRect(0, 0, canvasLine.width, canvasLine.height);

            const months = Object.keys(this.ventasMensuales);
            const values = Object.values(this.ventasMensuales);
            const maxVal = Math.max(...values) * 1.15;

            // Dibujar rejilla
            ctx.strokeStyle = gridColor;
            ctx.lineWidth = 1;
            ctx.fillStyle = labelColor;
            ctx.font = "10px Inter";

            // Líneas horizontales de fondo
            for (let i = 0; i <= 4; i++) {
                const y = 30 + i * 50;
                ctx.beginPath();
                ctx.moveTo(50, y);
                ctx.lineTo(470, y);
                ctx.stroke();

                const gridVal = maxVal * (1 - i / 4);
                ctx.textAlign = "right";
                ctx.fillText(`$${Math.round(gridVal)}`, 40, y + 3);
            }

            // Dibujar línea de tendencia
            ctx.beginPath();
            ctx.strokeStyle = accentColor;
            ctx.lineWidth = 3;
            
            const points = [];
            months.forEach((m, idx) => {
                const x = 70 + idx * 75;
                const y = 230 - (values[idx] / maxVal) * 200;
                points.push({ x, y, month: m, val: values[idx] });

                if (idx === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            });
            ctx.stroke();

            // Dibujar puntos
            points.forEach(p => {
                ctx.beginPath();
                ctx.arc(p.x, p.y, 5, 0, 2 * Math.PI);
                ctx.fillStyle = accentColor;
                ctx.fill();
                ctx.strokeStyle = isDark ? "#1e293b" : "#ffffff";
                ctx.lineWidth = 2;
                ctx.stroke();

                // Etiquetas de meses
                ctx.fillStyle = labelColor;
                ctx.textAlign = "center";
                ctx.fillText(p.month, p.x, 250);
            });
        }
    }

    // --- 2. MÓDULO CRUD: CLIENTES / EMPLEADOS / PEDIDOS ---
    loadCRUD(module) {
        this.clearForm(module);
        const data = db.get(module);
        const tbody = document.querySelector(`#table-${module} tbody`);
        tbody.innerHTML = "";

        if (module === 'clientes') {
            data.forEach(c => {
                const tr = document.createElement("tr");
                tr.onclick = () => this.selectItem('clientes', c);
                tr.innerHTML = `<td>${c.id}</td><td>${c.nombre}</td><td>${c.email}</td><td>${c.telefono}</td><td>${c.ciudad}</td><td><span class="badge ${c.estado === 'Activo' ? 'success' : 'danger'}">${c.estado}</span></td>`;
                tbody.appendChild(tr);
            });
        } else if (module === 'empleados') {
            data.forEach(e => {
                const tr = document.createElement("tr");
                tr.onclick = () => this.selectItem('empleados', e);
                tr.innerHTML = `<td>${e.id}</td><td>${e.nombre} ${e.apellido}</td><td>${e.email}</td><td>${e.telefono}</td><td>${e.puesto}</td><td>$${e.salario.toFixed(2)}</td>`;
                tbody.appendChild(tr);
            });
        } else if (module === 'pedidos') {
            const clientes = db.get("clientes");
            data.forEach(p => {
                const cli = clientes.find(c => c.id === p.clienteId);
                const cliNombre = cli ? cli.nombre : "Desconocido";
                const tr = document.createElement("tr");
                tr.onclick = () => this.selectItem('pedidos', p);
                tr.innerHTML = `<td>${p.numeroPedido}</td><td>${cliNombre}</td><td>${p.fecha}</td><td>$${p.total.toFixed(2)}</td><td><span class="badge ${p.estado === 'Completado' ? 'success' : p.estado === 'Cancelado' ? 'danger' : 'warning'}">${p.estado}</span></td>`;
                tbody.appendChild(tr);
            });

            // Rellenar combo de clientes en formulario de pedidos
            const combo = document.getElementById("form-pedido-cliente");
            combo.innerHTML = "";
            clientes.forEach(c => {
                const opt = document.createElement("option");
                opt.value = c.id;
                opt.innerText = c.nombre;
                combo.appendChild(opt);
            });
        }
    }

    selectItem(module, item) {
        if (module === 'clientes') {
            document.getElementById("form-cliente-id").value = item.id;
            document.getElementById("form-cliente-nombre").value = item.nombre;
            document.getElementById("form-cliente-email").value = item.email;
            document.getElementById("form-cliente-telefono").value = item.telefono;
            document.getElementById("form-cliente-ciudad").value = item.ciudad;
            document.getElementById("form-cliente-estado").value = item.estado;
        } else if (module === 'empleados') {
            document.getElementById("form-empleado-id").value = item.id;
            document.getElementById("form-empleado-nombre").value = item.nombre;
            document.getElementById("form-empleado-apellido").value = item.apellido;
            document.getElementById("form-empleado-email").value = item.email;
            document.getElementById("form-empleado-telefono").value = item.telefono;
            document.getElementById("form-empleado-puesto").value = item.puesto;
            document.getElementById("form-empleado-salario").value = item.salario;
        } else if (module === 'pedidos') {
            document.getElementById("form-pedido-id").value = item.id;
            document.getElementById("form-pedido-numero").value = item.numeroPedido;
            document.getElementById("form-pedido-cliente").value = item.clienteId;
            document.getElementById("form-pedido-fecha").value = item.fecha;
            document.getElementById("form-pedido-total").value = item.total;
            document.getElementById("form-pedido-estado").value = item.estado;
        }
    }

    clearForm(module) {
        if (module === 'clientes') {
            document.getElementById("form-cliente-id").value = "";
            document.getElementById("form-cliente-nombre").value = "";
            document.getElementById("form-cliente-email").value = "";
            document.getElementById("form-cliente-telefono").value = "";
            document.getElementById("form-cliente-ciudad").value = "";
            document.getElementById("form-cliente-estado").value = "Activo";
        } else if (module === 'empleados') {
            document.getElementById("form-empleado-id").value = "";
            document.getElementById("form-empleado-nombre").value = "";
            document.getElementById("form-empleado-apellido").value = "";
            document.getElementById("form-empleado-email").value = "";
            document.getElementById("form-empleado-telefono").value = "";
            document.getElementById("form-empleado-puesto").value = "";
            document.getElementById("form-empleado-salario").value = "";
        } else if (module === 'pedidos') {
            document.getElementById("form-pedido-id").value = "";
            document.getElementById("form-pedido-numero").value = "";
            document.getElementById("form-pedido-fecha").value = new Date().toISOString().substring(0, 10);
            document.getElementById("form-pedido-total").value = "";
            document.getElementById("form-pedido-estado").value = "Pendiente";
        }
    }

    saveItem(module) {
        const list = db.get(module);

        if (module === 'clientes') {
            const idVal = document.getElementById("form-cliente-id").value;
            const name = document.getElementById("form-cliente-nombre").value.trim();
            const email = document.getElementById("form-cliente-email").value.trim();
            const phone = document.getElementById("form-cliente-telefono").value.trim();
            const city = document.getElementById("form-cliente-ciudad").value.trim();
            const state = document.getElementById("form-cliente-estado").value;

            if (!name || !email || !phone || !city) {
                alert("⚠️ Por favor completa todos los campos del cliente.");
                return;
            }

            if (idVal) {
                // Modificar
                const idx = list.findIndex(c => c.id == idVal);
                list[idx] = { id: parseInt(idVal), nombre: name, email, telefono: phone, ciudad: city, estado: state };
                db.log(this.session.username, `Editó cliente: ${name} (ID: ${idVal})`);
            } else {
                // Nuevo
                const nextId = list.length > 0 ? Math.max(...list.map(c => c.id)) + 1 : 1;
                list.push({ id: nextId, nombre: name, email, telefono: phone, ciudad: city, estado: state });
                db.log(this.session.username, `Agregó cliente: ${name}`);
            }

        } else if (module === 'empleados') {
            const idVal = document.getElementById("form-empleado-id").value;
            const name = document.getElementById("form-empleado-nombre").value.trim();
            const last = document.getElementById("form-empleado-apellido").value.trim();
            const email = document.getElementById("form-empleado-email").value.trim();
            const phone = document.getElementById("form-empleado-telefono").value.trim();
            const puesto = document.getElementById("form-empleado-puesto").value.trim();
            const salary = parseFloat(document.getElementById("form-empleado-salario").value) || 0;

            if (!name || !last || !email || !phone || !puesto) {
                alert("⚠️ Por favor completa todos los campos del empleado.");
                return;
            }

            if (idVal) {
                const idx = list.findIndex(e => e.id == idVal);
                list[idx] = { id: parseInt(idVal), nombre: name, apellido: last, email, telefono: phone, puesto, salario: salary };
                db.log(this.session.username, `Editó empleado: ${name} (ID: ${idVal})`);
            } else {
                const nextId = list.length > 0 ? Math.max(...list.map(e => e.id)) + 1 : 1;
                list.push({ id: nextId, nombre: name, apellido: last, email, telefono: phone, puesto, salario: salary });
                db.log(this.session.username, `Agregó empleado: ${name} ${last}`);
            }

        } else if (module === 'pedidos') {
            const idVal = document.getElementById("form-pedido-id").value;
            const num = document.getElementById("form-pedido-numero").value.trim();
            const cliId = parseInt(document.getElementById("form-pedido-cliente").value);
            const date = document.getElementById("form-pedido-fecha").value;
            const total = parseFloat(document.getElementById("form-pedido-total").value) || 0;
            const state = document.getElementById("form-pedido-estado").value;

            if (!num || !cliId || !date || !total) {
                alert("⚠️ Por favor completa todos los campos del pedido.");
                return;
            }

            if (idVal) {
                const idx = list.findIndex(p => p.id == idVal);
                list[idx] = { id: parseInt(idVal), numeroPedido: num, clienteId: cliId, fecha: date, total, estado: state };
                db.log(this.session.username, `Editó pedido #${num} (ID: ${idVal}, Estado: ${state})`);
            } else {
                const nextId = list.length > 0 ? Math.max(...list.map(p => p.id)) + 1 : 1;
                list.push({ id: nextId, numeroPedido: num, clienteId: cliId, fecha: date, total, estado: state });
                db.log(this.session.username, `Agregó pedido #${num} (Total: ${total})`);
            }
        }

        db.save(module, list);
        this.loadCRUD(module);
    }

    deleteItem(module) {
        const idVal = document.getElementById(`form-${module.slice(0, -1)}-id`).value;
        if (!idVal) {
            alert("⚠️ Selecciona un elemento de la tabla primero.");
            return;
        }

        if (confirm("¿Estás seguro de que deseas eliminar este elemento?")) {
            const list = db.get(module);
            const index = list.findIndex(item => item.id == idVal);
            if (index !== -1) {
                const removedName = list[index].nombre || list[index].numeroPedido;
                list.splice(index, 1);
                db.save(module, list);
                db.log(this.session.username, `Eliminó elemento de ${module}: ${removedName} (ID: ${idVal})`);
                this.loadCRUD(module);
            }
        }
    }

    filterTable(module) {
        const query = document.getElementById(`search-${module}`).value.toLowerCase();
        const rows = document.querySelectorAll(`#table-${module} tbody tr`);

        rows.forEach(row => {
            const text = row.innerText.toLowerCase();
            if (text.includes(query)) {
                row.style.display = "";
            } else {
                row.style.display = "none";
            }
        });
    }

    // --- 3. MÓDULO: REPORTES ---
    loadReportes() {
        document.getElementById("report-from").value = "2026-05-01";
        document.getElementById("report-to").value = "2026-05-31";
        this.renderReportCharts();
    }

    renderReportCharts() {
        const type = document.getElementById("report-type").value;
        const canvas = document.getElementById("chart-report");
        if (!canvas) return;

        const ctx = canvas.getContext("2d");
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        const isDark = document.body.classList.contains("dark-mode");
        const gridColor = isDark ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.08)";
        const labelColor = isDark ? "#94a3b8" : "#475569";
        const accentColor = "#6366f1";

        document.getElementById("report-chart-title").innerText = `Visualización de Reporte: ${type}`;

        const pedidos = db.get("pedidos");

        if (type === "Ventas Mensuales") {
            const months = Object.keys(this.ventasMensuales);
            const values = Object.values(this.ventasMensuales);
            const maxVal = Math.max(...values) * 1.15;

            // Calcular KPIs de reportes
            const total = values.reduce((a, b) => a + b, 0);
            const avg = total / values.length;
            const max = Math.max(...values);

            document.getElementById("report-total").innerText = `$${total.toLocaleString('es-ES', { minimumFractionDigits: 2 })}`;
            document.getElementById("report-average").innerText = `$${avg.toLocaleString('es-ES', { minimumFractionDigits: 2 })}`;
            document.getElementById("report-max").innerText = `$${max.toLocaleString('es-ES', { minimumFractionDigits: 2 })}`;

            // Dibujar Bar Chart
            ctx.fillStyle = labelColor;
            ctx.font = "10px Inter";
            ctx.strokeStyle = gridColor;

            for (let i = 0; i <= 4; i++) {
                const y = 30 + i * 45;
                ctx.beginPath();
                ctx.moveTo(50, y);
                ctx.lineTo(550, y);
                ctx.stroke();

                const val = maxVal * (1 - i / 4);
                ctx.textAlign = "right";
                ctx.fillText(`$${Math.round(val)}`, 40, y + 3);
            }

            months.forEach((m, idx) => {
                const x = 80 + idx * 75;
                const barHeight = (values[idx] / maxVal) * 180;
                const y = 210 - barHeight;

                ctx.fillStyle = accentColor;
                ctx.fillRect(x - 15, y, 30, barHeight);

                ctx.fillStyle = labelColor;
                ctx.textAlign = "center";
                ctx.fillText(m, x, 230);
            });

        } else if (type === "Pedidos por Estado") {
            const counts = {};
            pedidos.forEach(p => counts[p.estado] = (counts[p.estado] || 0) + 1);

            const values = Object.values(counts);
            const maxVal = Math.max(...values, 0) + 1;

            document.getElementById("report-total").innerText = pedidos.length + " pedidos";
            document.getElementById("report-average").innerText = (pedidos.length / 4).toFixed(1) + " prom/est";
            document.getElementById("report-max").innerText = (maxVal - 1) + " max estado";

            // Dibujar barras horizontales
            ctx.fillStyle = labelColor;
            ctx.font = "12px Inter";
            ctx.textAlign = "left";

            let y = 50;
            const colors = { "Completado": "#10b981", "Pendiente": "#f59e0b", "Procesando": "#6366f1", "Cancelado": "#ef4444" };

            Object.keys(counts).forEach(key => {
                ctx.fillStyle = labelColor;
                ctx.fillText(key, 30, y + 15);

                const barWidth = (counts[key] / maxVal) * 350;
                ctx.fillStyle = colors[key] || "#cbd5e1";
                ctx.fillRect(120, y, barWidth, 20);

                ctx.fillStyle = labelColor;
                ctx.fillText(counts[key].toString(), 130 + barWidth, y + 15);
                y += 45;
            });
        } else if (type === "Clientes por Ciudad") {
            const clientes = db.get("clientes");
            const cities = {};
            clientes.forEach(c => cities[c.ciudad] = (cities[c.ciudad] || 0) + 1);

            document.getElementById("report-total").innerText = clientes.length + " clientes";
            document.getElementById("report-average").innerText = (clientes.length / Object.keys(cities).length || 0).toFixed(1) + " prom/ciudad";
            document.getElementById("report-max").innerText = Math.max(...Object.values(cities), 0) + " max ciudad";

            ctx.fillStyle = labelColor;
            ctx.font = "12px Inter";
            ctx.textAlign = "left";

            let y = 50;
            Object.keys(cities).forEach(city => {
                ctx.fillStyle = labelColor;
                ctx.fillText(city, 30, y + 15);

                const barWidth = (cities[city] / clientes.length) * 350;
                ctx.fillStyle = "#a855f7";
                ctx.fillRect(120, y, barWidth, 20);

                ctx.fillStyle = labelColor;
                ctx.fillText(cities[city].toString(), 130 + barWidth, y + 15);
                y += 45;
            });
        }
    }

    // Exportación del reporte a CSV o PDF
    exportReport(format) {
        const type = document.getElementById("report-type").value;
        const filename = `reporte_${type.toLowerCase().replace(" ", "_")}_${new Date().toISOString().substring(0,10)}`;

        if (format === 'csv') {
            let csvContent = "data:text/csv;charset=utf-8,";
            csvContent += "Concepto,Valor\r\n";
            csvContent += `Reporte,${type}\r\n`;
            csvContent += `Fecha de Generacion,${new Date().toLocaleDateString()}\r\n`;
            csvContent += `Rango,${document.getElementById("report-from").value} a ${document.getElementById("report-to").value}\r\n`;
            csvContent += `Metrica Total,${document.getElementById("report-total").innerText}\r\n`;
            csvContent += `Metrica Promedio,${document.getElementById("report-average").innerText}\r\n`;
            csvContent += `Metrica Maximo,${document.getElementById("report-max").innerText}\r\n`;

            const encodedUri = encodeURI(csvContent);
            const link = document.createElement("a");
            link.setAttribute("href", encodedUri);
            link.setAttribute("download", `${filename}.csv`);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            db.log(this.session.username, `Exportó reporte a CSV (${filename}.csv)`);
        } else if (format === 'pdf') {
            const { jsPDF } = window.jspdf;
            const doc = new jsPDF();

            doc.setFont("helvetica", "bold");
            doc.setFontSize(20);
            doc.text("RCP GESTIÓN EMPRESARIAL", 20, 25);
            
            doc.setFontSize(14);
            doc.setFont("helvetica", "normal");
            doc.text(`Tipo de Reporte: ${type}`, 20, 38);
            doc.text(`Fecha de Emisión: ${new Date().toLocaleDateString()}`, 20, 46);
            doc.text(`Periodo: ${document.getElementById("report-from").value} a ${document.getElementById("report-to").value}`, 20, 54);

            doc.setLineWidth(0.5);
            doc.line(20, 62, 190, 62);

            doc.setFont("helvetica", "bold");
            doc.text("Resumen Ejecutivo:", 20, 75);
            doc.setFont("helvetica", "normal");
            doc.text(` - Total en rango: ${document.getElementById("report-total").innerText}`, 20, 85);
            doc.text(` - Promedio: ${document.getElementById("report-average").innerText}`, 20, 93);
            doc.text(` - Máximo registrado: ${document.getElementById("report-max").innerText}`, 20, 101);

            doc.line(20, 115, 190, 115);
            doc.text("Generado automáticamente desde la plataforma web del cliente.", 20, 130);

            doc.save(`${filename}.pdf`);
            db.log(this.session.username, `Exportó reporte a PDF (${filename}.pdf)`);
        }
    }

    // --- 4. MÓDULO: CONFIGURACIÓN ---
    loadConfiguracion() {
        this.switchConfigTab("settings");
    }

    switchConfigTab(tabName) {
        document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

        const activeBtn = Array.from(document.querySelectorAll('.tab-btn')).find(btn => btn.innerText.toLowerCase().includes(tabName === 'settings' ? 'general' : 'auditoría'));
        if (activeBtn) activeBtn.classList.add('active');

        document.getElementById(`tab-config-${tabName}`).classList.add('active');

        if (tabName === 'audit') {
            this.loadAuditLogs();
        }
    }

    loadAuditLogs() {
        const logs = db.get("logs");
        const tbody = document.querySelector("#table-logs tbody");
        tbody.innerHTML = "";

        logs.forEach(l => {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td>${l.id}</td><td><strong>${l.usuario}</strong></td><td>${l.accion}</td><td>${l.fecha}</td>`;
            tbody.appendChild(tr);
        });
    }

    saveConfig() {
        const name = document.getElementById("config-empresa-nombre").value.trim();
        db.log(this.session.username, `Guardó cambios en la configuración empresarial (Empresa: ${name})`);
        alert("✅ Configuración guardada correctamente.");
    }

    // --- 5. IMPORTADOR DE CSV CLIENTE ---
    importCSV(module) {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = ".csv";
        
        input.onchange = (e) => {
            const file = e.target.files[0];
            if (!file) return;

            const reader = new FileReader();
            reader.onload = (evt) => {
                const text = evt.target.result;
                const lines = text.split("\n");
                
                const list = db.get(module);
                let importados = 0;
                let errores = 0;

                for (let i = 1; i < lines.length; i++) {
                    const line = lines[i].trim();
                    if (!line) continue;

                    const parts = line.split(/[,;]/);

                    if (module === 'clientes' && parts.length >= 5) {
                        try {
                            const nextId = list.length > 0 ? Math.max(...list.map(c => c.id)) + 1 : 1;
                            list.push({
                                id: nextId,
                                nombre: parts[0].trim(),
                                email: parts[1].trim(),
                                telefono: parts[2].trim(),
                                ciudad: parts[3].trim(),
                                estado: parts[4].trim() || "Activo"
                            });
                            importados++;
                        } catch (err) { errores++; }
                    } else if (module === 'empleados' && parts.length >= 6) {
                        try {
                            const nextId = list.length > 0 ? Math.max(...list.map(e => e.id)) + 1 : 1;
                            list.push({
                                id: nextId,
                                nombre: parts[0].trim(),
                                apellido: parts[1].trim(),
                                email: parts[2].trim(),
                                telefono: parts[3].trim(),
                                puesto: parts[4].trim(),
                                salario: parseFloat(parts[5].trim()) || 0.00
                            });
                            importados++;
                        } catch (err) { errores++; }
                    } else {
                        errores++;
                    }
                }

                db.save(module, list);
                db.log(this.session.username, `Importó ${importados} registros en ${module} desde ${file.name}`);
                this.loadCRUD(module);
                alert(`✅ Importación completada.\nRegistros importados con éxito: ${importados}\nFilas fallidas: ${errores}`);
            };
            reader.readAsText(file);
        };
        input.click();
    }
}

// Iniciar aplicación globalmente
window.app = new AppController();

// Firebase Configuration
const firebaseConfig = {
    apiKey: "AIzaSyB_hTt7AwyumfoeDVx13vczZexywrbVhAs",
    authDomain: "inventarioollas.firebaseapp.com",
    databaseURL: "https://inventarioollas.firebaseio.com",
    projectId: "inventarioollas",
    storageBucket: "inventarioollas.firebasestorage.app",
    messagingSenderId: "977587125876",
    appId: "1:977587125876:android:0c95283e953da445de1b21"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
const database = firebase.database();

// --- Auth Functions ---

function login(email, password) {
    return auth.signInWithEmailAndPassword(email, password);
}

function logout() {
    auth.signOut().then(() => {
        window.location.href = "index.html";
    }).catch((error) => {
        console.error("Error signing out", error);
    });
}

function checkAuth() {
    auth.onAuthStateChanged((user) => {
        if (!user) {
            window.location.href = "index.html";
        } else {
            const userEmailEl = document.getElementById("userEmail");
            if (userEmailEl) {
                userEmailEl.textContent = user.email;
            }
        }
    });
}

// --- Dashboard Logic ---

function initDashboard() {
    loadProductos();
    loadVentas();
}

function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(el => el.style.display = 'none');
    document.querySelectorAll('.sidebar nav a').forEach(el => el.classList.remove('active'));

    document.getElementById(sectionId + 'Section').style.display = 'block';
    // Update active link logic roughly
    event.target.classList.add('active');
}

function loadProductos() {
    const tableBody = document.querySelector("#productosTable tbody");
    if (!tableBody) return;

    database.ref('productos').on('value', (snapshot) => {
        tableBody.innerHTML = "";
        const data = snapshot.val();

        if (!data) {
            tableBody.innerHTML = "<tr><td colspan='5' style='text-align: center;'>No hay productos registrados.</td></tr>";
            return;
        }

        Object.values(data).forEach(producto => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${producto.nombre}</td>
                <td>${producto.codigo}</td>
                <td>${producto.stock}</td>
                <td>S/ ${parseFloat(producto.precioUnitario).toFixed(2)}</td>
                <td>${producto.categoria}</td>
            `;
            tableBody.appendChild(row);
        });
    });
}

function loadVentas() {
    const tableBody = document.querySelector("#ventasTable tbody");
    if (!tableBody) return;

    database.ref('ventas').limitToLast(50).on('value', (snapshot) => {
        tableBody.innerHTML = "";
        const data = snapshot.val();

        if (!data) {
            tableBody.innerHTML = "<tr><td colspan='5' style='text-align: center;'>No hay ventas registradas.</td></tr>";
            return;
        }

        // Convert object to array and sort by date descending
        const ventas = Object.values(data).sort((a, b) => b.fecha - a.fecha);

        ventas.forEach(venta => {
            const date = new Date(venta.fecha).toLocaleDateString() + ' ' + new Date(venta.fecha).toLocaleTimeString();
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${date}</td>
                <td>${venta.clienteNombre || 'Cliente General'}</td>
                <td>${venta.vendedorNombre || 'Desconocido'}</td>
                <td>S/ ${parseFloat(venta.total).toFixed(2)}</td>
                <td><button onclick="verDetalleVenta('${venta.id}')" style="cursor:pointer; color: blue; text-decoration: underline; background: none; border: none;">Ver</button></td>
            `;
            tableBody.appendChild(row);
        });
    });
}

function verDetalleVenta(ventaId) {
    alert("Detalle de venta: " + ventaId + "\n(Implementación de modal pendiente)");
}

// --- Login Page Logic ---

document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("loginForm");
    if (loginForm) {
        // Redirect if already logged in
        auth.onAuthStateChanged((user) => {
            if (user) {
                window.location.href = "dashboard.html";
            }
        });

        loginForm.addEventListener("submit", (e) => {
            e.preventDefault();
            const email = document.getElementById("email").value;
            const password = document.getElementById("password").value;
            const errorMsg = document.getElementById("errorMessage");
            const btn = document.getElementById("loginBtn");

            btn.disabled = true;
            btn.textContent = "Ingresando...";
            errorMsg.textContent = "";

            login(email, password)
                .then(() => {
                    // Redirect handled by onAuthStateChanged
                })
                .catch((error) => {
                    btn.disabled = false;
                    btn.textContent = "Ingresar";
                    let msg = "Error al iniciar sesión.";
                    if (error.code === 'auth/wrong-password' || error.code === 'auth/user-not-found') {
                        msg = "Correo o contraseña incorrectos.";
                    } else if (error.code === 'auth/invalid-email') {
                        msg = "Correo inválido.";
                    }
                    errorMsg.textContent = msg;
                    console.error(error);
                });
        });
    }
});

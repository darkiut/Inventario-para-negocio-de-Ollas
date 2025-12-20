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
            const stock = producto.stock;
            let stockClass = "";

            // Semáforo Logic
            if (stock <= 5) { // Updated to include 5 in critical
                stockClass = "stock-critical";
            } else if (stock >= 6 && stock <= 15) {
                stockClass = "stock-warning";
            } else {
                stockClass = "stock-good";
            }

            const row = document.createElement("tr");
            row.className = stockClass; // Apply row color

            // Safe DOM creation to prevent XSS
            const cellNombre = document.createElement("td");
            cellNombre.textContent = producto.nombre;

            const cellCodigo = document.createElement("td");
            cellCodigo.textContent = producto.codigo;

            const cellStock = document.createElement("td");
            cellStock.innerHTML = `<strong>${stock}</strong>`;

            const cellPrecio = document.createElement("td");
            cellPrecio.textContent = `S/ ${parseFloat(producto.precioUnitario).toFixed(2)}`;

            const cellCategoria = document.createElement("td");
            cellCategoria.textContent = producto.categoria;

            row.appendChild(cellNombre);
            row.appendChild(cellCodigo);
            row.appendChild(cellStock);
            row.appendChild(cellPrecio);
            row.appendChild(cellCategoria);

            tableBody.appendChild(row);
        });
    });
}

function loadVentas() {
    const tableBody = document.querySelector("#ventasTable tbody");
    if (!tableBody) return;

    // Listen to changes in real-time
    database.ref('ventas').limitToLast(100).on('value', (snapshot) => {
        // Save data globally for filtering
        window.allVentas = snapshot.val() ? Object.values(snapshot.val()) : [];
        renderVentas(window.allVentas);
    });
}

function renderVentas(ventas) {
    const tableBody = document.querySelector("#ventasTable tbody");
    if (!tableBody) return;

    tableBody.innerHTML = "";

    if (!ventas || ventas.length === 0) {
        tableBody.innerHTML = "<tr><td colspan='5' style='text-align: center;'>No hay ventas registradas.</td></tr>";
        return;
    }

    // Sort by date descending
    const ventasSorted = [...ventas].sort((a, b) => b.fecha - a.fecha);

    ventasSorted.forEach(venta => {
        const dateObj = new Date(venta.fecha);
        const dateStr = dateObj.toLocaleDateString() + ' ' + dateObj.toLocaleTimeString();

        // WhatsApp Link Construction
        const cliente = venta.clienteNombre || "Cliente";
        const mensaje = encodeURIComponent(`Hola ${cliente}, aquí tienes tu nota de pedido de Ollas.`);
        const telefono = venta.clienteTelefono || "";
        const waLink = `https://wa.me/${telefono}?text=${mensaje}`;

        // Serialize venta for print function safely
        const ventaJson = JSON.stringify(venta).replace(/'/g, "\\'").replace(/"/g, '&quot;');

        const row = document.createElement("tr");

        // Safe DOM creation
        const cellDate = document.createElement("td");
        cellDate.textContent = dateStr;

        const cellCliente = document.createElement("td");
        cellCliente.textContent = venta.clienteNombre || 'Cliente General';

        const cellVendedor = document.createElement("td");
        cellVendedor.textContent = venta.vendedorNombre || 'Desconocido';

        const cellTotal = document.createElement("td");
        cellTotal.textContent = `S/ ${parseFloat(venta.total).toFixed(2)}`;

        const cellAcciones = document.createElement("td");
        cellAcciones.innerHTML = `
            <a href="${waLink}" target="_blank" class="btn-whatsapp">WhatsApp</a>
            <button onclick="imprimirVenta(${ventaJson})" class="btn-print">Imprimir</button>
        `;

        row.appendChild(cellDate);
        row.appendChild(cellCliente);
        row.appendChild(cellVendedor);
        row.appendChild(cellTotal);
        row.appendChild(cellAcciones);

        tableBody.appendChild(row);
    });
}

function filterVentas() {
    const dateInput = document.getElementById('filterDate').value;
    const sellerInput = document.getElementById('filterSeller').value.toLowerCase();

    if (!window.allVentas) return;

    const filtered = window.allVentas.filter(venta => {
        const ventaDate = new Date(venta.fecha);
        const year = ventaDate.getFullYear();
        const month = String(ventaDate.getMonth() + 1).padStart(2, '0');
        const day = String(ventaDate.getDate()).padStart(2, '0');
        const ventaDateStr = `${year}-${month}-${day}`;

        const dateMatch = !dateInput || ventaDateStr === dateInput;
        const sellerMatch = !sellerInput || (venta.vendedorNombre && venta.vendedorNombre.toLowerCase().includes(sellerInput));

        return dateMatch && sellerMatch;
    });

    renderVentas(filtered);
}

function imprimirVenta(venta) {
    const printableArea = document.getElementById('printableArea');
    const dateStr = new Date(venta.fecha).toLocaleString();

    // Clear previous content
    printableArea.innerHTML = '';

    const invoiceContainer = document.createElement('div');
    invoiceContainer.className = 'invoice-container';

    // Header
    const header = document.createElement('div');
    header.className = 'invoice-header';
    header.innerHTML = `
        <h2>NOTA DE PEDIDO</h2>
        <p>Fecha: ${dateStr}</p>
        <p>Nro: ${venta.id ? venta.id.substring(1, 8).toUpperCase() : '---'}</p>
    `;

    // Details (Sanitized)
    const details = document.createElement('div');
    details.className = 'invoice-details';

    // Helper to create detail lines safely
    const createDetail = (label, value) => {
        const p = document.createElement('p');
        const strong = document.createElement('strong');
        strong.textContent = label + ': ';
        p.appendChild(strong);
        p.appendChild(document.createTextNode(value || '-'));
        return p;
    };

    details.appendChild(createDetail('Cliente', venta.clienteNombre || 'General'));
    details.appendChild(createDetail('DNI/RUC', venta.clienteDni || venta.clienteRuc));
    details.appendChild(createDetail('Dirección', venta.clienteDireccion));
    details.appendChild(createDetail('Vendedor', venta.vendedorNombre));

    // Table
    const table = document.createElement('table');
    table.className = 'invoice-items';
    table.innerHTML = `
        <thead>
            <tr>
                <th>Producto</th>
                <th>Cant.</th>
                <th>P. Unit.</th>
                <th>Subtotal</th>
            </tr>
        </thead>
    `;

    const tbody = document.createElement('tbody');
    if (venta.productos) {
        Object.values(venta.productos).forEach(item => {
            const tr = document.createElement('tr');

            const tdNombre = document.createElement('td');
            tdNombre.textContent = item.nombre;

            const tdCant = document.createElement('td');
            tdCant.textContent = item.cantidad;

            const tdPrice = document.createElement('td');
            tdPrice.textContent = `S/ ${parseFloat(item.precioUnitario).toFixed(2)}`;

            const tdSub = document.createElement('td');
            tdSub.textContent = `S/ ${parseFloat(item.subtotal).toFixed(2)}`;

            tr.appendChild(tdNombre);
            tr.appendChild(tdCant);
            tr.appendChild(tdPrice);
            tr.appendChild(tdSub);
            tbody.appendChild(tr);
        });
    }
    table.appendChild(tbody);

    // Total
    const totalDiv = document.createElement('div');
    totalDiv.className = 'invoice-total';
    totalDiv.textContent = `TOTAL: S/ ${parseFloat(venta.total).toFixed(2)}`;

    // Footer
    const footer = document.createElement('div');
    footer.style.textAlign = 'center';
    footer.style.marginTop = '2rem';
    footer.style.fontSize = '0.8rem';
    footer.textContent = '¡Gracias por su preferencia!';

    invoiceContainer.appendChild(header);
    invoiceContainer.appendChild(details);
    invoiceContainer.appendChild(table);
    invoiceContainer.appendChild(totalDiv);
    invoiceContainer.appendChild(footer);

    printableArea.appendChild(invoiceContainer);

    window.print();
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

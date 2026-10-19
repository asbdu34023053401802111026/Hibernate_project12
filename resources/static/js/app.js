'use strict';
const $ = (s) => document.querySelector(s);
const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = (v) => '$' + Number(v || 0).toFixed(2);
const LOW_STOCK = 10;

const ENTITIES = {
  products: { title: 'Products', one: 'product',
    cols: [['name', 'Name'], ['price', 'Price', money], ['stockQuantity', 'Stock', (v) => `<span class="${v <= LOW_STOCK ? 'low' : ''}">${v}</span>`], ['categoryName', 'Category'], ['supplierName', 'Supplier']],
    fields: [{ k: 'name', label: 'Name', required: true }, { k: 'price', label: 'Price', type: 'number', step: '0.01', min: '0', required: true },
      { k: 'stockQuantity', label: 'Stock quantity', type: 'number', min: '0', required: true },
      { k: 'categoryId', label: 'Category', select: 'categories' }, { k: 'supplierId', label: 'Supplier', select: 'suppliers' }] },
  categories: { title: 'Categories', one: 'category', cols: [['name', 'Name'], ['description', 'Description']],
    fields: [{ k: 'name', label: 'Name', required: true }, { k: 'description', label: 'Description' }] },
  suppliers: { title: 'Suppliers', one: 'supplier', cols: [['name', 'Name'], ['contactEmail', 'Email'], ['phone', 'Phone']],
    fields: [{ k: 'name', label: 'Name', required: true }, { k: 'contactEmail', label: 'Email', type: 'email' }, { k: 'phone', label: 'Phone' }] },
  customers: { title: 'Customers', one: 'customer', cols: [['name', 'Name'], ['email', 'Email'], ['phone', 'Phone']],
    fields: [{ k: 'name', label: 'Name', required: true }, { k: 'email', label: 'Email', type: 'email' }, { k: 'phone', label: 'Phone' }] }
};

const data = {};
let current = 'dashboard';

async function api(path, opts = {}) {
  const res = await fetch('/api/' + path, { headers: { 'Content-Type': 'application/json' }, ...opts });
  const body = res.status === 204 ? null : await res.json().catch(() => null);
  if (!res.ok) throw new Error(body?.message || `Request failed (${res.status})`);
  return body;
}
function toast(msg) {
  const t = $('#toast'); t.textContent = msg; t.hidden = false;
  clearTimeout(toast.id); toast.id = setTimeout(() => (t.hidden = true), 4000);
}
async function loadAll() {
  const names = ['products', 'categories', 'suppliers', 'customers', 'orders'];
  const lists = await Promise.all(names.map((n) => api(n)));
  names.forEach((n, i) => (data[n] = lists[i]));
}

function openDialog(title, html, onSubmit) {
  $('#dlg h2').textContent = title;
  $('#form .body').innerHTML = html;
  $('#form').onsubmit = async (e) => {
    e.preventDefault();
    try { await onSubmit(new FormData($('#form'))); $('#dlg').close(); await show(current); }
    catch (err) { toast(err.message); }
  };
  $('#dlg').showModal();
}
$('#cancel').onclick = () => $('#dlg').close();

function fieldHtml(f, item) {
  const val = item?.[f.k] ?? '';
  if (f.select) {
    const opts = data[f.select].map((o) => `<option value="${o.id}" ${o.id === val ? 'selected' : ''}>${esc(o.name)}</option>`).join('');
    return `<label>${f.label}<select name="${f.k}"><option value="">None</option>${opts}</select></label>`;
  }
  return `<label>${f.label}<input name="${f.k}" value="${esc(val)}" type="${f.type || 'text'}" ${f.step ? `step="${f.step}"` : ''} ${f.min ? `min="${f.min}"` : ''} ${f.required ? 'required' : ''}></label>`;
}

function entityForm(name, item) {
  const cfg = ENTITIES[name];
  openDialog(`${item ? 'Edit' : 'New'} ${cfg.one}`, cfg.fields.map((f) => fieldHtml(f, item)).join(''), async (fd) => {
    const body = {};
    cfg.fields.forEach((f) => {
      const v = fd.get(f.k);
      body[f.k] = f.type === 'number' ? Number(v) : f.select ? (v ? Number(v) : null) : v;
    });
    await api(item ? `${name}/${item.id}` : name, { method: item ? 'PUT' : 'POST', body: JSON.stringify(body) });
  });
}

async function remove(name, id) {
  if (!confirm('Delete this record?')) return;
  try { await api(`${name}/${id}`, { method: 'DELETE' }); await show(current); } catch (err) { toast(err.message); }
}

function table(name, rows, head, rowHtml) {
  if (!rows.length) return '<div class="scroll"><p class="empty">Nothing here yet. Use the button above to add the first one.</p></div>';
  return `<div class="scroll"><table><thead><tr>${head.map((h) => `<th>${h}</th>`).join('')}<th></th></tr></thead><tbody>${rows.map(rowHtml).join('')}</tbody></table></div>`;
}

function viewEntity(name) {
  const cfg = ENTITIES[name];
  $('#view').innerHTML = table(name, data[name], cfg.cols.map((c) => c[1]), (r) =>
    `<tr>${cfg.cols.map(([k, , fmt]) => `<td>${fmt ? fmt(r[k]) : esc(r[k])}</td>`).join('')}
     <td class="act"><button data-edit="${r.id}">Edit</button><button class="danger" data-del="${r.id}">Delete</button></td></tr>`);
  $('#view').querySelectorAll('[data-edit]').forEach((b) => (b.onclick = () => entityForm(name, data[name].find((r) => r.id == b.dataset.edit))));
  $('#view').querySelectorAll('[data-del]').forEach((b) => (b.onclick = () => remove(name, b.dataset.del)));
}

function viewOrders() {
  $('#view').innerHTML = table('orders', data.orders, ['Order', 'Date', 'Customer', 'Items', 'Total'], (o) =>
    `<tr><td>#${o.id}</td><td>${new Date(o.orderDate).toLocaleString()}</td><td>${esc(o.customerName)}</td>
     <td>${o.items.map((i) => `${esc(i.productName)} × ${i.quantity}`).join(', ')}</td><td>${money(o.totalAmount)}</td>
     <td class="act"><button class="danger" data-del="${o.id}">Delete</button></td></tr>`);
  $('#view').querySelectorAll('[data-del]').forEach((b) => (b.onclick = () => remove('orders', b.dataset.del)));
}

function orderRow() {
  const opts = data.products.map((p) => `<option value="${p.id}">${esc(p.name)} (${p.stockQuantity} in stock, ${money(p.price)})</option>`).join('');
  return `<div class="row"><select name="productId">${opts}</select><input name="quantity" type="number" min="1" value="1" aria-label="Quantity"></div>`;
}
function orderForm() {
  if (!data.customers.length || !data.products.length) return toast('Add at least one customer and one product first.');
  const customers = data.customers.map((c) => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
  openDialog('New order', `<label>Customer<select name="customerId">${customers}</select></label>
    <div id="rows">${orderRow()}</div><button type="button" id="addRow">Add another item</button>`, async (fd) => {
    const ids = fd.getAll('productId'), qty = fd.getAll('quantity');
    const items = ids.map((id, i) => ({ productId: Number(id), quantity: Number(qty[i]) }));
    await api('orders', { method: 'POST', body: JSON.stringify({ customerId: Number(fd.get('customerId')), items }) });
  });
  $('#addRow').onclick = () => $('#rows').insertAdjacentHTML('beforeend', orderRow());
}

function viewDashboard() {
  const revenue = data.orders.reduce((s, o) => s + Number(o.totalAmount), 0);
  const low = data.products.filter((p) => p.stockQuantity <= LOW_STOCK);
  const stat = (label, v) => `<div class="stat"><small>${label}</small><strong>${v}</strong></div>`;
  $('#view').innerHTML = `<div class="stats">${stat('Products', data.products.length)}${stat('Customers', data.customers.length)}${stat('Orders', data.orders.length)}${stat('Revenue', money(revenue))}</div>
    <div class="panel"><h3>Low stock (${LOW_STOCK} or fewer)</h3>${low.length ? `<ul>${low.map((p) => `<li>${esc(p.name)}: <span class="low">${p.stockQuantity}</span> left</li>`).join('')}</ul>` : '<p class="empty">All products are well stocked.</p>'}</div>`;
}

async function show(view) {
  current = view;
  document.querySelectorAll('#nav button').forEach((b) => b.classList.toggle('on', b.dataset.view === view));
  try { await loadAll(); } catch (err) { return toast('Could not reach the server. ' + err.message); }
  const add = $('#addBtn');
  add.hidden = view === 'dashboard';
  if (view === 'dashboard') { $('#title').textContent = 'Dashboard'; return viewDashboard(); }
  if (view === 'orders') { $('#title').textContent = 'Orders'; add.textContent = 'New order'; add.onclick = orderForm; return viewOrders(); }
  $('#title').textContent = ENTITIES[view].title; add.textContent = `New ${ENTITIES[view].one}`; add.onclick = () => entityForm(view);
  viewEntity(view);
}

document.querySelectorAll('#nav button').forEach((b) => (b.onclick = () => show(b.dataset.view)));
show('dashboard');

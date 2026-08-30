import { useEffect, useMemo, useState } from "react";
import { api, clearToken, getToken, setToken } from "./api";

const emptyProduct = {
  name: "",
  description: "",
  price: "",
  sku: "",
  imageUrl: "",
  categoryId: ""
};

function money(value) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2
  }).format(value);
}

function App() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [query, setQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("all");
  const [token, setAuthToken] = useState(getToken());
  const [screen, setScreen] = useState("shop");
  const [notice, setNotice] = useState("");
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyProduct);
  const [myProducts, setMyProducts] = useState([]);

  async function loadCatalog() {
    setLoading(true);
    try {
      const [productData, categoryData] = await Promise.all([
        api("/products"),
        api("/categories")
      ]);
      setProducts(productData);
      setCategories(categoryData);
    } catch (error) {
      setNotice(error.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadCatalog();
  }, []);

  const visibleProducts = useMemo(() => products.filter((product) => {
    const matchesQuery = `${product.name} ${product.description} ${product.sku}`
      .toLowerCase()
      .includes(query.toLowerCase());
    const matchesCategory = selectedCategory === "all" || String(product.category.id) === selectedCategory;
    return matchesQuery && matchesCategory && product.active;
  }), [products, query, selectedCategory]);

  function openCreate() {
    setEditing(null);
    setForm({ ...emptyProduct, categoryId: categories[0]?.id ? String(categories[0].id) : "" });
    setScreen("editor");
  }

  async function openManage() {
    setNotice("");
    try {
      setMyProducts(await api("/products/mine"));
      setScreen("manage");
    } catch (error) {
      setNotice(error.message);
    }
  }

  function openEdit(product) {
    setEditing(product);
    setForm({
      name: product.name,
      description: product.description || "",
      price: String(product.price),
      sku: product.sku,
      imageUrl: product.imageUrl || "",
      categoryId: String(product.category.id)
    });
    setScreen("editor");
  }

  async function saveProduct(event) {
    event.preventDefault();
    setNotice("");
    const payload = { ...form, price: Number(form.price), categoryId: Number(form.categoryId) };
    try {
      if (editing) {
        await api(`/products/${editing.id}`, { method: "PUT", body: JSON.stringify(payload) });
        setNotice("Product updated successfully.");
      } else {
        await api("/products", { method: "POST", body: JSON.stringify(payload) });
        setNotice("Product created successfully.");
      }
      await loadCatalog();
      await openManage();
    } catch (error) {
      setNotice(error.message);
    }
  }

  async function deleteProduct(product) {
    if (!window.confirm(`Delete ${product.name}? This cannot be undone.`)) return;
    try {
      await api(`/products/${product.id}`, { method: "DELETE" });
      setProducts((items) => items.filter((item) => item.id !== product.id));
      setMyProducts((items) => items.filter((item) => item.id !== product.id));
      setNotice("Product deleted.");
    } catch (error) {
      setNotice(error.message);
    }
  }

  function signOut() {
    clearToken();
    setAuthToken(null);
    setScreen("shop");
    setNotice("You have been signed out.");
  }

  return (
    <main>
      <header className="topbar">
        <button className="brand" onClick={() => setScreen("shop")}>Shop<span>Space</span></button>
        <nav>
          <button className={screen === "shop" ? "active" : ""} onClick={() => setScreen("shop")}>Shop</button>
          {token ? <>
            <button className={screen === "manage" ? "active" : ""} onClick={openManage}>My products</button>
            <button onClick={signOut}>Sign out</button>
          </> : <button onClick={() => setScreen("login")}>Seller sign in</button>}
        </nav>
      </header>

      {notice && <div className="notice" role="status">{notice}<button onClick={() => setNotice("")}>×</button></div>}

      {screen === "shop" && <Shop products={visibleProducts} categories={categories} query={query}
        selectedCategory={selectedCategory} loading={loading} onQuery={setQuery} onCategory={setSelectedCategory} />}
      {screen === "login" && <Login onLogin={(value) => { setToken(value); setAuthToken(value); setScreen("manage"); setNotice("Signed in successfully."); }} />}
      {screen === "manage" && <Manage products={myProducts} onCreate={openCreate} onEdit={openEdit} onDelete={deleteProduct} />}
      {screen === "editor" && <ProductEditor form={form} categories={categories} editing={editing}
        onChange={(key, value) => setForm((current) => ({ ...current, [key]: value }))}
      onCancel={openManage} onSubmit={saveProduct} />}
    </main>
  );
}

function Shop({ products, categories, query, selectedCategory, loading, onQuery, onCategory }) {
  return <>
    <section className="hero">
      <p className="eyebrow">CURATED FOR EVERYDAY LIFE</p>
      <h1>Find your next<br /><em>favourite thing.</em></h1>
      <p>Shop thoughtful products from independent sellers in one beautiful place.</p>
    </section>
    <section className="catalog" aria-label="Product catalog">
      <div className="filters">
        <label className="search"><span>⌕</span><input value={query} onChange={(event) => onQuery(event.target.value)} placeholder="Search products" /></label>
        <select value={selectedCategory} onChange={(event) => onCategory(event.target.value)} aria-label="Filter by category">
          <option value="all">All categories</option>
          {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
        </select>
      </div>
      {loading ? <p className="empty">Loading products…</p> : products.length === 0 ? <p className="empty">No products match your search.</p> :
        <div className="product-grid">{products.map((product) => <article className="product-card" key={product.id}>
          <div className="product-image">{product.imageUrl ? <img src={product.imageUrl} alt={product.name} /> : <span>✦</span>}</div>
          <div className="product-meta"><span>{product.category.name}</span><small>{product.seller.name}</small></div>
          <h2>{product.name}</h2><p>{product.description}</p><strong>{money(product.price)}</strong>
        </article>)}</div>}
    </section>
  </>;
}

function Login({ onLogin }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  async function submit(event) {
    event.preventDefault();
    try {
      const response = await api("/auth/login", { method: "POST", body: JSON.stringify({ email, password }) });
      onLogin(response.token);
    } catch (failure) { setError(failure.message); }
  }
  return <section className="auth-panel"><form onSubmit={submit}><p className="eyebrow">SELLER AREA</p><h1>Welcome back.</h1>
    <p>Sign in to add, edit, and manage your products.</p>{error && <p className="form-error">{error}</p>}
    <label>Email<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
    <label>Password<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></label>
    <button className="primary" type="submit">Sign in</button>
  </form></section>;
}

function Manage({ products, onCreate, onEdit, onDelete }) {
  return <section className="dashboard"><div className="section-heading"><div><p className="eyebrow">SELLER DASHBOARD</p><h1>Your products</h1></div><button className="primary" onClick={onCreate}>+ Add product</button></div>
    <p className="dashboard-note">You can edit or delete products that belong to your seller account.</p>
    <div className="manage-list">{products.length === 0 ? <p className="empty">You have not added any products yet.</p> : products.map((product) => <article key={product.id}><div><strong>{product.name}</strong><span>{product.category.name} · {money(product.price)}</span></div><div className="row-actions"><button onClick={() => onEdit(product)}>Edit</button><button className="danger" onClick={() => onDelete(product)}>Delete</button></div></article>)}</div>
  </section>;
}

function ProductEditor({ form, categories, editing, onChange, onCancel, onSubmit }) {
  return <section className="auth-panel editor"><form onSubmit={onSubmit}><p className="eyebrow">PRODUCT EDITOR</p><h1>{editing ? "Edit product" : "Add a product"}</h1>
    <div className="form-grid"><label>Name<input value={form.name} onChange={(e) => onChange("name", e.target.value)} required /></label><label>SKU<input value={form.sku} onChange={(e) => onChange("sku", e.target.value)} required /></label>
      <label>Price (₹)<input type="number" min="0.01" step="0.01" value={form.price} onChange={(e) => onChange("price", e.target.value)} required /></label><label>Category<select value={form.categoryId} onChange={(e) => onChange("categoryId", e.target.value)} required><option value="">Choose category</option>{categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
    </div><label>Description<textarea rows="4" value={form.description} onChange={(e) => onChange("description", e.target.value)} /></label><label>Image URL<input type="url" value={form.imageUrl} onChange={(e) => onChange("imageUrl", e.target.value)} placeholder="https://example.com/image.jpg" /></label>
    <div className="form-actions"><button type="button" onClick={onCancel}>Cancel</button><button className="primary" type="submit">{editing ? "Save changes" : "Create product"}</button></div>
  </form></section>;
}

export default App;

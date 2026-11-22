const API = '/api';
let token = '';

async function login() {
    const r = await fetch(API + '/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            email: document.getElementById('email').value,
            password: document.getElementById('password').value
        })
    });
    const d = await r.json();
    if (!r.ok) {
        document.getElementById('session').textContent = d.message || 'Помилка входу';
        return;
    }
    token = d.token;
    document.getElementById('session').textContent = `Увійшли: ${d.email || d.username || ''} (${d.role}, ${d.accountType})`;
    loadAds();
}

async function loadBrands() {
    try {
        const r = await fetch(API + '/brands');
        if (!r.ok) return;
        const brands = await r.json();
        const brandSelect = document.getElementById('brandId');
        brandSelect.innerHTML = brands.map(b => `<option value="${b.id}">${b.name}</option>`).join('');
        await updateModels();
    } catch (e) {
        console.error('Помилка завантаження марок:', e);
    }
}

async function updateModels() {
    const brandId = document.getElementById('brandId').value;
    const modelSelect = document.getElementById('modelId');
    if (!brandId) return;

    try {
        const r = await fetch(`${API}/brands/${brandId}/models`);
        if (r.ok) {
            const models = await r.json();
            modelSelect.innerHTML = models.map(m => `<option value="${m.id}">${m.name}</option>`).join('');
            return;
        }
    } catch (e) {}

    const fallbackModels = {
        '1': [{ id: 1, name: 'X5' }, { id: 2, name: '3 Series' }],
        '2': [{ id: 3, name: 'Lanos' }],
        '3': [{ id: 4, name: 'Camry' }]
    };
    const list = fallbackModels[brandId] || [{ id: 1, name: 'Model 1' }];
    modelSelect.innerHTML = list.map(m => `<option value="${m.id}">${m.name}</option>`).join('');
}

async function loadAds() {
    try {
        const r = await fetch(API + '/ads');
        const data = await r.json();
        const ads = Array.isArray(data) ? data : (data.content || []);
        document.querySelector('#ads').innerHTML = ads.map(a => {
            const brand = typeof a.brand === 'object' ? a.brand?.name : (a.brandName || a.brand || '');
            const model = typeof a.model === 'object' ? a.model?.name : (a.modelName || a.model || '');
            return `
        <article class="ad">
          ${a.imageUrl ? `<img src="${a.imageUrl}" alt="car">` : ''}
          <b>${brand} ${model} ${a.year ? `(${a.year})` : ''}</b><br>
          <b>${a.priceUsd ?? a.price ?? ''} USD</b> ${a.priceEur ? `· ${a.priceEur} EUR` : ''} ${a.priceUah ? `· ${a.priceUah} UAH` : ''}<br>
          <span>Регіон: ${a.region || ''} · Статус: ${a.status || ''}</span><br>
          <small>${a.description || ''}</small>
        </article>
      `;
        }).join('') || '<p>Оголошень ще немає</p>';
    } catch (e) {
        console.error('Помилка завантаження оголошень:', e);
    }
}

async function createAd() {
    const f = new FormData();
    ['brandId', 'modelId', 'year', 'price', 'currency', 'region', 'description'].forEach(id => {
        f.append(id, document.getElementById(id).value);
    });
    const file = document.getElementById('image').files[0];
    if (file) f.append('image', file);

    const r = await fetch(API + '/ads', {
        method: 'POST',
        headers: { Authorization: 'Bearer ' + token },
        body: f
    });
    const d = await r.json();
    document.getElementById('result').textContent = r.ok
        ? `Оголошення створено! Статус: ${d.status}`
        : (d.message || 'Помилка: спочатку натисніть "Увійти"');
    loadAds();
}

loadBrands();
loadAds();

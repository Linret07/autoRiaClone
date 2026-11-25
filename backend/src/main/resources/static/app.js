const API = '/api';
let token = '';
let currentUser = null;
let allAdsCache = [];
let selectedCondition = 'ALL';

/* Модальні вікна */
function openAuthModal(mode = 'login') {
    switchAuth(mode);
    document.getElementById('authModal').style.display = 'flex';
}

function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}

function closeModalOnBackdrop(e, id) {
    if (e.target.id === id) closeModal(id);
}

function handleSellClick() {
    if (!token || !currentUser) {
        openAuthModal('login');
        document.getElementById('session').textContent = 'Щоб додати авто, увійдіть як продавець (SELLER)';
        return;
    }
    const canCreate = String(currentUser.role).includes('SELLER') || String(currentUser.role).includes('ADMIN');
    if (!canCreate) {
        alert('Ви увійшли як Покупець (BUYER). Зареєструйте акаунт Продавця (SELLER), щоб виставляти авто.');
        return;
    }
    document.getElementById('result').textContent = '';
    document.getElementById('sellModal').style.display = 'flex';
}

/* Авторизація та реєстрація */
function switchAuth(mode) {
    const isLogin = mode === 'login';
    document.getElementById('authTitle').textContent = isLogin ? 'Вхід в особистий кабінет' : 'Реєстрація на AUTO.RIA';
    document.getElementById('loginForm').style.display = isLogin ? 'block' : 'none';
    document.getElementById('registerForm').style.display = isLogin ? 'none' : 'block';
    document.getElementById('session').textContent = '';
}

function applySession(d) {
    token = d.token;
    currentUser = d;
    closeModal('authModal');

    const badge = document.getElementById('userBadge');
    badge.textContent = `${d.email || d.username || ''} (${d.role}, ${d.accountType || 'BASIC'})`;
    badge.style.display = 'inline-block';

    document.getElementById('authOpenBtn').style.display = 'none';
    document.getElementById('logoutBtn').style.display = 'inline-block';
    loadAds();
}

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
        document.getElementById('session').textContent = d.message || 'Невірний email або пароль';
        return;
    }
    applySession(d);
}

async function register() {
    const r = await fetch(API + '/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            name: document.getElementById('regName').value,
            fullName: document.getElementById('regName').value,
            email: document.getElementById('regEmail').value,
            phone: document.getElementById('regPhone').value,
            password: document.getElementById('regPassword').value,
            role: document.getElementById('regRole').value
        })
    });
    const d = await r.json();
    if (!r.ok) {
        document.getElementById('session').textContent = d.message || 'Помилка реєстрації (перевірте дані)';
        return;
    }
    if (d.token) {
        applySession(d);
    } else {
        switchAuth('login');
        document.getElementById('session').textContent = 'Реєстрація успішна! Тепер увійдіть.';
    }
}

function logout() {
    token = '';
    currentUser = null;
    document.getElementById('userBadge').style.display = 'none';
    document.getElementById('authOpenBtn').style.display = 'inline-block';
    document.getElementById('logoutBtn').style.display = 'none';
}

/* Курси валют у шапці */
async function loadRates() {
    try {
        const r = await fetch(API + '/rates');
        if (!r.ok) return;
        const d = await r.json();
        if (d.USD_UAH && d.EUR_UAH) {
            document.getElementById('headerRates').textContent =
                `ПБ: $ ${Number(d.USD_UAH).toFixed(2)} | € ${Number(d.EUR_UAH).toFixed(2)}`;
        }
    } catch (e) {}
}

/* Марки та моделі (для форми додавання та фільтра пошуку) */
async function loadBrands() {
    try {
        const r = await fetch(API + '/brands');
        if (!r.ok) return;
        const brands = await r.json();

        const brandSelect = document.getElementById('brandId');
        brandSelect.innerHTML = brands.map(b => `<option value="${b.id}">${b.name}</option>`).join('');

        const filterBrand = document.getElementById('filterBrand');
        filterBrand.innerHTML = '<option value="">Марка (всі)</option>' +
            brands.map(b => `<option value="${b.id}" data-name="${b.name}">${b.name}</option>`).join('');

        await updateModels();
    } catch (e) {
        console.error('Помилка завантаження марок:', e);
    }
}

async function fetchModelsForBrand(brandId) {
    try {
        const r = await fetch(`${API}/brands/${brandId}/models`);
        if (r.ok) return await r.json();
    } catch (e) {}

    const fallback = {
        '1': [{ id: 1, name: 'X5' }, { id: 2, name: '3 Series' }],
        '2': [{ id: 3, name: 'Lanos' }],
        '3': [{ id: 4, name: 'Camry' }]
    };
    return fallback[brandId] || [];
}

async function updateModels() {
    const brandId = document.getElementById('brandId').value;
    const modelSelect = document.getElementById('modelId');
    if (!brandId) return;
    const models = await fetchModelsForBrand(brandId);
    modelSelect.innerHTML = models.map(m => `<option value="${m.id}">${m.name}</option>`).join('');
}

async function updateFilterModels() {
    const brandId = document.getElementById('filterBrand').value;
    const filterModel = document.getElementById('filterModel');
    if (!brandId) {
        filterModel.innerHTML = '<option value="">Модель (всі)</option>';
        return;
    }
    const models = await fetchModelsForBrand(brandId);
    filterModel.innerHTML = '<option value="">Модель (всі)</option>' +
        models.map(m => `<option value="${m.name}">${m.name}</option>`).join('');
}

/* Фільтрація та швидкопошук */
function setCondition(cond, btnEl) {
    selectedCondition = cond;
    document.querySelectorAll('.cond-tab').forEach(b => b.classList.remove('active'));
    if (btnEl) {
        btnEl.classList.add('active');
    } else {
        const match = document.querySelector(`.cond-tab[data-cond="${cond}"]`);
        if (match) match.classList.add('active');
    }
    applyFilters();
}

function filterVerified() {
    applyQuickFilter('VERIFIED');
}

function applyQuickFilter(type) {
    resetFilters(false);
    if (type === 'VERIFIED') {
        renderAds(allAdsCache.filter(a => (a.status || 'ACTIVE') === 'ACTIVE'));
        return;
    }
    if (type === 'UNDER_5000') {
        document.getElementById('filterMaxPrice').value = 5000;
    } else if (type === 'LVIV') {
        document.getElementById('filterRegion').value = 'LVIV';
    } else if (type === 'KYIV') {
        document.getElementById('filterRegion').value = 'KYIV';
    } else if (type === 'BMW') {
        const opt = Array.from(document.getElementById('filterBrand').options).find(o => o.text.toUpperCase() === 'BMW');
        if (opt) document.getElementById('filterBrand').value = opt.value;
    }
    applyFilters();
}

function resetFilters(shouldRender = true) {
    selectedCondition = 'ALL';
    document.querySelectorAll('.cond-tab').forEach(b => b.classList.toggle('active', b.dataset.cond === 'ALL'));
    document.getElementById('filterBrand').value = '';
    document.getElementById('filterModel').innerHTML = '<option value="">Модель (всі)</option>';
    document.getElementById('filterYearFrom').value = '';
    document.getElementById('filterMaxPrice').value = '';
    document.getElementById('filterRegion').value = '';
    document.getElementById('filterSort').value = 'NEWEST';
    if (shouldRender) renderAds(allAdsCache);
}

function applyFilters() {
    const brandSel = document.getElementById('filterBrand');
    const brandName = brandSel.value ? brandSel.options[brandSel.selectedIndex].text.toLowerCase() : '';
    const modelName = document.getElementById('filterModel').value.toLowerCase();
    const yearFrom = Number(document.getElementById('filterYearFrom').value) || 0;
    const maxPrice = Number(document.getElementById('filterMaxPrice').value) || 0;
    const region = document.getElementById('filterRegion').value;
    const sort = document.getElementById('filterSort').value;

    let list = allAdsCache.filter(a => {
        const b = (typeof a.brand === 'object' ? a.brand?.name : (a.brandName || a.brand || '')).toLowerCase();
        const m = (typeof a.model === 'object' ? a.model?.name : (a.modelName || a.model || '')).toLowerCase();
        const price = Number(a.priceUsd ?? a.price ?? 0);
        const year = Number(a.year ?? 0);

        if (selectedCondition === 'NEW' && year < 2024) return false;
        if (selectedCondition === 'USED' && year >= 2024) return false;
        if (brandName && b !== brandName) return false;
        if (modelName && m !== modelName) return false;
        if (yearFrom && year < yearFrom) return false;
        if (maxPrice && price > maxPrice) return false;
        if (region && a.region !== region) return false;
        return true;
    });

    if (sort === 'PRICE_ASC') list.sort((a, b) => (a.priceUsd ?? 0) - (b.priceUsd ?? 0));
    if (sort === 'PRICE_DESC') list.sort((a, b) => (b.priceUsd ?? 0) - (a.priceUsd ?? 0));
    if (sort === 'YEAR_DESC') list.sort((a, b) => (b.year ?? 0) - (a.year ?? 0));

    renderAds(list);
}

/* Відображення карток */
function toggleFav(btn) {
    btn.classList.toggle('liked');
    btn.textContent = btn.classList.contains('liked') ? '♥' : '♡';
}

function formatRelativeDate(createdAt) {
    if (!createdAt) return 'Сьогодні';
    const created = new Date(createdAt);
    const diffDays = Math.floor((Date.now() - created.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays <= 0) return 'Сьогодні';
    if (diffDays === 1) return 'День тому';
    if (diffDays < 5) return `${diffDays} дні тому`;
    return `${diffDays} днів тому`;
}

async function loadAds() {
    try {
        const r = await fetch(API + '/ads');
        const data = await r.json();
        allAdsCache = Array.isArray(data) ? data : (data.content || []);

        document.getElementById('totalCountBadge').textContent = allAdsCache.length;
        document.getElementById('verifiedCountBadge').textContent =
            allAdsCache.filter(a => (a.status || 'ACTIVE') === 'ACTIVE').length;

        renderAds(allAdsCache);
    } catch (e) {
        console.error('Помилка завантаження оголошень:', e);
    }
}

function renderAds(ads) {
    const regionNames = {
        KYIV: 'Київ',
        LVIV: 'Львів',
        ODESA: 'Одеса',
        KHARKIV: 'Харків',
        DNIPRO: 'Дніпро'
    };

    const regionPrefixes = {
        KYIV: 'AA',
        LVIV: 'BC',
        ODESA: 'BH',
        KHARKIV: 'AX',
        DNIPRO: 'AE'
    };

    document.querySelector('#ads').innerHTML = ads.map((a, idx) => {
        const brand = typeof a.brand === 'object' ? a.brand?.name : (a.brandName || a.brand || '');
        const model = typeof a.model === 'object' ? a.model?.name : (a.modelName || a.model || '');
        const status = a.status || 'ACTIVE';
        const isActive = status === 'ACTIVE';
        const regionLabel = regionNames[a.region] || a.region || 'Україна';
        const prefix = regionPrefixes[a.region] || 'KA';
        const plateNum = `${prefix} ${String(1024 + (a.id || idx) * 713).slice(0, 4)} UA`;

        const formatNum = n => n != null ? Number(n).toLocaleString('uk-UA') : '';

        return `
            <article class="ria-card">
                <div class="ria-photo-box">
                    ${a.imageUrl
            ? `<img src="${a.imageUrl}" alt="${brand} ${model}">`
            : `<div class="ria-no-photo">🚗 ${brand} ${model}<span>Фото не додано</span></div>`}

                    <div class="ria-watermark">
                        <span class="w-auto">auto</span><span class="w-ria">RIA</span>
                    </div>

                    <div class="ria-photo-bottom">
                        <div class="ria-badges-left">
                            <span class="ria-vin-badge ${isActive ? '' : 'warn'}">
                                ${isActive ? '✔ Перевірено' : '⚠ ' + status}
                            </span>
                            <span class="ria-plate">
                                <span class="ria-plate-ua">UA</span>
                                <span class="ria-plate-num">${plateNum}</span>
                            </span>
                        </div>
                        <button type="button" class="ria-fav-btn" onclick="toggleFav(this)" title="В обране">♡</button>
                    </div>
                </div>

                <div class="ria-content">
                    <h3 class="ria-title">${brand} ${model} ${a.year || ''}</h3>

                    <div class="ria-price-row">
                        <span class="ria-price-usd">${formatNum(a.priceUsd ?? a.price)} $</span>
                        ${a.priceUah ? `<span class="ria-price-sep">•</span><span class="ria-price-uah">${formatNum(a.priceUah)} грн</span>` : ''}
                        ${a.priceEur ? `<span class="ria-price-eur">(${formatNum(a.priceEur)} €)</span>` : ''}
                    </div>

                    <div class="ria-specs">
                        <div class="ria-spec-item">💱 Курс ПБ</div>
                        <div class="ria-spec-item">⚙️ Автомат / Мех.</div>
                        <div class="ria-spec-item">⛽ Бензин / Дизель</div>
                        <div class="ria-spec-item">📍 ${regionLabel}</div>
                    </div>

                    <div class="ria-desc-line">${a.description || 'Офіційний автомобіль, деталі за телефоном.'}</div>

                    <div class="ria-time">${formatRelativeDate(a.createdAt)}</div>
                </div>
            </article>
        `;
    }).join('') || '<p>За вибраними параметрами оголошень не знайдено.</p>';
}

/* Створення оголошення */
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
    if (r.ok) {
        closeModal('sellModal');
        loadAds();
    } else {
        document.getElementById('result').textContent = d.message || 'Помилка створення оголошення';
    }
}

loadRates();
loadBrands();
loadAds();

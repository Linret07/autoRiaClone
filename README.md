# AutoRia Clone

Навчальний MVP платформи продажу автомобілів. Backend написаний на Java/Spring Boot, frontend — vanilla JavaScript. Структура та підходи використані з навчальних проєктів `Java`, `JavaScript` і `springBoot`.

## Запуск

Потрібні Java 17 і Maven 3.9+.

```bash
cd backend
mvn spring-boot:run
```

Backend: `http://localhost:8080`, health check: `http://localhost:8080/actuator/health`.

В іншому терміналі можна запустити frontend:

```bash
cd frontend
python -m http.server 5500
```

Відкрити `http://localhost:5500`.

## Демо-користувачі

| username | password | роль | акаунт |
|---|---|---|---|
| buyer | buyer | BUYER | BASIC |
| seller | seller | SELLER | BASIC |
| premium | premium | SELLER | PREMIUM |
| manager | manager | MANAGER | PREMIUM |
| admin | admin | ADMIN | PREMIUM |

## Зображення

Картинка передається як `multipart/form-data` у полі `image`. Backend:

- приймає тільки JPG/JPEG/PNG/WEBP;
- генерує UUID-ім'я, тому оригінальне ім'я не може перезаписати інший файл;
- зберігає файл у `uploads` або в каталозі з `UPLOAD_DIR`;
- повертає URL `/uploads/{uuid}.ext`;
- перевіряє нормалізований шлях перед записом;
- зберігає ім'я, URL, тип і оригінальне ім'я разом з оголошенням.

Для AWS наступним кроком сховище можна замінити на S3 без зміни API: винести `ImageStorage` в окремий адаптер і підключити S3 implementation.

## Реалізовано

- ролі BUYER, SELLER, MANAGER, ADMIN;
- розширювана перевірка доступу через ролі, підготовлена до permission-моделі автосалонів;
- BASIC-продавець може мати одне активне оголошення, PREMIUM — без цього обмеження;
- марки та моделі через `/api/brands`;
- USD/EUR/UAH у моделі оголошення, щоденне оновлення курсу з API ПриватБанку, збереження початкової ціни, курсу й перерахованих значень;
- автоматична модерація тестовим словником, статуси `ACTIVE`, `NEEDS_EDIT`, `INACTIVE_REVIEW`;
- максимум три редагування оголошення;
- статистика для PREMIUM;
- Postman collection з основним flow.

Поточний MVP використовує in-memory репозиторій, щоб перевірка запускалася без окремої БД. Для production/AWS його слід замінити на PostgreSQL/MongoDB repository, а сесійні токени — на JWT/Redis. Це свідомий адаптерний шар для першої ітерації.

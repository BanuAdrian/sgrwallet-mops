# SGRWallet

Spring Boot application with PostgreSQL and Flyway migrations.

The database model is represented by JPA entities in
`src/main/java/ro/mops/sgrwallet/model`. Flyway keeps the schema versioned and
reproducible for every developer.

## Run locally

From the project directory:

```powershell
Copy-Item .env.example .env
docker compose up -d postgres
mvn spring-boot:run
```

PostgreSQL runs in Docker on `127.0.0.1:5433`.
The application runs on `http://localhost:8080`.

To run the React frontend, open a second terminal:

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```

Open `http://localhost:5173`. The frontend uses `VITE_API_URL` from
`frontend/.env` and defaults to `http://localhost:8080/api`.

## VS Code

Open the project root in VS Code. Use **Run and Debug** and select
**Debug Spring Boot + frontend** to start the frontend and debug Spring Boot.
Alternatively, open **Terminal → Run Task** and select **start-full-stack**.

Flyway creates the database tables automatically when the application starts.

No `.env` changes are required for local development. If you change the
password, update both `POSTGRES_PASSWORD` and `DB_PASSWORD` to the same value.
For a completely new database after changing credentials:

```powershell
docker compose down -v
docker compose up -d postgres
mvn spring-boot:run
```

## Stop

```powershell
docker compose down
```

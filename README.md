# TEApp — Agenda Semanal para Niños con TEA

Aplicación web y móvil (Android) full-stack para organizar la rutina semanal de niños con Trastorno del Espectro Autista (TEA). Los padres o tutores crean y gestionan la agenda; los niños la visualizan en un **modo participante** accesible, con pictogramas ARASAAC, temporizadores visuales y síntesis de voz.

---

## Características principales

- **Agenda semanal** organizada por día y franja horaria (Mañana / Tarde / Noche)
- **Modo participante** (solo lectura para el niño): muestra "Ahora → Después", pictogramas y widget del clima
- **Completitud de actividades**: los niños marcan actividades como hechas; se pueden resetear por semana
- **Temporizador visual** para actividades con duración definida, con opción de pausa
- **Pasos detallados**: cada actividad puede tener sub-pasos con pictogramas propios
- **Catálogo de actividades**: actividades predefinidas del sistema + personalizadas por padre
- **Pictogramas ARASAAC**: búsqueda y selección desde la API pública de ARASAAC
- **Modo terapeuta**: los terapeutas supervisan las agendas de sus padres vinculados (solo lectura)
- **Vinculación terapeuta-padre** mediante código de invitación
- **Recordar dispositivo**: opción de sesión persistente (localStorage) o temporal (sessionStorage)
- **Recuperación de contraseña** por email
- **Avatares** para usuarios y participantes (foto o color con inicial)
- **Paleta autism-friendly**: colores suaves, sin rojo, alto contraste
- **Android nativo** via Capacitor (APK generado con Android Studio)

---

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Spring Boot 3.2.3 · Java 17 · Spring Security + JWT (jjwt 0.12.5) · Spring Data JPA · Flyway |
| Base de datos | PostgreSQL 16 |
| Frontend | Angular 17 (standalone) · Angular Material 17 · CDK Drag-Drop · Capacitor 6 |
| API Docs | Swagger UI / SpringDoc OpenAPI 2 |
| Docs Frontend | Compodoc |

---

## Requisitos previos

- Java 17
- Maven 3.9+
- Node.js 20+ y npm
- PostgreSQL 16
- Android Studio (solo si se quiere generar APK)

---

## Instalación y arranque

### 1. Base de datos

```sql
CREATE DATABASE teapp_db;
-- Usuario y contraseña por defecto: postgres / postgres
-- Ajustar en teapp-backend/src/main/resources/application.yml si es necesario
```

### 2. Backend

```bash
cd teapp-backend
mvn spring-boot:run
```

Arranca en `http://localhost:8080`. Las migraciones Flyway se ejecutan automáticamente (V1–V24).

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### 3. Frontend (web)

```bash
cd teapp-frontend
npm install
npm start
```

Abre en `http://localhost:4200`

### 4. Android (APK)

```bash
cd teapp-frontend
npm run build
npx cap sync android
# Luego abrir teapp-frontend/android/ en Android Studio y ejecutar
```

> El emulador Android usa `10.0.2.2:8080` para acceder al backend local.
> El CORS y la URL del API para Android están configurados en `environment.android.ts`.

### 5. Documentación del frontend (Compodoc)

```bash
cd teapp-frontend
npm run compodoc
# Se genera en teapp-frontend/docs/index.html
```

---

## Estructura del proyecto

```
TEApp/
├── teapp-backend/
│   └── src/main/
│       ├── java/com/teapp/
│       │   ├── config/         # SecurityConfig, OpenApiConfig
│       │   ├── controller/     # AuthController, ChildController, ActivityController,
│       │   │                   # ScheduleController, CompletionController,
│       │   │                   # TherapistController, UserController, PasswordResetController
│       │   ├── dto/            # Records de request/response
│       │   ├── entity/         # User, Child, Activity, ActivityStep,
│       │   │                   # ScheduleEntry, Completion, ParentTherapist
│       │   ├── repository/     # Interfaces JpaRepository
│       │   ├── security/       # JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl
│       │   └── service/        # AuthService, ChildService, ActivityService,
│       │                       # ScheduleService, CompletionService, TherapistService,
│       │                       # UserService, PasswordResetService
│       └── resources/
│           ├── application.yml
│           └── db/migration/   # V1–V24 migraciones Flyway
│
├── teapp-frontend/
│   ├── android/                # Proyecto Android (Capacitor)
│   └── src/app/
│       ├── core/
│       │   ├── guards/         # authGuard, guestGuard
│       │   ├── interceptors/   # JwtInterceptor
│       │   ├── models/         # Child, Activity, ScheduleEntry, User…
│       │   └── services/       # AuthService, ChildService, ActivityService,
│       │                       # ScheduleService, TherapistService, UserService,
│       │                       # WeatherService, SpeechService, ArasaacService
│       ├── shared/
│       │   └── components/     # HeaderComponent, ChildCardComponent, ActivityChipComponent,
│       │                       # ConfirmDialog, AvatarPickerDialog, ChangePasswordDialog,
│       │                       # VisualTimerDialog, StepViewerDialog
│       └── features/
│           ├── auth/           # LoginComponent, ForgotPasswordComponent
│           ├── dashboard/      # DashboardComponent
│           ├── children/       # ChildListComponent, ChildFormComponent
│           ├── activities/     # ActivityCatalogComponent, ActivityFormDialog,
│           │                   # ActivityStepsDialog
│           ├── agenda/         # AgendaViewComponent, TimeSlotColumnComponent,
│           │                   # ActivityPickerDialog, EntrySettingsDialog,
│           │                   # KidExitDialogComponent
│           └── therapist/      # TherapistDashboardComponent
│
└── documentacion/              # Documentación académica y técnica (HTML)
    ├── TEApp - Walkthrough del Código.html
    ├── TEApp - Diagrama de Clases.html
    ├── TEApp - Diagrama ER.html
    └── …
```

---

## API REST — Endpoints

> Todos los endpoints (excepto `/api/auth/**` y `/api/password-reset/**`) requieren el header:
> `Authorization: Bearer <token>`

### Autenticación

| Método | URL | Descripción |
|--------|-----|-------------|
| POST | `/api/auth/register` | Registro de padre o terapeuta |
| POST | `/api/auth/login` | Login, retorna JWT + datos del usuario |

### Recuperación de contraseña

| Método | URL | Descripción |
|--------|-----|-------------|
| POST | `/api/password-reset/request` | Envía email con enlace de reset |
| POST | `/api/password-reset/reset` | Cambia la contraseña con el token |

### Participantes (niños)

| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/children` | Lista todos los participantes del padre |
| POST | `/api/children` | Crea un nuevo participante |
| GET | `/api/children/{id}` | Obtiene un participante |
| PUT | `/api/children/{id}` | Actualiza datos del participante |
| DELETE | `/api/children/{id}` | Elimina participante y su agenda |
| PUT | `/api/children/{id}/avatar` | Actualiza avatar (base64) |

### Actividades

| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/activities` | Catálogo completo (predefinidas + propias) |
| GET | `/api/activities?category=HYGIENE` | Filtrado por categoría |
| GET | `/api/activities/predefined` | Solo actividades del sistema |
| POST | `/api/activities` | Crea actividad personalizada |
| PUT | `/api/activities/{id}` | Actualiza actividad |
| DELETE | `/api/activities/{id}` | Elimina actividad |
| GET | `/api/activities/{id}/steps` | Obtiene pasos de la actividad |
| PUT | `/api/activities/{id}/steps` | Guarda/reemplaza todos los pasos |

### Agenda semanal

| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/children/{id}/schedule` | Agenda semanal completa |
| POST | `/api/children/{id}/schedule` | Agrega una entrada a la agenda |
| PUT | `/api/children/{id}/schedule/{entryId}` | Actualiza una entrada |
| DELETE | `/api/children/{id}/schedule/{entryId}` | Elimina una entrada |

### Completitud de actividades

| Método | URL | Descripción |
|--------|-----|-------------|
| POST | `/api/children/{id}/schedule/{entryId}/completions` | Marca como completada |
| DELETE | `/api/children/{id}/schedule/{entryId}/completions` | Desmarca |
| DELETE | `/api/children/{id}/completions/current-week` | Resetea toda la semana actual |

### Terapeutas

| Método | URL | Descripción |
|--------|-----|-------------|
| POST | `/api/therapist/link` | El padre se vincula a un terapeuta (por código) |
| DELETE | `/api/therapist/link/{therapistId}` | El padre se desvincula |
| GET | `/api/therapist/my-therapists` | Terapeutas del padre autenticado |
| GET | `/api/therapist/supervised` | Padres del terapeuta autenticado |
| GET | `/api/therapist/supervised/{parentId}/children` | Niños de un padre supervisado |
| GET | `/api/therapist/supervised/{parentId}/children/{childId}/schedule` | Agenda (solo lectura) |

### Usuario

| Método | URL | Descripción |
|--------|-----|-------------|
| PUT | `/api/users/me/avatar` | Actualiza foto de perfil |
| PUT | `/api/users/me/password` | Cambia contraseña |

---

## Categorías de actividades

| Clave | Etiqueta |
|-------|----------|
| `HYGIENE` | Higiene personal |
| `MEAL` | Comidas |
| `EDUCATION` | Educación |
| `PLAY` | Juego |
| `THERAPY` | Terapia |
| `CHORES` | Tareas del hogar |
| `SOCIAL` | Social |
| `REST` | Descanso |
| `SPECIAL_EVENT` | Evento especial |
| `OTHER` | Otro |

---

## Paleta de colores (autism-friendly)

| Nombre | Hex | Uso |
|--------|-----|-----|
| Azul cielo | `#A8D8EA` | Color primario, header, botones |
| Verde suave | `#B8E0C8` | Éxito, categoría Educación |
| Amarillo pastel | `#FAF0BE` | Resaltados, categoría Comidas |
| Lavanda | `#C9B8E8` | Categoría Juego |
| Melocotón | `#F9D8C0` | Actividades personalizadas |
| Fondo mañana | `#FFF9E6` | Slot Mañana |
| Fondo tarde | `#F0FAF4` | Slot Tarde |
| Fondo noche | `#F0EDF8` | Slot Noche |

> No se utiliza el rojo en ningún elemento de la interfaz por recomendaciones de diseño autism-friendly.

---

## Migraciones de base de datos (Flyway)

| Versión | Descripción |
|---------|-------------|
| V1 | Tablas base: users, children, activities, schedule_entries |
| V2 | Tabla completions |
| V3 | Tabla parent_therapist (vinculación) |
| V4 | Tabla password_reset_tokens |
| V5 | ~30 actividades predefinidas del catálogo inicial |
| V6–V22 | Mejoras de esquema: pasos, avatares, colores, pictogramas, orden |
| V23 | Corrección de pasos de actividades |
| V24 | Corrección pictograma zapatillas (ID ARASAAC 2621) |

---

## Seguridad

- **JWT stateless**: token firmado con HMAC-SHA256, expiración 7 días
- **Almacenamiento del token**: `localStorage` (si "Recordar dispositivo" está activo) o `sessionStorage` (sesión temporal)
- **Verificación de expiración**: el cliente decodifica el JWT localmente sin llamada al backend
- **Contraseñas**: hasheadas con BCrypt
- **CORS**: habilitado para `localhost:4200` y `10.0.2.2:4200` (emulador Android)
- **Rutas protegidas**: `authGuard` en `/app/**`; `guestGuard` en `/login` (redirige al dashboard si ya hay sesión)

---

## Roles de usuario

| Rol | Capacidades |
|-----|-------------|
| `PARENT` | Gestiona participantes, agenda, actividades personalizadas; puede vincularse a un terapeuta |
| `THERAPIST` | Supervisa agendas de los participantes de sus padres vinculados (solo lectura); genera código de invitación |

---

## Licencia

Proyecto académico — Escuela Da Vinci.

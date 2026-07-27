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
- **App Android nativa** en Java (proyecto `teapp-android/`, consume la misma API REST)

---

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Spring Boot 3.2.3 · Java 17 · Spring Security + JWT (jjwt 0.12.5) · Spring Data JPA · Flyway |
| Base de datos | PostgreSQL 16 |
| Frontend | Angular 17 (standalone) · Angular Material 17 · CDK Drag-Drop |
| Android  | App nativa Java · Retrofit 2 · Material Components · ViewBinding |
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

Arranca en `http://localhost:8080`. La migración Flyway se ejecuta automáticamente al iniciar.

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### 3. Frontend (web)

```bash
cd teapp-frontend
npm install
npm start
```

Abre en `http://localhost:4200`

### 4. Android (APK nativo)

1. Abrí `teapp-android/` en Android Studio
2. Editá `app/src/main/java/com/teapp/util/Constants.java` y reemplazá la IP con la de tu máquina en la red local (o dejá `10.0.2.2` para el emulador AVD)
3. Ejecutá **Run** en Android Studio

> El emulador AVD accede al backend local mediante `10.0.2.2:8080`.

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
│           └── db/migration/   # V1__init.sql — schema completo + datos iniciales
│
├── teapp-android/              # App Android nativa (Java)
│   └── app/src/main/
│       ├── java/com/teapp/
│       │   ├── api/            # ApiService (Retrofit) — todos los endpoints del backend
│       │   ├── model/          # Modelos: Child, ActivityItem, ScheduleEntry, TherapistInfo…
│       │   ├── ui/
│       │   │   ├── auth/       # LoginActivity, ForgotPasswordActivity, ChangePasswordActivity
│       │   │   ├── dashboard/  # DashboardActivity
│       │   │   ├── child/      # ChildFormActivity (con avatar foto + selector de color)
│       │   │   ├── agenda/     # AgendaActivity, AgendaDayFragment, ActivityPickerActivity,
│       │   │   │               # KidModeActivity (modo niño con pasos, timer, clima, TTS)
│       │   │   ├── activity/   # ActivityCatalogActivity, ActivityFormActivity (CRUD + ARASAAC)
│       │   │   └── therapist/  # TherapistDashboardActivity, SupervisedAgendaActivity,
│       │   │                   # ManageTherapistActivity
│       │   └── util/           # Constants, PrefsManager, WeatherHelper (Open-Meteo)
│       └── res/                # Layouts, colores sincronizados con Angular, temas
│
├── teapp-frontend/
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
| `HYGIENE` | Higiene |
| `MEAL` | Comidas |
| `EDUCATION` | Educación |
| `PLAY` | Juego |
| `THERAPY` | Terapia |
| `REST` | Descanso |
| `OUTDOOR` | Aire libre |
| `CUSTOM` | Personalizada |
| `SPECIAL_EVENT` | Evento especial |

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

- `V1__init.sql` — schema completo y datos iniciales
- `V2__add_soft_delete_and_profile.sql` — borrado lógico y campos de perfil
- `V3__seed_demo_data.sql` — datos de demostración para la evaluación

Contenido:

- Tablas: `users`, `children`, `activities`, `activity_steps`, `schedule_entries`, `activity_completions`, `therapist_parent_links`, `password_reset_tokens`
- ~30 actividades predefinidas con pictogramas ARASAAC
- Pasos detallados para actividades de higiene y vestimenta

### Usuarios de demostración

| Email | Contraseña | Rol |
|-------|-----------|-----|
| `padre@teapp.com` | `Test1234!` | Padre — con la participante Sofía y su agenda semanal cargada |
| `terapeuta@teapp.com` | `Test1234!` | Terapeuta — vinculado al padre, acceso de solo lectura |

---

## Seguridad

- **JWT stateless**: token firmado con HMAC-SHA256, expiración 7 días
- **Almacenamiento del token**: `localStorage` (si "Recordar dispositivo" está activo) o `sessionStorage` (sesión temporal)
- **Verificación de expiración**: el cliente decodifica el JWT localmente sin llamada al backend
- **Contraseñas**: hasheadas con BCrypt
- **CORS**: habilitado para `localhost:4200` (Angular), `10.0.2.2` (emulador AVD) y la IP de red local (celular físico)
- **Rutas protegidas**: `authGuard` en `/app/**`; `guestGuard` en `/login` (redirige al dashboard si ya hay sesión)

---

## Roles de usuario

| Rol | Capacidades |
|-----|-------------|
| `PARENT` | Gestiona participantes, agenda, actividades personalizadas; puede vincularse a un terapeuta |
| `THERAPIST` | Supervisa agendas de los participantes de sus padres vinculados (solo lectura); genera código de invitación |

---

## Equipo

| Integrante | Rol |
|------------|-----|
| Espina Fernando | Desarrollo |

Proyecto académico — Escuela Da Vinci.

---

## Licencia

Proyecto académico — Escuela Da Vinci.

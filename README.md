# TA Recruitment System

> **BUPT International School Teaching Assistant Recruitment System**
>
> EBU6304 – Software Engineering Group Project

---

## 1. Project Overview

This is a **standalone Java Swing desktop application** developed for BUPT International School to streamline the Teaching Assistant (TA) recruitment process. The system replaces manual form-based and Excel-based workflows with a unified software platform.

The project is developed using **Agile methodologies** (Scrum) with iterative delivery across multiple sprints.

### Key Objectives

- Provide a centralized platform for TA recruitment management
- Support three distinct user roles: **TA**, **Module Organiser (MO)**, and **Admin**
- Store all data in lightweight text-based formats (JSON) — no database required
- Deliver a clean, modern, and intuitive GUI using Java Swing

---

## 2. Features

### 2.1 User Authentication & Registration

| Feature               | Description                                              |
|-----------------------|----------------------------------------------------------|
| Login                 | Authenticate with username and password                  |
| Register              | Create a new account with role selection (TA / MO / Admin) |
| Input Validation      | Password confirmation, minimum length, required fields   |
| Duplicate Prevention  | Username uniqueness check during registration            |

### 2.2 Teaching Assistant (TA) Features

| Feature               | Description                                              |
|-----------------------|----------------------------------------------------------|
| Profile Management    | Edit name, email, phone, department, and skills          |
| CV Upload             | Upload CV file (copied to `data/cv/` directory)          |
| Browse Jobs           | View all open job postings in a sortable table           |
| View Job Details      | See full job description, required skills, and deadlines |
| Apply for Jobs        | Submit application with optional cover letter            |
| My Applications       | Track all submitted applications and their status        |
| Withdraw Application  | Withdraw a pending application                           |

### 2.3 Module Organiser (MO) Features

| Feature               | Description                                              |
|-----------------------|----------------------------------------------------------|
| Post New Job          | Create job postings with title, module, type, skills, positions, deadline |
| My Posted Jobs        | View and manage all jobs posted by the MO                |
| Close / Delete Job    | Close a job to stop accepting applications, or delete it |
| Review Applicants     | View all applicants for a selected job                   |
| Accept / Reject       | Accept or reject individual applicants with optional notes |

### 2.4 Admin Features

| Feature               | Description                                              |
|-----------------------|----------------------------------------------------------|
| TA Workload Overview  | View all TAs with accepted job count and pending applications |
| View TA Details       | Drill into individual TA profiles and application history |
| Manage Users          | View and delete user accounts                            |
| All Jobs Overview     | View every job posting across the system                 |
| All Applications      | View every application with reviewer information         |

---

## 3. Tech Stack & Dependencies

| Component        | Technology                  | Version   |
|------------------|-----------------------------|-----------|
| Language         | Java                        | 11+       |
| GUI Framework    | Java Swing + FlatLaf        | 3.2.5     |
| JSON Processing  | Google Gson                 | 2.10.1    |
| Build Tool       | Apache Maven                | 3.6+      |
| Unit Testing     | JUnit                       | 4.13.2    |
| Data Storage     | JSON flat files             | —         |

> All dependencies are managed via Maven (`pom.xml`). See [DEPENDENCIES.md](DEPENDENCIES.md) for a detailed breakdown.

---

## 4. Project Structure

```
ta-recruitment-system/
│
├── pom.xml                                          # Maven build configuration
├── README.md                                        # This file
├── DEPENDENCIES.md                                  # Dependency documentation
│
├── data/                                            # Runtime data (JSON files)
│   ├── users.json                                   # User accounts & profiles
│   ├── jobs.json                                    # Job postings
│   ├── applications.json                            # Job applications
│   └── cv/                                          # Uploaded CV files
│
├── src/
│   ├── main/java/com/recruitment/
│   │   │
│   │   ├── Main.java                                # Application entry point
│   │   │
│   │   ├── model/                                   # Data model layer (POJO)
│   │   │   ├── User.java                            #   User entity (TA/MO/Admin)
│   │   │   ├── Job.java                             #   Job posting entity
│   │   │   └── Application.java                     #   Job application entity
│   │   │
│   │   ├── service/                                 # Business logic layer
│   │   │   ├── UserService.java                     #   User CRUD & authentication
│   │   │   ├── JobService.java                      #   Job CRUD & queries
│   │   │   └── ApplicationService.java              #   Application CRUD & workflow
│   │   │
│   │   ├── util/                                    # Utility classes
│   │   │   ├── JsonUtil.java                        #   JSON file read/write (Gson)
│   │   │   └── IDGenerator.java                     #   UUID-based ID generation
│   │   │
│   │   └── view/                                    # GUI layer (Swing)
│   │       ├── LoginFrame.java                      #   Login window
│   │       ├── RegisterDialog.java                  #   Registration dialog
│   │       ├── TADashboard.java                     #   TA main dashboard
│   │       ├── MODashboard.java                     #   MO main dashboard
│   │       └── AdminDashboard.java                  #   Admin main dashboard
│   │
│   └── test/java/com/recruitment/                   # Unit tests
│       └── service/
│           ├── UserServiceTest.java                 #   5 tests for UserService
│           ├── JobServiceTest.java                  #   5 tests for JobService
│           └── ApplicationServiceTest.java          #   7 tests for ApplicationService
│
└── target/                                          # Build output (generated)
    └── ta-recruitment-system-1.0-SNAPSHOT.jar        # Executable JAR
```

---

## 5. Architecture

The application follows a **layered architecture** (MVC-like):

```
┌──────────────────────────────────────────────┐
│                  View Layer                  │
│  (LoginFrame, TADashboard, MODashboard,      │
│   AdminDashboard, RegisterDialog)            │
├──────────────────────────────────────────────┤
│               Service Layer                  │
│  (UserService, JobService,                   │
│   ApplicationService)                        │
├──────────────────────────────────────────────┤
│                Model Layer                   │
│  (User, Job, Application)                    │
├──────────────────────────────────────────────┤
│            Data Access Layer                 │
│  (JsonUtil → JSON files in data/)            │
└──────────────────────────────────────────────┘
```

- **View**: Swing-based GUI components, one dashboard per role
- **Service**: Business logic, data validation, CRUD operations
- **Model**: Plain Java objects (POJOs) with getters/setters
- **Data Access**: `JsonUtil` reads/writes JSON files using Gson

---

## 6. Setup & Installation

### 6.1 Prerequisites

| Requirement       | Minimum Version | Check Command         |
|--------------------|----------------|-----------------------|
| Java JDK           | 11             | `java -version`       |
| Apache Maven        | 3.6            | `mvn -version`        |
| Git (optional)      | 2.0            | `git --version`       |

### 6.2 Clone the Repository

```bash
git clone git@github.com:alkalik/SE.git
cd SE
```

### 6.3 Build the Project

```bash
mvn clean package
```

This will:
1. Compile all Java source files
2. Run all 17 unit tests
3. Package the application into an executable fat JAR (with all dependencies included)

### 6.4 Run the Application

**Option A — Run the packaged JAR:**
```bash
java -jar target/ta-recruitment-system-1.0-SNAPSHOT.jar
```

**Option B — Run directly via Maven:**
```bash
mvn compile exec:java -Dexec.mainClass="com.recruitment.Main"
```

### 6.5 Run Tests Only

```bash
mvn test
```

Expected output: `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`

---

## 7. User Guide

### 7.1 Login

Launch the application and you will see the login screen. Enter your username and password, then click **Login**. To create a new account, click **Register**.

### 7.2 Registration

Fill in all required fields:
- **Username** — must be unique
- **Password** — minimum 4 characters, confirmed by re-entry
- **Full Name**, **Email** — required
- **Role** — select TA, MO, or Admin

### 7.3 TA Workflow

1. **My Profile** tab: Fill in your skills (comma-separated, e.g., `Java, Python, Agile`), phone, department, and upload your CV.
2. **Browse Jobs** tab: Click **Refresh** to load available positions. Select a job and click **View Details** to see full information, or **Apply for Selected Job** to submit an application.
3. **My Applications** tab: Track the status of all your applications (Pending / Accepted / Rejected / Withdrawn). You can **Withdraw** a pending application.

### 7.4 MO Workflow

1. **Post New Job** tab: Fill in job title, module name, job type, description, required skills, number of positions, semester, and deadline. Click **Post Job**.
2. **My Posted Jobs** tab: View all your posted jobs. You can **Close Job** (stop accepting applications) or **Delete Job**.
3. **Review Applicants** tab: Select a job from the dropdown, then view all applicants. Click **Accept** or **Reject** for each applicant.

### 7.5 Admin Workflow

1. **TA Workload Overview** tab: See all TAs with their accepted job count and pending application count. Click **View TA Details** for a detailed profile.
2. **All Users** tab: Manage all registered users. You can **Delete User** (cannot delete yourself).
3. **All Jobs** / **All Applications** tabs: Monitor the entire system's job postings and applications.

---

## 8. Default Test Accounts

| Username | Password   | Role   | Name            |
|----------|------------|--------|-----------------|
| `admin`  | `admin123` | Admin  | System Admin    |
| `mo1`    | `mo123`    | MO     | Dr. Zhang Wei   |
| `mo2`    | `mo123`    | MO     | Dr. Li Ming     |
| `ta1`    | `ta123`    | TA     | Wang Xiaoming   |
| `ta2`    | `ta123`    | TA     | Chen Lei        |

---

## 9. Data Storage

All data is persisted in **JSON format** in the `data/` directory:

| File                  | Description                                    |
|-----------------------|------------------------------------------------|
| `data/users.json`     | User accounts, profiles, skills, CV paths      |
| `data/jobs.json`      | Job postings with requirements and status       |
| `data/applications.json` | Applications with status and review notes    |
| `data/cv/`            | Uploaded CV files                              |

Data files are created automatically on first run if they do not exist.

---

## 10. Testing

The project includes **17 unit tests** across 3 test classes:

| Test Class                  | Tests | Coverage                                          |
|-----------------------------|-------|---------------------------------------------------|
| `UserServiceTest`           | 5     | Register, authenticate, duplicate check, update, delete |
| `JobServiceTest`            | 5     | Create, query open jobs, close, query by MO, delete |
| `ApplicationServiceTest`    | 7     | Apply, duplicate check, accept, reject, withdraw, query, count |

Run tests:
```bash
mvn test
```

---

## 11. Development Notes

### Agile Methodology
- Developed using **Scrum** with iterative sprints
- Features prioritised by customer value and feasibility
- Each iteration produces a working software increment

### Design Principles
- **Separation of Concerns**: Model / Service / View layers
- **Single Responsibility**: Each class has a clear, focused purpose
- **Data Independence**: JSON-based storage, easily swappable
- **Extensibility**: New roles, job types, or features can be added with minimal changes

### Version Control
- Git-based workflow with feature branches
- Each team member contributes via their own branch
- Pull requests merged to `main` branch

---

## 12. License

This project is developed as coursework for **EBU6304 Software Engineering** at BUPT International School. For academic use only.

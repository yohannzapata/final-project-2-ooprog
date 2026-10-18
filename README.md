# Hospital Management System

A Java console-based Hospital Management System for a small hospital. It manages patients, doctors,
appointments, medical records, rooms, admissions and bills, with a role-based menu for Admins,
Doctors and Patients.

**Group 2 – C2A:** Dapito, Lanto, Mellomida, Zapata

## Requirements

- Java 11 or newer (tested on JDK 17)

## How to run

From the project folder:

```bash
javac -d out $(find src -name "*.java")
java -cp out Main
```

On Windows (PowerShell):

```powershell
javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp out Main
```

You can also open the folder in IntelliJ IDEA or VS Code and run `src/Main.java`.

Data is saved in the `data/` folder (created on first run) so records are kept between sessions.

### Default account

On the first run a default admin account is created:

| Username | Password    |
|----------|-------------|
| `admin`  | `Admin1234` |

You will be asked to change the password after the first login. Accounts created by the admin
(doctors and walk-in patients) also get a temporary password that must be changed on first login.

## Features

| Role    | What they can do |
|---------|------------------|
| Admin   | Manage patients, doctors, rooms, admissions and bills; view all appointments and book, reschedule or cancel them for patients; apply discounts; mark bills as paid |
| Doctor  | View today's/upcoming appointments, mark them completed or no-show, view own patients, add/view/update/delete medical records |
| Patient | Register, update profile, view doctors, book/reschedule/cancel own appointments, view own medical records, admissions and bills |

Type `back` at any prompt to cancel the current action.

### Main rules and validations

- **Login:** 3 failed attempts return to the main menu. Passwords are stored as salted SHA-256 hashes.
- **Accounts:** usernames are 4–20 characters and unique; passwords need 8+ characters with an uppercase
  letter, a lowercase letter and a number, and must be typed twice.
- **Patients:** names (letters, spaces, hyphens, apostrophes, periods), birthdate not in the future and age
  0–120, PH mobile numbers (`09XXXXXXXXX`, `+63` accepted), optional email, Senior Citizen ID (60+ only),
  PWD ID (`RR-PPMM-BBB-NNNNNNN`) and PhilHealth number (12 digits). The same person (name + birthdate)
  cannot be registered twice.
- **Doctors:** 7-digit PRC license (unique), consultation fee > 0, available days (e.g. `MON-FRI`) and
  clinic hours. A schedule change that would leave booked patients outside the new hours is rejected.
- **Appointments:** 30-minute slots within the doctor's clinic days and hours, not in the past and at most
  90 days ahead. No double booking of a doctor, and a patient cannot have two appointments at the same time
  or two with the same doctor on the same day. Only past appointments can be marked completed/no-show.
- **Medical records:** one record per completed appointment, written by the doctor of that appointment.
- **Rooms and admissions:** only available rooms can be used, one active admission per patient, discharge
  date cannot be before the admission date. A same-day discharge is billed as 1 day.
- **Billing:** consultation (fee at time of booking) or admission (daily rate × days). Regular, Senior
  Citizen (20%), PWD (20%) or PhilHealth (fixed deduction, not more than the bill). Only discounts the
  patient is eligible for are offered. Paid bills cannot be changed or deleted.
- **Confirmation:** delete, cancel and discharge actions ask `Are you sure? (Y/N)`.

## OOP concepts and design patterns

- **Encapsulation:** private fields with getters/setters; setters reject invalid values.
- **Abstraction:** abstract `User` class and the `BillingStrategy` interface.
- **Inheritance:** `Admin`, `Doctor` and `Patient` extend `User`; custom exceptions extend `Exception`;
  every menu extends the abstract `Menu` class.
- **Polymorphism:** `showMenu()` is overridden by each user type; each billing strategy has its own
  `computeTotal()`.
- **Singleton:** `HospitalData` holds all lists and handles saving/loading of the text files.
- **Strategy:** `RegularBilling`, `SeniorCitizenBilling`, `PwdBilling` and `PhilHealthBilling`.

## Project structure

```
src/
├── Main.java                 main menu: login, register, exit
└── hms/
    ├── billing/              BillingStrategy and its implementations
    ├── data/                 HospitalData (Singleton, file persistence)
    ├── exceptions/           custom exceptions
    ├── menus/                Admin, Doctor and Patient menus and forms
    ├── models/               User, Admin, Doctor, Patient, Appointment, MedicalRecord, Room, Admission, Bill
    ├── services/             login, appointment, admission and billing rules
    └── util/                 Validator, InputHelper, formatting and file helpers
```

## Limitations

- Console only, one user at a time.
- Payments are only recorded (marking a bill as paid); no actual payment processing.
- Laboratory, pharmacy and nursing functions are not included.
- For academic purposes only; not a production hospital information system.

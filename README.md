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
javac -d out $(find . -name "*.java")
java -cp out Main
```

On Windows (PowerShell):

```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java).FullName
java -cp out Main
```

You can also open the folder in IntelliJ IDEA or VS Code and run `Main.java`.

Data is saved in the `hospital_data/` folder (created on first run) so records are kept between sessions.

Amounts are shown with the peso sign (₱). On Windows, run `chcp 65001` in the terminal first so the sign
displays correctly.

### Default account

On the first run a default admin account is created:

| Username | Password    |
|----------|-------------|
| `admin`  | `Admin1234` |

Nothing is shown while you type a password (the prompt says "hidden as you type"); type it and press Enter.
You will be asked to change the password after the first login. Accounts created by the admin
(doctors and walk-in patients) also get a temporary password that must be changed on first login.

## Features

| Role    | What they can do |
|---------|------------------|
| Admin   | Manage patients, doctors, rooms, admissions and bills; view all appointments and book, reschedule or cancel them for patients; apply discounts; mark bills as paid |
| Doctor  | View today's/upcoming appointments, mark them completed or no-show, view own patients, add/view/update/delete medical records |
| Patient | Register, update profile, view doctors, book/reschedule/cancel own appointments, view own medical records, admissions and bills |

Type `back` at any prompt to cancel the current action. Empty input is never accepted: type `none` for an
optional detail you don't have (middle name, email, prescription), `keep` to keep a value when updating a
record, and `today` for today's date.

### Main rules and validations

- **Login:** 3 failed attempts return to the main menu. Passwords are stored as salted SHA-256 hashes.
- **Accounts:** usernames are 4–20 characters and unique; passwords need 8+ characters with an uppercase
  letter, a lowercase letter and a number, and must be typed twice.
- **Numbers:** menu choices, amounts and room numbers cannot have leading zeros (`1`, not `01`).
  Amounts accept `1500`, `1,500`, `1500.50` or `₱1,500`, up to 2 decimals.
- **Names:** letters, spaces, hyphens, apostrophes and periods only. Numbers are not allowed; suffixes are
  written in Roman numerals (`Jose Rizal III`, not `Jose Rizal 3rd`).
- **Patients:** first, optional middle and last name,
  birthdate not in the future and age 0–120, PH mobile numbers (`09XXXXXXXXX`, `+63` accepted), optional
  email, Senior Citizen ID (60+ only), PWD ID (`RR-PPMM-BBB-NNNNNNN`) and PhilHealth number (12 digits).
  The same person (name + birthdate) cannot be registered twice. Patients below 18 cannot create their
  own account; the admin registers them.
- **Doctors:** 7-digit PRC license (unique), consultation fee > 0, available days (e.g. `MON-FRI`) and
  clinic hours. A schedule change that would leave booked patients outside the new hours is rejected.
  A doctor with appointments still marked Scheduled cannot be deleted.
- **Appointments:** 30-minute slots within the doctor's clinic days and hours, not in the past and at most
  90 days ahead. No double booking of a doctor, and a patient cannot have two appointments at the same time
  or two with the same doctor on the same day. Admitted patients cannot book until they are discharged.
  Only past appointments can be marked completed/no-show, and past appointments can no longer be cancelled.
  Times can be typed as `9:00` or `09:00`.
- **Medical records:** one record per completed appointment, written by the doctor of that appointment.
- **Rooms and admissions:** only available rooms can be used or deleted, one active admission per patient,
  discharge date cannot be before the admission date, and a backdated admission cannot overlap an earlier
  stay of the same patient or in the same room. Admitted patients cannot book or reschedule appointments. A same-day discharge is billed as 1 day.
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
├── Main.java             main menu: login, register, exit
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

package menus;

import data.HospitalData;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import models.Patient;
import services.AuthService;
import util.Format;
import util.InputHelper;
import util.Validator;

import java.time.LocalDate;
import java.time.Period;

/**
 * Patient details form shared by self-registration, the admin's "Add patient",
 * and profile updates.
 */
public final class PatientForm {

    /** Age of majority in the Philippines; younger patients are registered by the admin. */
    public static final int ADULT_AGE = 18;

    private PatientForm() {
    }

    /**
     * Collects all details of a new patient and creates the account.
     *
     * @param byAdmin true when an admin creates the record; the password given is then
     *                temporary and must be changed on first login
     */
    public static Patient create(boolean byAdmin) throws DuplicateRecordException, InvalidInputException {
        HospitalData data = HospitalData.getInstance();
        AuthService auth = new AuthService();

        System.out.println("Personal information");
        String firstName = InputHelper.read("First name: ", s -> Validator.validateName(s, "First name"));
        String middleName = InputHelper.read("Middle name (type \"none\" if none): ",
                s -> InputHelper.isNone(s) ? "" : Validator.validateName(s, "Middle name"));
        String lastName = InputHelper.read("Last name: ", s -> Validator.validateName(s, "Last name"));
        LocalDate birthdate = InputHelper.read("Birthdate (yyyy-MM-dd): ",
                s -> Validator.validateBirthdate(Validator.parseDate(s, "Birthdate")));
        // Check early so the user does not fill in the whole form for nothing.
        auth.checkDuplicatePatient(firstName, middleName, lastName, birthdate, null);
        int age = Period.between(birthdate, LocalDate.now()).getYears();
        if (!byAdmin && age < ADULT_AGE) {
            // Minors cannot give consent on their own, so a parent or guardian registers them at the hospital.
            throw new InvalidInputException("Patients below " + ADULT_AGE + " cannot create their own account. "
                    + "Please register at the admitting section with a parent or guardian.");
        }

        String sex = InputHelper.read("Sex (M/F): ", Validator::validateSex);
        String contact = InputHelper.read("Mobile number (09XXXXXXXXX): ", Validator::validateContactNumber);
        String email = InputHelper.read("Email (type \"none\" if none): ",
                s -> InputHelper.isNone(s) ? "" : Validator.validateEmail(s, true));
        String address = InputHelper.read("Address (street, barangay, city): ", Validator::validateAddress);

        String seniorId = "";
        if (age >= Validator.SENIOR_AGE && InputHelper.readYesNo("Do you have a Senior Citizen ID?")) {
            seniorId = InputHelper.read("Senior Citizen ID number: ", Validator::validateSeniorId);
        }
        String pwdId = "";
        if (InputHelper.readYesNo("Do you have a PWD ID?")) {
            pwdId = InputHelper.read("PWD ID number (RR-PPMM-BBB-NNNNNNN): ",
                    s -> checkUniqueId(Validator.validatePwdId(s), null, true));
        }
        String philHealth = "";
        if (InputHelper.readYesNo("Are you a PhilHealth member?")) {
            philHealth = InputHelper.read("PhilHealth number (12 digits): ",
                    s -> checkUniqueId(Validator.validatePhilHealthNumber(s), null, false));
        }

        System.out.println();
        System.out.println("Account information");
        String username = Prompts.readNewUsername();
        Prompts.printPasswordRules();
        String password = Prompts.readNewPassword(username, byAdmin ? "Temporary password" : "Password");

        Patient patient = new Patient(data.nextPatientId(), firstName, lastName, username, password,
                birthdate, sex, contact, email, address);
        patient.setMiddleName(middleName);
        patient.setSeniorCitizenId(seniorId);
        patient.setPwdId(pwdId);
        patient.setPhilHealthNumber(philHealth);
        patient.setMustChangePassword(byAdmin);
        data.getPatients().add(patient);
        data.save();
        return patient;
    }

    /**
     * Updates contact details. Identity fields (name, birthdate, sex) can only be corrected by an admin,
     * the same way a hospital records desk would require a valid ID before changing them.
     */
    public static void update(Patient patient, boolean byAdmin) {
        HospitalData data = HospitalData.getInstance();
        System.out.println("Type \"" + InputHelper.KEEP_WORD + "\" to keep the current value.");

        String newFirst = patient.getFirstName();
        String newMiddle = patient.getMiddleName();
        String newLast = patient.getLastName();
        LocalDate newBirth = patient.getBirthdate();
        String newSex = patient.getSex();
        boolean isIdentityValid = !byAdmin;
        while (!isIdentityValid) {
            String first = InputHelper.readOptional("First name [" + patient.getFirstName() + "]: ",
                    s -> Validator.validateName(s, "First name"));
            String middle = InputHelper.readOptional("Middle name [" + Format.orDash(patient.getMiddleName())
                    + "] (type \"none\" to remove): ", s -> InputHelper.isNone(s) ? "" : Validator.validateName(s, "Middle name"));
            String last = InputHelper.readOptional("Last name [" + patient.getLastName() + "]: ",
                    s -> Validator.validateName(s, "Last name"));
            LocalDate birth = InputHelper.readOptional("Birthdate [" + patient.getBirthdate() + "]: ",
                    s -> Validator.validateBirthdate(Validator.parseDate(s, "Birthdate")));
            String sex = InputHelper.readOptional("Sex [" + patient.getSex() + "]: ", Validator::validateSex);

            newFirst = first == null ? patient.getFirstName() : first;
            newMiddle = middle == null ? patient.getMiddleName() : middle;
            newLast = last == null ? patient.getLastName() : last;
            newBirth = birth == null ? patient.getBirthdate() : birth;
            newSex = sex == null ? patient.getSex() : sex;
            // The same person cannot be on file twice; the identity fields are asked again instead.
            try {
                new AuthService().checkDuplicatePatient(newFirst, newMiddle, newLast, newBirth, patient.getId());
                isIdentityValid = true;
            } catch (DuplicateRecordException e) {
                Format.error(e.getMessage());
                Format.info("Please enter the name and birthdate again.");
            }
        }
        boolean seniorAge = Period.between(newBirth, LocalDate.now()).getYears() >= Validator.SENIOR_AGE;

        String contact = InputHelper.readOptional("Mobile number [" + patient.getContactNumber() + "]: ",
                Validator::validateContactNumber);
        String email = InputHelper.readOptional("Email [" + Format.orDash(patient.getEmail())
                + "] (type \"none\" to remove): ", s -> InputHelper.isNone(s) ? "" : Validator.validateEmail(s, true));
        String address = InputHelper.readOptional("Address [" + patient.getAddress() + "]: ", Validator::validateAddress);

        String senior = null;
        if (seniorAge) {
            senior = InputHelper.readOptional("Senior Citizen ID [" + Format.orDash(patient.getSeniorCitizenId())
                    + "] (type \"none\" to remove): ", s -> InputHelper.isNone(s) ? "" : Validator.validateSeniorId(s));
        } else if (patient.hasSeniorCitizenId()) {
            // Birthdate was corrected and the patient is no longer 60+.
            senior = "";
            Format.info("Senior Citizen ID will be removed because the patient is below 60.");
        }
        String pwd = InputHelper.readOptional("PWD ID [" + Format.orDash(patient.getPwdId())
                + "] (type \"none\" to remove): ",
                s -> InputHelper.isNone(s) ? "" : checkUniqueId(Validator.validatePwdId(s), patient.getId(), true));
        String philHealth = InputHelper.readOptional("PhilHealth number [" + Format.orDash(patient.getPhilHealthNumber())
                + "] (type \"none\" to remove): ",
                s -> InputHelper.isNone(s) ? "" : checkUniqueId(Validator.validatePhilHealthNumber(s), patient.getId(), false));

        // Apply everything only after all inputs were accepted.
        patient.setFirstName(newFirst);
        patient.setMiddleName(newMiddle);
        patient.setLastName(newLast);
        patient.setBirthdate(newBirth);
        patient.setSex(newSex);
        if (contact != null) {
            patient.setContactNumber(contact);
        }
        if (email != null) {
            patient.setEmail(email);
        }
        if (address != null) {
            patient.setAddress(address);
        }
        if (senior != null) {
            patient.setSeniorCitizenId(senior);
        }
        if (pwd != null) {
            patient.setPwdId(pwd);
        }
        if (philHealth != null) {
            patient.setPhilHealthNumber(philHealth);
        }
        data.save();
    }

    /** A PWD ID or PhilHealth number belongs to exactly one person. */
    private static String checkUniqueId(String value, String excludePatientId, boolean pwd)
            throws InvalidInputException {
        for (Patient p : HospitalData.getInstance().getPatients()) {
            if (p.getId().equals(excludePatientId)) {
                continue;
            }
            String existing = pwd ? p.getPwdId() : p.getPhilHealthNumber();
            if (existing.equalsIgnoreCase(value)) {
                throw new InvalidInputException((pwd ? "PWD ID " : "PhilHealth number ") + value
                        + " is already registered to another patient.");
            }
        }
        return value;
    }

    public static void printProfile(Patient p) {
        System.out.println("Patient ID     : " + p.getId());
        System.out.println("Name           : " + p.getCompleteName());
        System.out.println("Birthdate      : " + Format.date(p.getBirthdate()) + " (" + p.getAge() + " years old)");
        System.out.println("Sex            : " + p.getSex());
        System.out.println("Mobile number  : " + p.getContactNumber());
        System.out.println("Email          : " + Format.orDash(p.getEmail()));
        System.out.println("Address        : " + p.getAddress());
        System.out.println("Senior Cit. ID : " + Format.orDash(p.getSeniorCitizenId()));
        System.out.println("PWD ID         : " + Format.orDash(p.getPwdId()));
        System.out.println("PhilHealth No. : " + Format.orDash(p.getPhilHealthNumber()));
        System.out.println("Username       : " + p.getUsername());
    }
}

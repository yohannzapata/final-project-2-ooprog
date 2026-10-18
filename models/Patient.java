package models;

import menus.PatientMenu;
import util.RecordFormat;
import util.Validator;

import java.time.LocalDate;
import java.time.Period;

/**
 * A patient of the hospital. Senior Citizen, PWD and PhilHealth numbers are optional
 * and are only needed when the patient wants the matching billing discount.
 */
public class Patient extends User {

    private LocalDate birthdate;
    private String sex;
    private String contactNumber;
    private String email;
    private String address;
    private String seniorCitizenId;
    private String pwdId;
    private String philHealthNumber;

    public Patient(String id, String firstName, String lastName, String username, String password,
                   LocalDate birthdate, String sex, String contactNumber, String email, String address) {
        super(id, firstName, lastName, username, password);
        this.birthdate = birthdate;
        this.sex = sex;
        this.contactNumber = contactNumber;
        this.email = email;
        this.address = address;
        this.seniorCitizenId = "";
        this.pwdId = "";
        this.philHealthNumber = "";
    }

    private Patient(String[] f) {
        super(f[0], f[1], f[2], f[3], f[4], f[5], Boolean.parseBoolean(f[6]));
        this.birthdate = LocalDate.parse(f[7]);
        this.sex = f[8];
        this.contactNumber = f[9];
        this.email = f[10];
        this.address = f[11];
        this.seniorCitizenId = f[12];
        this.pwdId = f[13];
        this.philHealthNumber = f[14];
        if (f.length > 15) {
            setMiddleName(f[15]); // older files have no middle name
        }
    }

    @Override
    public void showMenu() {
        new PatientMenu(this).run();
    }

    @Override
    public String getRole() {
        return "Patient";
    }

    public int getAge() {
        return Period.between(birthdate, LocalDate.now()).getYears();
    }

    public boolean isSeniorCitizen() {
        return getAge() >= Validator.SENIOR_AGE;
    }

    public boolean hasSeniorCitizenId() {
        return !seniorCitizenId.isEmpty();
    }

    public boolean hasPwdId() {
        return !pwdId.isEmpty();
    }

    public boolean hasPhilHealth() {
        return !philHealthNumber.isEmpty();
    }

    public String toRecord() {
        return RecordFormat.join(getId(), getFirstName(), getLastName(), getUsername(),
                getPasswordHash(), getSalt(), String.valueOf(isMustChangePassword()),
                birthdate.toString(), sex, contactNumber, email, address,
                seniorCitizenId, pwdId, philHealthNumber, getMiddleName());
    }

    public static Patient fromRecord(String[] f) {
        return new Patient(f);
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        if (birthdate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birthdate cannot be in the future.");
        }
        this.birthdate = birthdate;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSeniorCitizenId() {
        return seniorCitizenId;
    }

    public void setSeniorCitizenId(String seniorCitizenId) {
        this.seniorCitizenId = seniorCitizenId == null ? "" : seniorCitizenId;
    }

    public String getPwdId() {
        return pwdId;
    }

    public void setPwdId(String pwdId) {
        this.pwdId = pwdId == null ? "" : pwdId;
    }

    public String getPhilHealthNumber() {
        return philHealthNumber;
    }

    public void setPhilHealthNumber(String philHealthNumber) {
        this.philHealthNumber = philHealthNumber == null ? "" : philHealthNumber;
    }
}

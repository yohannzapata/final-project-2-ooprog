package services;

import data.HospitalData;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.LoginFailedException;
import models.Patient;
import models.User;
import util.Validator;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Login, registration and password rules.
 */
public class AuthService {

    private final HospitalData data = HospitalData.getInstance();

    public User login(String username, String password) throws LoginFailedException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new LoginFailedException("Username and password are required.");
        }
        Optional<User> user = data.findUserByUsername(username.trim());
        // Same message for unknown user and wrong password so usernames cannot be guessed.
        if (!user.isPresent() || !user.get().checkPassword(password)) {
            throw new LoginFailedException("Incorrect username or password.");
        }
        return user.get();
    }

    /** Checks that the username is valid and not used by any admin, doctor or patient. */
    public String checkNewUsername(String input) throws InvalidInputException, DuplicateRecordException {
        String username = Validator.validateUsername(input);
        if (data.isUsernameTaken(username)) {
            throw new DuplicateRecordException("Username \"" + username + "\" is already taken.");
        }
        return username;
    }

    /**
     * Prevents creating the same person twice (same first name, last name and birthdate),
     * like a hospital checking for an existing patient record before making a new one.
     */
    public void checkDuplicatePatient(String firstName, String lastName, LocalDate birthdate, String excludeId)
            throws DuplicateRecordException {
        for (Patient p : data.getPatients()) {
            if (p.getId().equals(excludeId)) {
                continue;
            }
            if (p.getFirstName().equalsIgnoreCase(firstName) && p.getLastName().equalsIgnoreCase(lastName)
                    && p.getBirthdate().equals(birthdate)) {
                throw new DuplicateRecordException("A patient record for " + p.getFullName() + " born on "
                        + birthdate + " already exists (" + p.getId() + ").");
            }
        }
    }

    public void changePassword(User user, String currentPassword, String newPassword, String confirmPassword)
            throws InvalidInputException {
        if (!user.checkPassword(currentPassword)) {
            throw new InvalidInputException("Current password is incorrect.");
        }
        setNewPassword(user, newPassword, confirmPassword);
    }

    /** Used for the forced password change on first login, where the current password was just entered. */
    public void setNewPassword(User user, String newPassword, String confirmPassword) throws InvalidInputException {
        Validator.validatePassword(newPassword, user.getUsername());
        if (!newPassword.equals(confirmPassword)) {
            throw new InvalidInputException("Passwords do not match.");
        }
        if (user.checkPassword(newPassword)) {
            throw new InvalidInputException("New password must be different from the current password.");
        }
        user.setPassword(newPassword);
        user.setMustChangePassword(false);
        data.save();
    }
}

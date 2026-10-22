package services;

import data.HospitalData;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.LoginFailedException;
import models.Admin;
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
        Optional<User> found = data.findUserByUsername(username.trim());
        // Same message for unknown user and wrong password so usernames cannot be guessed.
        if (!found.isPresent()) {
            throw new LoginFailedException("Incorrect username or password.");
        }
        User user = found.get();
        if (user.isLocked()) {
            throw new LoginFailedException(lockedMessage(user), true);
        }
        if (!user.checkPassword(password)) {
            // Counted per account and saved, so starting a new login or restarting the program does not
            // give unlimited guesses.
            user.recordFailedLogin();
            data.save();
            if (user.isLocked()) {
                throw new LoginFailedException("Incorrect username or password. " + lockedMessage(user), true);
            }
            throw new LoginFailedException("Incorrect username or password.");
        }
        if (user.getFailedLogins() > 0) {
            user.clearFailedLogins();
            data.save();
        }
        return user;
    }

    private static String lockedMessage(User user) {
        String unlock = user instanceof Admin
                ? "Delete hospital_data/admins.txt to restore the default admin account."
                : "Ask the admin to reset your password.";
        return "This account is locked after " + User.MAX_FAILED_LOGINS + " wrong passwords in a row. " + unlock;
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
     * Two people with different middle names are treated as different persons.
     */
    public void checkDuplicatePatient(String firstName, String middleName, String lastName, LocalDate birthdate,
                                      String excludeId) throws DuplicateRecordException {
        for (Patient p : data.getPatients()) {
            if (p.getId().equals(excludeId)) {
                continue;
            }
            boolean middleMatches = p.getMiddleName().isEmpty() || middleName.isEmpty()
                    || p.getMiddleName().equalsIgnoreCase(middleName);
            if (p.getFirstName().equalsIgnoreCase(firstName) && p.getLastName().equalsIgnoreCase(lastName)
                    && middleMatches && p.getBirthdate().equals(birthdate)) {
                throw new DuplicateRecordException("A patient record for " + p.getFullName() + " born on "
                        + birthdate + " already exists (" + p.getId() + ").");
            }
        }
    }

    /**
     * Admin reset for a doctor or patient who forgot their password or was locked out, which also unlocks
     * the account. The new password is temporary, so the user has to choose their own at the next login,
     * like a newly created account.
     */
    public void resetPassword(User user, String temporaryPassword) throws InvalidInputException {
        Validator.validatePassword(temporaryPassword, user.getUsername());
        user.setPassword(temporaryPassword);
        user.setMustChangePassword(true);
        user.clearFailedLogins();
        data.save();
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

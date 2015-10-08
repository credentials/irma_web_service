package org.irmacard.web.restapi.resources.surfnetRegister;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class RegistrationStore {
    private static RegistrationStore vs;
    DataSource ds = null;
    private static Logger log = Logger.getLogger(RegistrationStore.class.getName());

    private RegistrationStore() throws RegistrationException {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            ds = (DataSource) envCtx.lookup("jdbc/irma_voucher");
        } catch (NamingException e) {
            e.printStackTrace();
            log.log(Level.SEVERE, "Cannot access database!");
            throw new RegistrationException("Cannot access database!");
        }
    }

    public static RegistrationStore getStore() throws RegistrationException {
        if (vs == null) {
            vs = new RegistrationStore();
        }

        return vs;
    }

    /**
     * Either returns existing registration token if user was already
     * registered, or null if user wasn't registered yet.
     *
     * @param name
     * @return
     */
    public String getUserRegistrationToken(String name) {
        Connection conn = null;
        ResultSet result;
        String uuid = null;

        log.log(Level.INFO, "Checking if user " + name + " is already registered");

        try {
            conn = ds.getConnection();

            // Test if name has already registered
            PreparedStatement test_exists = conn
                    .prepareStatement("SELECT * FROM registrations WHERE name = ?");
            test_exists.setString(1, name);

            result = test_exists.executeQuery();
            if (result.next()) {
                uuid = result.getString("registrationToken");
                log.log(Level.INFO, "User has RegistrationToken " + uuid);
            }
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
            log.log(Level.WARNING, "Unable to access database or database error", e);
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
            }
        }

        return uuid;
    }

    /**
     * Either returns existing registration token if user was already
     * registered, or null if user wasn't registered yet.
     *
     * @param name
     * @return
     */
    public boolean isRegistrationTokenKnown(String registrationToken) {
        Connection conn = null;
        ResultSet result;

        log.log(Level.INFO,
                "Checking if registrationToken " + registrationToken + " is known");

        try {
            conn = ds.getConnection();

            // Test if name has already registered
            PreparedStatement test_exists = conn.prepareStatement(
                    "SELECT * FROM registrations WHERE registrationToken = ?");
            test_exists.setString(1, registrationToken);

            result = test_exists.executeQuery();
            if (result.next()) {
                log.log(Level.INFO,
                        "RegistrationToken " + registrationToken + " is known");
                return true;
            }
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
            log.log(Level.WARNING, "Unable to access database or database error", e);
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
            }
        }

        return false;
    }

    public String registerUser(String name, boolean lunch) {
        Connection conn = null;
        String uuid = UUID.randomUUID().toString();

        log.log(Level.INFO, "Registering user " + name + " with UUID " + uuid
                + " and lunch " + lunch);

        try {
            conn = ds.getConnection();

            PreparedStatement insertRegistration = conn
                    .prepareStatement("INSERT INTO registrations "
                            + "(name, registrationToken, lunch) VALUES (?, ?, ?)");
            insertRegistration.setString(1, name);
            insertRegistration.setString(2, uuid);
            insertRegistration.setBoolean(3, lunch);

            insertRegistration.executeUpdate();

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            log.log(Level.WARNING, "Unable to access database or database error", e);
            return null;
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
            }
        }

        return uuid;
    }

    public boolean updateUser(String uuid, String name, boolean lunch) {
        Connection conn = null;

        log.log(Level.INFO,
                "Updating user " + name + " with UUID " + uuid + " and lunch " + lunch);

        try {
            conn = ds.getConnection();

            PreparedStatement insertRegistration = conn
                    .prepareStatement("UPDATE registrations "
                            + "SET lunch = ? WHERE registrationToken = ?");
            insertRegistration.setBoolean(1, lunch);
            insertRegistration.setString(2, uuid);

            insertRegistration.executeUpdate();

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            log.log(Level.WARNING, "Unable to access database or database error", e);
            return false;
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
            }
        }

        return true;
    }
}

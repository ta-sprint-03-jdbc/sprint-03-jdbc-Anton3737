package dao.impm_dao;

import dao.ChildDAO;
import dao.exception.DatabaseException;
import lombok.extern.slf4j.Slf4j;
import model.Child;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChildDB implements ChildDAO, AutoCloseable {

    // SQL query constants
    private static final String SQL_INSERT_CHILD = "INSERT INTO child (first_name, last_name, birth_date) VALUES (?,?,?);";
    private static final String SQL_UPDATE_CHILD = "UPDATE child SET first_name = ?, last_name = ?, birth_date = ? WHERE id = ?;";
    private static final String SQL_DELETE_CHILD = "DELETE FROM child WHERE id = ?;";
    private static final String SQL_SELECT_CHILDREN_BY_MIN_AGE = "SELECT id, first_name, last_name, birth_date FROM child WHERE EXTRACT(YEAR FROM AGE(birth_date)) >= ?;";
    private static final String SQL_SELECT_CHILDREN_WITHOUT_BIRTHDATE = "SELECT id, first_name, last_name, birth_date FROM child WHERE birth_date IS NULL;";


    private static final String SQL_SELECT_CHILD_BY_ID = "SELECT id, first_name, last_name, birth_date FROM child WHERE id = ?;";

    private final Connection conn;
    private final boolean ownsConnection;

    public ChildDB(Connection conn) {
        this.conn = conn;
        this.ownsConnection = false;
    }

    public ChildDB() {
        try {
            this.conn = DBUtil.getConnection();
            this.ownsConnection = true;
        } catch (SQLException e) {
            log.error("Failed to get database connection", e);
            throw new DatabaseException("Failed to get database connection", e);
        }
    }

    @Override
    public void close() throws Exception {
        if (!ownsConnection || conn == null || conn.isClosed()) {
            return;
        }
        try {
            conn.close();
            log.info("Connection closed successfully");
        } catch (SQLException e) {
            log.error("Error closing connection", e);
            throw e;
        }
    }

    @Override
    public Child addChild(Child child) throws SQLException {
        if (child == null) {
            log.warn("Attempted to add null child");
            throw new IllegalArgumentException("Child cannot be null");
        }

        if (child.firstName() == null || child.firstName().trim().isEmpty()) {
            log.warn("Attempted to add child with null or empty first name");
            throw new IllegalArgumentException("Child first name cannot be null or empty");
        }

        if (child.lastName() == null || child.lastName().trim().isEmpty()) {
            log.warn("Attempted to add child with null or empty last name");
            throw new IllegalArgumentException("Child last name cannot be null or empty");
        }

        try (PreparedStatement pst = conn.prepareStatement(SQL_INSERT_CHILD, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, child.firstName());
            pst.setString(2, child.lastName());
            java.sql.Date date = null;
            if (child.birthDate() != null) {
                date = java.sql.Date.valueOf(child.birthDate());
            }
            pst.setDate(3, date);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    log.info("Added new child with id: {}", id);
                    return new Child(id, child.firstName(), child.lastName(), child.birthDate());
                }
                log.error("Failed to get generated keys after insert");
                throw new SQLException("Failed to get generated keys after insert");
            }
        }
    }

    @Override
    public boolean updateChild(Child child) throws SQLException {
        if (child == null) {
            log.warn("Attempted to update null child");
            throw new IllegalArgumentException("Child cannot be null");
        }

        if (child.id() == null) {
            log.warn("Attempted to update child with null id");
            throw new IllegalArgumentException("Child ID cannot be null");
        }

        if (child.firstName() == null || child.firstName().trim().isEmpty()) {
            log.warn("Attempted to update child with null or empty first name");
            throw new IllegalArgumentException("Child first name cannot be null or empty");
        }

        if (child.lastName() == null || child.lastName().trim().isEmpty()) {
            log.warn("Attempted to update child with null or empty last name");
            throw new IllegalArgumentException("Child last name cannot be null or empty");
        }

        try (PreparedStatement pst = conn.prepareStatement(SQL_UPDATE_CHILD)) {
            pst.setString(1, child.firstName());
            pst.setString(2, child.lastName());
            if (child.birthDate() != null) {
                pst.setDate(3, java.sql.Date.valueOf(child.birthDate()));
            } else {
                pst.setNull(3, java.sql.Types.DATE);
            }
            pst.setLong(4, child.id());
            int affectedRows = pst.executeUpdate();
            log.info("Updated child with id: {}, affected rows: {}", child.id(), affectedRows);
            return affectedRows == 1;
        }
    }

    @Override
    public boolean deleteChild(Long id) throws SQLException {
        if (id == null) {
            log.warn("Attempted to delete child with null id");
            throw new IllegalArgumentException("Child ID cannot be null");
        }

        try (PreparedStatement pst = conn.prepareStatement(SQL_DELETE_CHILD)) {
            pst.setLong(1, id);
            int affectedRows = pst.executeUpdate();
            log.info("Deleted child with id: {}, affected rows: {}", id, affectedRows);
            return affectedRows == 1;
        }
    }


    public Child findChildById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Child ID cannot be null");
        }
        try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_CHILD_BY_ID)) {

            stmt.setLong(1, id); // Підставляємо ID замість знака питання

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Child(
                            rs.getLong("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getDate("birth_date") != null ? rs.getDate("birth_date").toLocalDate() : null
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    @Override
    public List<Child> findChildrenWithMinimumAge(int age) throws SQLException {
        if (age < 0) {
            log.warn("Attempted to find children with negative age: {}", age);
            throw new IllegalArgumentException("Age cannot be negative");
        }

        try (PreparedStatement statement = conn.prepareStatement(SQL_SELECT_CHILDREN_BY_MIN_AGE)) {
            statement.setInt(1, age);
            try (ResultSet rs = statement.executeQuery()) {
                List<Child> children = fromResultSetToChild(rs);
                log.info("Found {} children with minimum age: {}", children.size(), age);
                return children;
            }
        }
    }

    @Override
    public List<Child> findChildrenWithoutBirthDate() throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(SQL_SELECT_CHILDREN_WITHOUT_BIRTHDATE)) {
            try (ResultSet rs = statement.executeQuery()) {
                List<Child> children = fromResultSetToChild(rs);
                log.info("Found {} children without birth date", children.size());
                return children;
            }
        }
    }


    private static List<Child> fromResultSetToChild(ResultSet rs) throws SQLException {
        List<Child> children = new ArrayList<>();
        while (rs.next()) {
            Long id = rs.getLong("id");
            String firstName = rs.getString("first_name");
            String lastName = rs.getString("last_name");
            LocalDate date = null;
            Date birthDate = rs.getDate("birth_date");
            if (birthDate != null) {
                date = birthDate.toLocalDate();
            }
            var child = new Child(id, firstName, lastName, date);
            children.add(child);
        }
        return children;
    }

}
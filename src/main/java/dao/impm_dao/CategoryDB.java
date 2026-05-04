package dao.impm_dao;

import dao.CategoryDAO;
import lombok.extern.slf4j.Slf4j;
import model.Category;
import model.Child;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CategoryDB implements CategoryDAO, AutoCloseable {

    // SQL query constants
    private static final String SQL_SELECT_CATEGORY_BY_TITLE = "SELECT id, avatar, title FROM categories WHERE title = ?";
    private static final String SQL_INSERT_CATEGORY = "INSERT INTO categories (avatar, title) VALUES (?, ?)";
    private static final String SQL_UPDATE_CATEGORY = "UPDATE categories SET avatar = ?, title = ? WHERE id = ?";
    private static final String SQL_DELETE_CATEGORY = "DELETE FROM categories WHERE id = ?";
    private static final String SQL_SELECT_CATEGORIES_BY_TITLE_PART = "SELECT id, avatar, title FROM categories WHERE LOWER(title) LIKE ?";
    private static final String SQL_SELECT_CHILDREN_BY_CATEGORY = "SELECT DISTINCT c.id, c.first_name, c.last_name, c.birth_date FROM child c JOIN club_child cc ON c.id = cc.child_id JOIN club cl ON cc.club_id = cl.id WHERE cl.category_id = ?";

    private final Connection conn;
    private final boolean ownsConnection;

    public CategoryDB(Connection conn) {
        this.conn = conn;
        this.ownsConnection = false;
    }

    public CategoryDB() throws SQLException {
        this.conn = DBUtil.getConnection();
        this.ownsConnection = true;
    }

    @Override
    public void close() throws Exception {
        if (ownsConnection && conn != null && !conn.isClosed()) {
            try {
                conn.close();
                log.info("Connection closed successfully");
            } catch (SQLException e) {
                log.error("Error closing connection", e);
                throw e;
            }
        }
    }

    @Override
    public Category addCategory(Category category) throws SQLException {
        if (category == null) {
            log.warn("Attempted to add null category");
            throw new IllegalArgumentException("Category cannot be null");
        }

        if (category.title() == null || category.title().trim().isEmpty()) {
            log.warn("Attempted to add category with null or empty title");
            throw new IllegalArgumentException("Category title cannot be null or empty");
        }

        try (PreparedStatement pst = conn.prepareStatement(SQL_SELECT_CATEGORY_BY_TITLE)) {
            pst.setString(1, category.title());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong("id");
                    String avatar = rs.getString("avatar");
                    String title = rs.getString("title");
                    log.info("Found existing category with title: {}", category.title());
                    return new Category(id, avatar, title);
                }
            }
        }

        // Category doesn't exist, insert a new one
        try (PreparedStatement pst = conn.prepareStatement(SQL_INSERT_CATEGORY, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, category.avatar());
            pst.setString(2, category.title());
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    log.info("Added new category with id: {}", id);
                    return new Category(id, category.avatar(), category.title());
                }
                log.error("Failed to get generated keys after insert");
                throw new SQLException("Failed to get generated keys after insert");
            }
        }
    }

    @Override
    public boolean updateCategory(Category category) throws SQLException {
        if (category == null) {
            log.warn("Attempted to update null category");
            throw new IllegalArgumentException("Category cannot be null");
        }

        if (category.id() == null) {
            log.warn("Attempted to update category with null id");
            throw new IllegalArgumentException("Category ID cannot be null");
        }

        if (category.title() == null || category.title().trim().isEmpty()) {
            log.warn("Attempted to update category with null or empty title");
            throw new IllegalArgumentException("Category title cannot be null or empty");
        }

        try (PreparedStatement pst = conn.prepareStatement(SQL_UPDATE_CATEGORY)) {
            pst.setString(1, category.avatar());
            pst.setString(2, category.title());
            pst.setLong(3, category.id());
            int affectedRows = pst.executeUpdate();
            log.info("Updated category with id: {}, affected rows: {}", category.id(), affectedRows);
            return affectedRows == 1;
        }
    }

    @Override
    public boolean deleteCategory(Long id) throws SQLException {
        if (id == null) {
            log.warn("Attempted to delete category with null id");
            throw new IllegalArgumentException("Category ID cannot be null");
        }

        try (PreparedStatement pst = conn.prepareStatement(SQL_DELETE_CATEGORY)) {
            pst.setLong(1, id);
            int affectedRows = pst.executeUpdate();
            log.info("Deleted category with id: {}, affected rows: {}", id, affectedRows);
            return affectedRows == 1;
        }
    }

    @Override
    public List<Category> findCategoriesByTitlePart(String titlePart) throws SQLException {
        if (titlePart == null) {
            log.warn("Attempted to find categories with null titlePart");
            throw new IllegalArgumentException("Title part cannot be null");
        }

        List<Category> categories = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(SQL_SELECT_CATEGORIES_BY_TITLE_PART)) {
            statement.setString(1, "%" + titlePart.toLowerCase() + "%");
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String avatar = rs.getString("avatar");
                    String title = rs.getString("title");
                    Category category = new Category(id, avatar, title);
                    categories.add(category);
                }
            }
        }
        log.info("Found {} categories matching title part: {}", categories.size(), titlePart);
        return categories;
    }

    @Override
    public List<Child> findAllChildrenInCategory(Long categoryId) throws SQLException {
        if (categoryId == null) {
            log.warn("Attempted to find children with null categoryId");
            throw new IllegalArgumentException("Category ID cannot be null");
        }

        List<Child> children = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(SQL_SELECT_CHILDREN_BY_CATEGORY)) {
            statement.setLong(1, categoryId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String birthDateStr = rs.getString("birth_date");
                    LocalDate date = birthDateStr != null ? LocalDate.parse(birthDateStr) : null;
                    var child = new Child(id, firstName, lastName, date);
                    children.add(child);
                }
            }
        }
        log.info("Found {} children in category with id: {}", children.size(), categoryId);
        return children;
    }

}

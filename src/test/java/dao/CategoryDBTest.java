package dao;

import dao.impm_dao.CategoryDB;
import model.Category;
import model.Child;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utils.DBUtil;

import java.io.IOException;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Category Database Operations Tests")
class CategoryDBTest {

    private static final String EXISTING_TITLE = "Old Category";
    private static final String SPORTS_CATEGORY_TITLE = "Sports uniquE Activities";
    private static final String UNIQUE_SEARCH_PART = "uniquE";
    private static final int UNIQUE_CATEGORIES_COUNT = 4;

    private CategoryDB db;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        new DBUtil().executeFile("init.sql");
        db = new CategoryDB();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db != null) {
            db.close();
        }
    }

    @Test
    @DisplayName("Should increment category count when adding a new category")
    void incrementCategoryCountWhenAddNewCategory() throws SQLException {
        int oldCount = DBUtil.totalCount("categories");
        String uniqueTitle = "Educational Workshop " + System.currentTimeMillis();
        String avatarUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAIAQMAAAD+wSzIAAAABlBMVEX///+/v7+jQ3Y5AAAADklEQVQI12P4AIX8EAgALgAD/aNpbtEAAAAASUVORK5CYII";

        db.addCategory(new Category(avatarUrl, uniqueTitle));

        assertEquals(oldCount + 1, DBUtil.totalCount("categories"),
                "Database should contain exactly one more category after adding a new one");
    }

    @Test
    @DisplayName("Should not change category count when adding a category with existing title")
    void unchangedCategoryCountWhenAddCategoryWithExistingTitle() throws SQLException {
        int oldCount = DBUtil.totalCount("categories");

        Category result = db.addCategory(new Category("data:image/png;base64,NEW_AVATAR_DATA", EXISTING_TITLE));

        assertEquals(oldCount, DBUtil.totalCount("categories"),
                "Category count should remain unchanged when adding a category with existing title");
        assertNotNull(result.id(), "Result should have an ID");
        assertEquals(EXISTING_TITLE, result.title(), "Result should have the same title");
    }

    @Test
    @DisplayName("Should update an existing category")
    void updateExistingCategory() throws SQLException {
        String originalTitle = "Science Workshop " + System.currentTimeMillis();
        Category added = db.addCategory(new Category("data:image/png;base64,ORIGINAL_AVATAR", originalTitle));

        String updatedTitle = "Updated " + originalTitle;
        String updatedAvatar = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAIAQMAAAD+wSzIAAAABlBMVEX///+/v7+jQ3Y5AAAADklEQVQI12P4AIX8EAgALgAD/aNpbtEAAAAASUVORK5CYII";

        boolean updateResult = db.updateCategory(new Category(added.id(), updatedAvatar, updatedTitle));

        assertTrue(updateResult, "Update operation should return true when category is successfully updated");

        String query = "SELECT title FROM categories WHERE id = ?";
        try (Connection conn = utils.DBUtil.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setLong(1, added.id());
            try (ResultSet rs = pst.executeQuery()) {
                assertTrue(rs.next(), "Updated category should exist in database");
                assertEquals(updatedTitle, rs.getString("title"), "Category title should be updated in database");
            }
        }
    }

    @Test
    @DisplayName("Should delete an existing category")
    void deleteExistingCategory() throws SQLException {
        String uniqueTitle = "Temporary Category for Deletion " + System.currentTimeMillis();
        Category added = db.addCategory(new Category("data:image/png;base64,TEMP_AVATAR", uniqueTitle));

        boolean deleteResult = db.deleteCategory(added.id());

        assertTrue(deleteResult, "Delete operation should return true when category is successfully deleted");

        String verifyQuery = "SELECT COUNT(*) FROM categories WHERE id = ?";
        try (Connection conn = utils.DBUtil.getConnection();
             PreparedStatement pst = conn.prepareStatement(verifyQuery)) {
            pst.setLong(1, added.id());
            try (ResultSet rs = pst.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt(1), "Category should not exist in database after deletion");
            }
        }
    }

    @Test
    @DisplayName("Should find categories containing a specific title part")
    void findCategoriesByTitlePart() throws SQLException {
        List<Category> categories = db.findCategoriesByTitlePart(UNIQUE_SEARCH_PART);

        assertEquals(UNIQUE_CATEGORIES_COUNT, categories.size(),
                "Search should find exactly " + UNIQUE_CATEGORIES_COUNT + " categories containing '" + UNIQUE_SEARCH_PART + "'");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adding null category")
    void addCategoryShouldThrowExceptionForNullCategory() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> db.addCategory(null));

        assertEquals("Category cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adding category with null title")
    void addCategoryShouldThrowExceptionForNullTitle() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> db.addCategory(new Category("avatar-url", null)));

        assertEquals("Category title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adding category with empty title")
    void addCategoryShouldThrowExceptionForEmptyTitle() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> db.addCategory(new Category("avatar-url", "")));

        assertEquals("Category title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when updating null category")
    void updateCategoryShouldThrowExceptionForNullCategory() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> db.updateCategory(null));

        assertEquals("Category cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when updating category with null ID")
    void updateCategoryShouldThrowExceptionForNullId() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> db.updateCategory(new Category(null, "avatar-url", "Valid Title")));

        assertEquals("Category ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when updating category with null title")
    void updateCategoryShouldThrowExceptionForNullTitle() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> db.updateCategory(new Category(1L, "avatar-url", null)));

        assertEquals("Category title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when updating category with empty title")
    void updateCategoryShouldThrowExceptionForEmptyTitle() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> db.updateCategory(new Category(1L, "avatar-url", "")));

        assertEquals("Category title cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when deleting category with null ID")
    void deleteCategoryShouldThrowExceptionForNullId() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> db.deleteCategory(null));

        assertEquals("Category ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when finding categories with null title part")
    void findCategoriesByTitlePartShouldThrowExceptionForNullTitlePart() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> db.findCategoriesByTitlePart(null));

        assertEquals("Title part cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when finding children with null category ID")
    void findAllChildrenInCategoryShouldThrowExceptionForNullCategoryId() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> db.findAllChildrenInCategory(null));

        assertEquals("Category ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should find all children in category")
    void findAllChildrenInCategory() throws SQLException {
        Long categoryId = null;
        try (Connection conn = utils.DBUtil.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT id FROM categories WHERE title = ?")) {
            pst.setString(1, SPORTS_CATEGORY_TITLE);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    categoryId = rs.getLong("id");
                }
            }
        }
        assertNotNull(categoryId, "Category '" + SPORTS_CATEGORY_TITLE + "' should exist");

        List<Child> children = db.findAllChildrenInCategory(categoryId);

        assertFalse(children.isEmpty(), "Category should have children");
        assertTrue(children.stream().anyMatch(c -> "Michael".equals(c.firstName())), "Should find Michael in category");
        assertTrue(children.stream().anyMatch(c -> "William".equals(c.firstName())), "Should find William in category");
    }
}

package dao;

import dao.impm_dao.ChildDB;
import model.Child;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.*;
import utils.DBUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Child Database Operations Tests")
class ChildDBTest {

    private ChildDB db;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        new DBUtil().executeFile("init.sql");
        db = new ChildDB();
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("TRUNCATE TABLE child CASCADE");
        }
        db.close();
    }

    @Test
    @DisplayName("Should add a child and return it with an ID")
    void addShouldAddChildAndReturnWithId() throws SQLException {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        LocalDate birthDate = LocalDate.of(2010, 1, 1);
        Child child = new Child(firstName, lastName, birthDate);

        // Act
        Child addedChild = db.addChild(child);
        System.out.println("[DEBUG_LOG] Added child ID: " + addedChild.id());

        // Assert
        assertNotNull(addedChild.id(), "Child ID should not be null");
        assertEquals(firstName, addedChild.firstName(), "First name should match");
        assertEquals(lastName, addedChild.lastName(), "Last name should match");
        assertEquals(birthDate, addedChild.birthDate(), "Birth date should match");
    }

    @Test
    @DisplayName("Should add a child with null birth date")
    void addShouldHandleNullBirthDate() throws SQLException {

        Child child = new Child("Andreas", "Boticcelli", null);

        // Arrange
        Child addNewChildWithNullBDate = db.addChild(child);
        // Act

        assertNotNull(addNewChildWithNullBDate.id());
        assertNull(addNewChildWithNullBDate.birthDate());
        // Assert

    }

    @Test
    @DisplayName("Should update an existing child")
    void updateShouldUpdateExistingChild() throws SQLException {
        // Arrange - Add a child first
        Child addNewChild = new Child("Olha", "Tortykivna", LocalDate.of(2017, 6, 15));
        Child saveChild = db.addChild(addNewChild);
        Long realId = saveChild.id();

        // Create updated child
        Child updateChild = new Child(realId, "Olha", "Keksikivna", LocalDate.of(2017, 6, 15));
        // Act
        db.updateChild(updateChild);

        // Assert
        Child result = db.findChildById(realId);
        // Verify the update by querying the database
        assertNotNull(result, "Дитина має бути знайдена в БД");
        assertEquals("Keksikivna", result.lastName(), "Прізвище мало змінитись");
        assertEquals(realId, result.id(), "ID має залишитись незмінним");
    }


    @Test
    @DisplayName("Should delete an existing child")
    void deleteShouldDeleteExistingChild() throws SQLException {
        // Arrange - Add a child first
        Child newChild = db.addChild(new Child("Andreas", "Boticcelli", null));        // Act
        boolean isDeletedChild = db.deleteChild(newChild.id());

        // Assert
        assertTrue(isDeletedChild, "Child with id:" + newChild.id() + " deleted successful");
        // Verify the deletion by querying the database
        assertNull(db.findChildById(newChild.id()), "Child with id:" + newChild.id() + " doesn't exist");
    }


    @Test
    @DisplayName("Should return children with at least the specified age")
    void findChildrenWithMinimumAgeShouldReturnChildrenWithMinimumAge() throws SQLException {
        // Arrange - Add children with different ages
        // Child 1 - 10 years old
        // Child 2 - 5 years old
        // Child 3 - 15 years old
        Child child1 = new Child("Luisa", "Pannetone", LocalDate.of(2015, 3, 15));
        Child child2 = new Child("Donald", "Croissant", LocalDate.of(2021, 1, 20));
        Child child3 = new Child("Nadia", "Eclere", LocalDate.of(2011, 6, 11));

        db.addChild(child1);
        db.addChild(child2);
        db.addChild(child3);

        // Act - Get all children at least 10 years old
        List<Child> chosenAge = db.findChildrenWithMinimumAge(10);

        // Assert
        assertEquals(11, chosenAge.size(), "Collection should have 2 child with age more then 10 years old");

        // Verify that the result contains children with correct ages
        boolean hasLuisa = chosenAge.stream().anyMatch(c -> c.firstName().equals("Luisa"));
        boolean hasNadia = chosenAge.stream().anyMatch(c -> c.firstName().equals("Nadia"));
        boolean hasDonald = chosenAge.stream().anyMatch(c -> c.firstName().equals("Donald"));

        assertTrue(hasLuisa);
        assertTrue(hasNadia);
        assertFalse(hasDonald);
    }

    @Test
    @DisplayName("Should return children with null birth date")
    void findChildrenWithoutBirthDateShouldReturnChildrenWithNullBirthDate() throws SQLException {
        // Arrange - Add children with and without birth dates

        // Child with birth date
        Child childWithBD = new Child("Luisa", "Pannetone", LocalDate.of(2015, 3, 15));

        // Child without birth date
        Child childWithoutBD = new Child("Nadia", "Eclere", null);

        // Act
        db.addChild(childWithBD);
        db.addChild(childWithoutBD);

        // Assert
        List<Child> childrenWithoutBD = db.findChildrenWithoutBirthDate();
        System.out.println(childrenWithoutBD.size());

        // Verify that the result contains the child without birth date
        assertEquals(2, db.findChildrenWithoutBirthDate().size(), "Collection should have 2 child without birth date");

    }
}


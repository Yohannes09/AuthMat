//package RepositoryTests;
//
//import com.payme.app.AuthenticationApplication;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.SQLException;
//
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//@Testcontainers
//@SpringBootTest(classes = AuthenticationApplication.class)
//@ExtendWith(SpringExtension.class)
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class PostgresContainerTest {
//
//    @Container
//    private static final PostgreSQLContainer<?> postgresContainer
//            = new PostgreSQLContainer<>("postgres:15.3")
//            .withDatabaseName("testdb")
//            .withUsername("testuser")
//            .withPassword("testpass");
//
//    @Autowired
//    private DataSource dataSource;
//
//    static {
//        postgresContainer.start();
//        System.setProperty("spring.datasource.url", postgresContainer.getJdbcUrl());
//        System.setProperty("spring.datasource.username", postgresContainer.getUsername());
//        System.setProperty("spring.datasource.password", postgresContainer.getPassword());
//    }
//
//    @Test
//    void testContainerIsRunning() {
//        assert (postgresContainer.isRunning());
//    }
//
//    @Test
//    void testDatabaseConnection() throws SQLException {
//        try (Connection connection = dataSource.getConnection()) {
//            assertNotNull(connection);
//        }
//    }
//}
//

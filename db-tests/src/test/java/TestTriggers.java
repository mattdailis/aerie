import com.impossibl.postgres.jdbc.PGDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Assume;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseTests {
  private static final File initSqlScriptFile = new File("../deployment/postgres-init-db/sql/merlin/init.sql");
  private java.sql.Connection connection;

  // Setup test database
  @BeforeAll
  void beforeAll() throws SQLException, IOException, InterruptedException {

    // Create test database and grant privileges
    {
      final var pb = new ProcessBuilder("psql",
                                             "postgresql://postgres:postgres@localhost:5432",
                                             "-v", "ON_ERROR_STOP=1",
                                             "-c", "CREATE DATABASE aerie_merlin_test;",
                                             "-c", "GRANT ALL PRIVILEGES ON DATABASE aerie_merlin_test TO aerie;"
      );

      final var proc = pb.start();

      // Handle the case where we cannot connect to postgres by skipping the tests
      final var errors = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
      Assume.assumeFalse(errors.contains("Connection refused"));
      proc.waitFor();
      proc.destroy();
    }

    // Grant table privileges to aerie user for the tests
    // Apparently, the previous privileges are insufficient on their own
    {
      final var pb = new ProcessBuilder("psql",
                              "postgresql://postgres:postgres@localhost:5432/aerie_merlin_test",
                              "-v", "ON_ERROR_STOP=1",
                              "-c", "ALTER DEFAULT PRIVILEGES GRANT ALL ON TABLES TO aerie;",
                              "-c", "\\ir %s".formatted(initSqlScriptFile.getAbsolutePath())
      );

      pb.redirectError(ProcessBuilder.Redirect.INHERIT);
      final var proc = pb.start();
      proc.waitFor();
      proc.destroy();
    }

    final var pgDataSource = new PGDataSource();

    pgDataSource.setServerName("localhost");
    pgDataSource.setPortNumber(5432);
    pgDataSource.setDatabaseName("aerie_merlin_test");
    pgDataSource.setApplicationName("Merlin Database Tests");

    final var hikariConfig = new HikariConfig();
    hikariConfig.setUsername("aerie");
    hikariConfig.setPassword("aerie");
    hikariConfig.setDataSource(pgDataSource);

    final var hikariDataSource = new HikariDataSource(hikariConfig);

    connection = hikariDataSource.getConnection();
  }

  // Teardown test database
  @AfterAll
  void afterAll() throws SQLException, IOException, InterruptedException {
    Assume.assumeNotNull(connection);
    connection.close();

    // Clear out all data from the database on test conclusion
    // This is done WITH (FORCE) so there aren't issues with trying
    // to drop a database while there are connected sessions from
    // dev tools
    final var pb = new ProcessBuilder("psql",
                            "postgresql://postgres:postgres@localhost:5432",
                            "-v", "ON_ERROR_STOP=1",
                            "-c", "DROP DATABASE IF EXISTS aerie_merlin_test WITH (FORCE);"
    );

    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
    final var proc = pb.start();
    proc.waitFor();
    proc.destroy();
  }

  int insertFileUpload() throws SQLException {
    final var res = connection.createStatement()
                              .executeQuery(
                                  """
                                      INSERT INTO uploaded_file (path, name)
                                      VALUES ('test-path', 'test-name-%s')
                                      RETURNING id;"""
                                      .formatted(UUID.randomUUID().toString())
                              );
    res.next();
    return res.getInt("id");
  }

  void clearFileUploads() throws SQLException {
    connection.createStatement()
              .executeUpdate(
                  """
                      TRUNCATE uploaded_file CASCADE;"""
              );
  }

  int insertMissionModel(int fileId) throws SQLException {
    final var res = connection.createStatement()
                              .executeQuery(
                                  """
                                      INSERT INTO mission_model (name, mission, owner, version, jar_id)
                                      VALUES ('test-mission-model-%s', 'test-mission', 'tester', '0', %s)
                                      RETURNING id;"""
                                      .formatted(UUID.randomUUID().toString(), fileId)
                              );
    res.next();
    return res.getInt("id");
  }

  void clearMissionModels() throws SQLException {
    connection.createStatement()
              .executeUpdate(
                  """
                      TRUNCATE mission_model CASCADE;"""
              );
  }

  int insertPlan(int missionModelId) throws SQLException {
    final var res = connection.createStatement()
                              .executeQuery(
                                  """
                                      INSERT INTO plan (name, model_id, duration, start_time)
                                      VALUES ('test-plan-%s', '%s', '0', '2020-1-1 00:00:00')
                                      RETURNING id;"""
                                      .formatted(UUID.randomUUID().toString(), missionModelId)
                              );
    res.next();
    return res.getInt("id");
  }

  void clearPlans() throws SQLException {
    connection.createStatement()
              .executeUpdate(
                  """
                      TRUNCATE plan CASCADE;"""
              );
  }

  int insertActivity(int planId) throws SQLException {
    final var res = connection.createStatement()
                              .executeQuery(
                                  """
                                      INSERT INTO activity (type, plan_id, start_offset, arguments)
                                      VALUES ('test-activity', '%s', '00:00:00', '{}')
                                      RETURNING id;"""
                                      .formatted(planId)
                              );

    res.next();
    return res.getInt("id");
  }

  void clearActivities() throws SQLException {
    connection.createStatement()
              .executeUpdate(
                  """
                      TRUNCATE activity CASCADE;"""
              );
  }

  int insertSimulationTemplate(int modelId) throws SQLException {
    final var res = connection.createStatement()
                              .executeQuery(
                                  """
                                      INSERT INTO simulation_template (model_id, description, arguments)
                                      VALUES ('%s', 'test-description', '{}')
                                      RETURNING id;"""
                                      .formatted(modelId)
                              );
    res.next();
    return res.getInt("id");
  }

  void clearSimulationTemplates() throws SQLException {
    connection.createStatement()
              .executeUpdate(
                  """
                      TRUNCATE simulation_template CASCADE;"""
              );
  }

  int insertSimulation(int simulationTemplateId, int planId) throws SQLException {
    final var res = connection.createStatement()
                              .executeQuery(
                                  """
                                      INSERT INTO simulation (simulation_template_id, plan_id, arguments)
                                      VALUES ('%s', '%s', '{}')
                                      RETURNING id;"""
                                      .formatted(simulationTemplateId, planId)
                              );
    res.next();
    return res.getInt("id");
  }

  void clearSimulations() throws SQLException {
    connection.createStatement()
              .executeUpdate(
                  """
                      TRUNCATE simulation CASCADE;"""
              );
  }

  int fileId;
  int missionModelId;
  int planId;
  int activityId;
  int simulationTemplateId;
  int simulationId;

  @BeforeEach
  void beforeEach() throws SQLException {
    fileId = insertFileUpload();
    missionModelId = insertMissionModel(fileId);
    planId = insertPlan(missionModelId);
    activityId = insertActivity(planId);
    simulationTemplateId = insertSimulationTemplate(missionModelId);
    simulationId = insertSimulation(simulationTemplateId, planId);
  }

  @AfterEach
  void afterEach() throws SQLException {
    clearFileUploads();
    clearMissionModels();
    clearPlans();
    clearActivities();
    clearSimulationTemplates();
    clearSimulations();
  }

  @Nested
  class MissionModelTriggers {
    @Test
    void shouldIncrementMissionModelRevisionOnMissionModelUpdate() throws SQLException {
      final var res = connection.createStatement()
                                .executeQuery(
                                    """
                                        SELECT revision
                                        FROM mission_model
                                        WHERE id = %s;"""
                                        .formatted(missionModelId)
                                );
      res.next();
      final var revision = res.getInt("revision");

      final var updateRes = connection.createStatement()
                                      .executeUpdate(
                                          """
                                              UPDATE mission_model
                                              SET name = 'updated-name-%s'
                                              WHERE id = %s;"""
                                              .formatted(UUID.randomUUID().toString(), missionModelId)
                                      );
      assertEquals(1, updateRes);

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision
                                               FROM mission_model
                                               WHERE id = %s;"""
                                               .formatted(missionModelId)
                                       );
      updatedRes.next();
      final var updatedRevision = updatedRes.getInt("revision");

      assertEquals(revision + 1, updatedRevision);
    }

    @Test
    void shouldIncrementMissionModelRevisionOnMissionModelJarIdUpdate() throws SQLException {
      final var res = connection.createStatement()
                                .executeQuery(
                                    """
                                        SELECT revision
                                        FROM mission_model
                                        WHERE id = %s;"""
                                        .formatted(missionModelId)
                                );
      res.next();
      final var revision = res.getInt("revision");

      final var updateRes = connection.createStatement()
                                      .executeUpdate(
                                          """
                                              UPDATE uploaded_file
                                              SET path = 'test-path-updated'
                                              WHERE id = %s;"""
                                              .formatted(fileId)
                                      );
      assertEquals(1, updateRes);

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision
                                               FROM mission_model
                                               WHERE id = %s;"""
                                               .formatted(missionModelId)
                                       );
      updatedRes.next();
      final var updatedRevision = updatedRes.getInt("revision");

      assertEquals(revision + 1, updatedRevision);
    }
  }

  @Nested
  class PlanTriggers {
    @Test
    void shouldIncrementPlanRevisionOnPlanUpdate() throws SQLException {
      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");

      connection.createStatement()
                .executeUpdate(
                    """
                        UPDATE plan SET name = 'test-plan-updated-%s'
                        WHERE id = %s;"""
                        .formatted(UUID.randomUUID().toString(), planId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      updatedRes.next();

      assertEquals(initialRevision + 1, updatedRes.getInt("revision"));
    }

    @Test
    void shouldIncrementPlanRevisionOnActivityInsert() throws SQLException {
      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");

      final var activityId = insertActivity(planId);

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      updatedRes.next();

      assertEquals(initialRevision + 1, updatedRes.getInt("revision"));
    }

    @Test
    void shouldIncrementPlanRevisionOnActivityUpdate() throws SQLException {

      final var activityId = insertActivity(planId);

      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");

      connection.createStatement()
                .executeUpdate(
                    """
                        UPDATE activity SET type = 'test-activity-updated'
                        WHERE id = %s;"""
                        .formatted(activityId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      updatedRes.next();

      assertEquals(initialRevision + 1, updatedRes.getInt("revision"));
    }

    @Test
    void shouldIncrementPlanRevisionOnActivityDelete() throws SQLException {

      final var activityId = insertActivity(planId);

      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");

      connection.createStatement()
                .executeUpdate(
                    """
                        DELETE FROM activity
                        WHERE id = %s;"""
                        .formatted(activityId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      updatedRes.next();

      assertEquals(initialRevision + 1, updatedRes.getInt("revision"));
    }
  }

  @Nested
  class SimulationTemplateTriggers {
    @Test
    void shouldIncrementSimulationTemplateRevisionOnSimulationTemplateUpdate() throws SQLException {

      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM simulation_template
                                               WHERE id = %s;"""
                                               .formatted(simulationTemplateId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");

      connection.createStatement()
                .executeUpdate(
                    """
                        UPDATE simulation_template SET description = 'test-description-updated'
                        WHERE id = %s;"""
                        .formatted(simulationTemplateId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM simulation_template
                                               WHERE id = %s;"""
                                               .formatted(simulationTemplateId)
                                       );
      updatedRes.next();

      assertEquals(initialRevision + 1, updatedRes.getInt("revision"));
    }
  }

  @Nested
  class SimulationTriggers {
    @Test
    void shouldIncrementSimulationRevisionOnSimulationUpdate() throws SQLException {

      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM simulation
                                               WHERE id = %s;"""
                                               .formatted(simulationId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");

      connection.createStatement()
                .executeUpdate(
                    """
                        UPDATE simulation SET arguments = '{}'
                        WHERE id = %s;"""
                        .formatted(simulationId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM simulation
                                               WHERE id = %s;"""
                                               .formatted(simulationId)
                                       );
      updatedRes.next();

      assertEquals(initialRevision + 1, updatedRes.getInt("revision"));
    }
  }
}

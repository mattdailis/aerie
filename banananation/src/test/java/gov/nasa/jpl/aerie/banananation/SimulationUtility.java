package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.generated.GeneratedMissionModelFactory;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SimulationUtility {
  private static MissionModel<?> makeMissionModel(final MissionModelBuilder builder, final SerializedValue config) {
    final var factory = new GeneratedMissionModelFactory();
    // TODO: [AERIE-1516] Teardown the model to release any system resources (e.g. threads).
    final var model = factory.instantiate(config, builder);
    return builder.build(model, factory.getTaskSpecTypes());
  }

  public static SimulationResults
  simulate(final Map<String, Pair<Duration, SerializedActivity>> schedule, final Duration simulationDuration) {
    final var dataPath = Path.of(SimulationUtility.class.getClassLoader().getResource("data/lorem_ipsum.txt").getPath());
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, dataPath);
    final var serializedConfig = new ConfigurationValueMapper().serializeValue(config);
    final var missionModel = makeMissionModel(new MissionModelBuilder(), serializedConfig);
    final var startTime = Instant.now();

    return SimulationDriver.simulate(
        missionModel,
        schedule,
        startTime,
        simulationDuration);
  }

  @SafeVarargs
  public static Map<String, Pair<Duration, SerializedActivity>> buildSchedule(final Pair<Duration, SerializedActivity>... activitySpecs) {
    final var schedule = new HashMap<String, Pair<Duration, SerializedActivity>>();

    for (final var activitySpec : activitySpecs) {
      schedule.put(UUID.randomUUID().toString(), activitySpec);
    }

    return schedule;
  }
}

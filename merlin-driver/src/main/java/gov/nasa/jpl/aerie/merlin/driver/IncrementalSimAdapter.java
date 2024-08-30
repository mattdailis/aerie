package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.ammos.aerie.simulation.protocol.ProfileSegment;
import gov.nasa.ammos.aerie.simulation.protocol.Results;
import gov.nasa.ammos.aerie.simulation.protocol.Schedule;
import gov.nasa.ammos.aerie.simulation.protocol.Simulator;
import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class IncrementalSimAdapter implements Simulator<IncrementalSimAdapter.IncrementalSimCache> {

  public record IncrementalSimCache(Optional<SimulationDriver> driver) {}

    @Override
    public IncrementalSimCache initCache() {
        return new IncrementalSimCache(Optional.empty());
    }

    @Override
    public <Config, Model> ResultsWithCache<IncrementalSimCache> simulate(ModelType<Config, Model> modelType, Config config, Schedule schedule, Instant startTime, Duration duration, IncrementalSimCache cache) {
      // TODO conditionally call simulate or diffAndSimulate depending on whether or not this is the first simulation

        final var builder = new MissionModelBuilder();
        final var builtModel = builder.build(modelType.instantiate(startTime, config, builder), DirectiveTypeRegistry.extract(modelType));

        if (cache.driver.isEmpty()) {
          final var driver = new SimulationDriver(builtModel, startTime, duration, false);
          SimulationResultsInterface results = driver.simulate(
              adaptSchedule(schedule),
              startTime,
              duration,
              startTime,
              duration,
              () -> false,
              $ -> {}
          );
          return new ResultsWithCache<>(adaptResults(results), new IncrementalSimCache(Optional.of(driver)));
        } else {
          final var driver = cache.driver.get();
          driver.initSimulation(duration);
          SimulationResultsInterface results = driver.diffAndSimulate(
              adaptSchedule(schedule),
              startTime,
              duration,
              startTime,
              duration
          );
          return new ResultsWithCache<>(adaptResults(results), new IncrementalSimCache(Optional.of(driver)));
        }

    }

    private Results adaptResults(SimulationResultsInterface results) {
        return new Results(
                results.getStartTime(),
                results.getDuration(),
                results.getRealProfiles().entrySet().stream().map($ -> Pair.of($.getKey(), Pair.of($.getValue().getKey(), adaptProfile($)))).collect(Collectors.toMap(Pair::getKey, Pair::getValue)),
                results.getDiscreteProfiles().entrySet().stream().map($ -> Pair.of($.getKey(), Pair.of($.getValue().getKey(), adaptProfile($)))).collect(Collectors.toMap(Pair::getKey, Pair::getValue)),
                results.getSimulatedActivities().entrySet().stream().map($ -> Pair.of($.getKey().id(), adaptSimulatedActivity($.getValue()))).collect(Collectors.toMap(Pair::getKey, Pair::getValue))
        );
    }

    private gov.nasa.ammos.aerie.simulation.protocol.SimulatedActivity adaptSimulatedActivity(SimulatedActivity simulatedActivity) {
        return new gov.nasa.ammos.aerie.simulation.protocol.SimulatedActivity(
                simulatedActivity.type(),
                simulatedActivity.arguments(),
                simulatedActivity.start(),
                simulatedActivity.duration(),
                simulatedActivity.parentId() == null ? null : simulatedActivity.parentId().id(),
                simulatedActivity.childIds().stream().map(SimulatedActivityId::id).toList(),
                simulatedActivity.directiveId().map(ActivityDirectiveId::id),
                simulatedActivity.computedAttributes()
        );
    }

    private static <T> List<ProfileSegment<T>> adaptProfile(Map.Entry<String, Pair<ValueSchema, List<gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment<T>>>> $) {
        return $.getValue().getValue().stream().map(IncrementalSimAdapter::adaptSegment).toList();
    }

    private static <T> ProfileSegment<T> adaptSegment(gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment<T> segment) {
        return new ProfileSegment<>(segment.extent(), segment.dynamics());
    }

    private Map<ActivityDirectiveId, ActivityDirective> adaptSchedule(Schedule schedule) {
        final var res = new HashMap<ActivityDirectiveId, ActivityDirective>();
        for (var entry : schedule.entries()) {
            res.put(new ActivityDirectiveId(entry.id()),
                    new ActivityDirective(
                            entry.startOffset(),
                            entry.directive().type(),
                            entry.directive().arguments(),
                            null,
                            true));
        }
        return res;
    }
}

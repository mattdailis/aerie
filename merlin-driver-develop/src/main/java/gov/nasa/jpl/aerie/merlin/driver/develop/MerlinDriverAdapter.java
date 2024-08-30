package gov.nasa.jpl.aerie.merlin.driver.develop;

import gov.nasa.ammos.aerie.simulation.protocol.ProfileSegment;
import gov.nasa.ammos.aerie.simulation.protocol.Results;
import gov.nasa.ammos.aerie.simulation.protocol.Schedule;
import gov.nasa.ammos.aerie.simulation.protocol.Simulator;
import gov.nasa.jpl.aerie.merlin.driver.develop.*;
import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.types.*;

import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;

public class MerlinDriverAdapter implements Simulator<Unit> {
    @Override
    public Unit initCache() {
        return Unit.UNIT;
    }

    @Override
    public <Config, Model> ResultsWithCache<Unit> simulate(ModelType<Config, Model> modelType, Config config, Schedule schedule, Instant startTime, Duration duration, Unit unit) {
        final var builder = new MissionModelBuilder();
        final var builtModel = builder.build(modelType.instantiate(startTime, config, builder), DirectiveTypeRegistry.extract(modelType));
        SimulationResults results = SimulationDriver.simulate(
                builtModel,
                adaptSchedule(schedule),
                startTime,
                duration,
                startTime,
                duration,
                () -> false
        );
        return new ResultsWithCache<>(adaptResults(results), Unit.UNIT);
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

    private Results adaptResults(SimulationResults results) {
        return new Results(
                results.startTime,
                results.duration,
                results.realProfiles.entrySet().stream().map($ -> Pair.of($.getKey(), Pair.of($.getValue().getKey(), adaptProfile($)))).collect(Collectors.toMap(Pair::getKey, Pair::getValue)),
                results.discreteProfiles.entrySet().stream().map($ -> Pair.of($.getKey(), Pair.of($.getValue().getKey(), adaptProfile($)))).collect(Collectors.toMap(Pair::getKey, Pair::getValue)),
                results.simulatedActivities.entrySet().stream().map($ -> Pair.of($.getKey().id(), adaptSimulatedActivity($.getValue()))).collect(Collectors.toMap(Pair::getKey, Pair::getValue))
        );
    }

    private static <T> List<ProfileSegment<T>> adaptProfile(Map.Entry<String, Pair<ValueSchema, List<gov.nasa.jpl.aerie.merlin.driver.develop.engine.ProfileSegment<T>>>> $) {
        return $.getValue().getValue().stream().map(MerlinDriverAdapter::adaptSegment).toList();
    }

    private static <T> ProfileSegment<T> adaptSegment(gov.nasa.jpl.aerie.merlin.driver.develop.engine.ProfileSegment<T> segment) {
        return new ProfileSegment<>(segment.extent(), segment.dynamics());
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
}

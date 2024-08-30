package gov.nasa.ammos.aerie.simulation.protocol;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record Results(
        Instant startTime,
        Duration duration,
        Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> realProfiles,
        Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> discreteProfiles,
        Map<Long, SimulatedActivity> simulatedActivities
//        Map<SimulatedActivityId, UnfinishedActivity> unfinishedActivities,
//        List<Triple<Integer, String, ValueSchema>> topics,
//        Map<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events
) {
    static Results empty() {
        return new Results(Instant.EPOCH, Duration.ZERO, Map.of(), Map.of(), Map.of());
    }
}

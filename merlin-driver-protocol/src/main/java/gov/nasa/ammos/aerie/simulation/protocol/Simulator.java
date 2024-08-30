package gov.nasa.ammos.aerie.simulation.protocol;

import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface Simulator<Cache> {
    record ResultsWithCache<Cache>(Results results, Cache cache) {
        public Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> getRealProfiles() {
            return results.realProfiles();
        }

        public Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> getDiscreteProfiles() {
            return results.discreteProfiles();
        }

        public Map<Long, SimulatedActivity> getSimulatedActivities() {
            return results.simulatedActivities();
        }
    }

    Cache initCache();
    <Config, Model> ResultsWithCache<Cache> simulate(
            ModelType<Config, Model> modelType,
            Config config,
            Schedule schedule,
            Instant startTime,
            Duration duration,
            Cache cache);

    default <Config, Model> ResultsWithCache<Cache> simulate(
        ModelType<Config, Model> modelType,
        Config config,
        Schedule schedule,
        Instant startTime,
        Duration duration) {
      return simulate(modelType, config, schedule, startTime, duration, initCache());
    }

    default void initSimulation(Duration duration) {
//        throw new NotImplementedException();
    }

    default <Config, Model> ResultsWithCache<Cache> diffAndSimulate(ModelType<Config, Model> modelType, Config config, Schedule originalSchedule, Schedule diff, Instant startTime, Duration simDuration, Cache cache) {
        return this.simulate(modelType, config, diff, startTime, simDuration, cache);
    }

    default ResultsWithCache<Cache> computeResults(Instant startTime, Duration simDuration) {
        throw new NotImplementedException();
    }

    default String getFruitProfileAsString() {
        //String correctResProfile = driver.getEngine().resources.get(new ResourceId("/fruit")).profile().segments().toString();
        return "fRuItPrOfIlE";
    }
}

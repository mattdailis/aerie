package gov.nasa.ammos.aerie.simulation.protocol;

import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
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

/**
 * Interactive simulator specification
 *
 * - Initialize
 *   - Load mission model
 *   - Allocate cells
 *   - Pause waiting for input
 * - Schedule task at offset from current time (could be zero)
 *   - Returns a handle to refer to this task(?)
 * - Pause in Query context (outside of any task)
 *   - May be based on time, condition, task start/end, or "any events" on a given topic
 *   - Stepping options:
 *     - Step into active task
 *     -
 * - Pause in Task context (step through tasks's actions one by one)
 *   - May be stepped into from a pause in Query context if the task is active
 *   - May be the result of an event being emitted on a given topic
 * -
 *
 *
 * Interactive simulation is /like/ a task, but there is value in a more global view
 *
 * Halting debug - stop the program in certain conditions
 * Semi-hosting = protocol for halting the program - breakpoints serve as system calls
 *
 * @param <Cache>
 */
public interface InteractiveSimulator<Cache> {
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

    <Config, Model> InteractiveSimulation begin(
        ModelType<Config, Model> modelType,
        Config config,
        Instant startTime
    );

    Cache initCache();
    <Config, Model> ResultsWithCache<Cache> simulate(
            ModelType<Config, Model> modelType,
            Config config,
            GenericSchedule schedule,
            Instant startTime,
            Duration duration,
            Cache cache);

    default <Config, Model> ResultsWithCache<Cache> simulate(
        ModelType<Config, Model> modelType,
        Config config,
        GenericSchedule schedule,
        Instant startTime,
        Duration duration) {
      return simulate(modelType, config, schedule, startTime, duration, initCache());
    }

    default void initSimulation(Duration duration) {
//        throw new NotImplementedException();
    }

    default <Config, Model> ResultsWithCache<Cache> diffAndSimulate(ModelType<Config, Model> modelType, Config config, GenericSchedule originalGenericSchedule, GenericSchedule diff, Instant startTime, Duration simDuration, Cache cache) {
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

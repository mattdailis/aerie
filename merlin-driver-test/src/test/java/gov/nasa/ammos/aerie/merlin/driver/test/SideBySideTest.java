package gov.nasa.ammos.aerie.merlin.driver.test;

import gov.nasa.ammos.aerie.simulation.protocol.Directive;
import gov.nasa.ammos.aerie.simulation.protocol.Schedule;
import gov.nasa.ammos.aerie.simulation.protocol.Simulator;
import gov.nasa.jpl.aerie.banananation.Configuration;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.banananation.generated.GeneratedModelType;
import gov.nasa.jpl.aerie.merlin.driver.develop.MerlinDriverAdapter;
import gov.nasa.jpl.aerie.merlin.driver.IncrementalSimAdapter;
import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SideBySideTest {
  private static final ModelType<Configuration, Mission> MODEL = new GeneratedModelType();
  private static final Configuration CONFIG = new Configuration(
      Configuration.DEFAULT_PLANT_COUNT,
      Configuration.DEFAULT_PRODUCER,
      Path.of("/etc/hosts"),
      Configuration.DEFAULT_INITIAL_CONDITIONS,
      false);
  private static final Instant START = Instant.EPOCH;
  private TestRegistrar model;

  @BeforeEach
  void setup() {
    model = new TestRegistrar();
  }

  @SuppressWarnings("unchecked")
  @Test
  <Cache> void testSideBySide() {
    final var incrementalSimulator = (Simulator<Cache>) new IncrementalSimAdapter();
    final var regularSimulator = new MerlinDriverAdapter();

    final var schedule1 = Schedule.build(Pair.of(duration(1, SECOND), new Directive("BiteBanana", Map.of())));
    final var schedule2 = Schedule.build(
        Pair.of(duration(1, SECOND), new Directive("BiteBanana", Map.of())),
        Pair.of(duration(2, SECOND), new Directive("BiteBanana", Map.of())));

    final var regResult1 = regularSimulator.simulate(MODEL, CONFIG, schedule1, START, duration(10, SECONDS));
    final Simulator.ResultsWithCache<Cache> incResult1 = incrementalSimulator.simulate(
        MODEL,
        CONFIG,
        schedule1,
        START,
        duration(10, SECONDS));

    assertEquals(regResult1.results(), incResult1.results());

    final var regResult2 = regularSimulator.simulate(MODEL, CONFIG, schedule2, START, duration(10, SECONDS));
    final var incResult2 = incrementalSimulator.simulate(
        MODEL,
        CONFIG,
        schedule2,
        START,
        duration(10, SECONDS),
        incResult1.cache());

    assertEquals(regResult2.results(), incResult2.results());
  }

  public record Cell<Event, Value>(
      Topic<Event> topic,
      Value initialValue,
      Function<List<Event>, Value> apply
  )
  {
    public static <Event, Value> Cell<Event, Value> of(
        Value initialValue,
        Function<List<Event>, Value> apply)
    {
      return new Cell<>(new Topic<>(), initialValue, apply);
    }

    public void emit(Event event) {
      TestContext.get().scheduler().emit(event, this.topic);
    }

    @SuppressWarnings("unchecked")
    public Value get() {
      final var context = TestContext.get();
      final var scheduler = context.scheduler();
      final var cellId = context.cells().get(this);
      return ((MutableObject<Value>) scheduler.get(Objects.requireNonNull(cellId))).getValue();
    }
  }

  @Test
  void testAlternateInlineMissionModel() {
    final var cell1 = model.cell(1);
    final var cell2 = model.cell(2);

    model.activity("abc", $ -> {
      assertEquals(1, cell1.get());
      assertEquals(2, cell2.get());

      cell1.emit(3);

      assertEquals(3, cell1.get());
      assertEquals(2, cell2.get());

      cell2.emit(4);

      assertEquals(3, cell1.get());
      assertEquals(4, cell2.get());

      cell1.emit(1);
      cell2.emit(2);

      assertEquals(1, cell1.get());
      assertEquals(2, cell2.get());
    });

    model.resource("cell1", cell1::get);

    incrementalSimTestCase(
        model.asModelType(),
        Unit.UNIT,
        START,
        duration(10, SECONDS),
        Schedule.build(
            Pair.of(duration(1, SECOND), new Directive("abc", Map.of())),
            Pair.of(duration(2, SECOND), new Directive("abc", Map.of())),
            Pair.of(duration(3, SECOND), new Directive("abc", Map.of()))),
        Schedule.build(
            Pair.of(duration(1, SECOND), new Directive("abc", Map.of())),
            Pair.of(duration(3, SECOND), new Directive("abc", Map.of()))));
  }

  @Test
  void testDelay() {
    final var model = new TestRegistrar();
    final var cell1 = model.cell(1);
    final var cell2 = model.cell(2);

    model.activity("abc", $ -> {
      assertEquals(1, cell1.get());
      assertEquals(2, cell2.get());

      cell1.emit(3);

      delay(duration(10, SECOND));

      assertEquals(3, cell1.get());
      assertEquals(2, cell2.get());

      cell2.emit(4);

      assertEquals(3, cell1.get());
      assertEquals(4, cell2.get());

      cell1.emit(1);
      cell2.emit(2);

      assertEquals(1, cell1.get());
      assertEquals(2, cell2.get());
    });

    incrementalSimTestCase(
        model.asModelType(),
        Unit.UNIT,
        START,
        duration(10, SECONDS),
        Schedule.build(
            Pair.of(duration(1, SECOND), new Directive("abc", Map.of())),
            Pair.of(duration(2, SECOND), new Directive("abc", Map.of())),
            Pair.of(duration(3, SECOND), new Directive("abc", Map.of()))),
        Schedule.build(
            Pair.of(duration(1, SECOND), new Directive("abc", Map.of())),
            Pair.of(duration(3, SECOND), new Directive("abc", Map.of()))));
  }

  private void delay(Duration duration) {
    TestContext.get().threadedTask().thread().delay(duration);
  }

  private void waitUntil(Condition condition) {
    TestContext.get().threadedTask().thread().waitUntil(condition);
  }

  private void call(TaskFactory<?> child) {
    TestContext.get().threadedTask().thread().call(InSpan.Fresh, child);
  }

  public <Cache, Config, Model> void incrementalSimTestCase(
      ModelType<Config, Model> model,
      Config config,
      Instant startTime,
      Duration duration,
      Schedule... schedules)
  {
    final var incrementalSimulator = (Simulator<Cache>) new IncrementalSimAdapter();
    final var regularSimulator = new MerlinDriverAdapter();
    Cache cache = incrementalSimulator.initCache();
    for (final var schedule : schedules) {
      final var regularResultsWithCache = regularSimulator.simulate(model, config, schedule, startTime, duration);
      final Simulator.ResultsWithCache<Cache> incrementalResultsWithCache = incrementalSimulator.simulate(
          model,
          config,
          schedule,
          startTime,
          duration,
          cache);
      assertEquals(regularResultsWithCache.results(), incrementalResultsWithCache.results());
      cache = incrementalResultsWithCache.cache();
    }
  }

  public static <Event, Value> CellId<MutableObject<Value>> allocate(
      final Initializer builder,
      final Cell<Event, Value> cell)
  {
    return builder.allocate(
        new MutableObject<>(cell.initialValue()),
        new CellType<>() {
          @Override
          public EffectTrait<List<Event>> getEffectType() {
            return new EffectTrait<>() {
              @Override
              public List<Event> empty() {
                return List.of();
              }

              @Override
              public List<Event> sequentially(final List<Event> prefix, final List<Event> suffix) {
                final var res = new ArrayList<Event>(prefix);
                res.addAll(suffix);
                return res;
              }

              @Override
              public List<Event> concurrently(final List<Event> left, final List<Event> right) {
                if (left.isEmpty()) return right;
                if (right.isEmpty()) return left;
                throw new UnsupportedOperationException("Unsupported concurrent composition of non-empty lists");
              }
            };
          }

          @Override
          public MutableObject<Value> duplicate(final MutableObject<Value> mutableObject) {
            return new MutableObject<>(mutableObject.getValue());
          }

          @Override
          public void apply(final MutableObject<Value> mutableObject, final List<Event> o) {
            mutableObject.setValue(cell.apply().apply(o));
          }
        },
        List::of,
        cell.topic());
  }
}


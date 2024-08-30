package gov.nasa.ammos.aerie.merlin.driver.test;

import gov.nasa.ammos.aerie.simulation.protocol.Directive;
import gov.nasa.ammos.aerie.simulation.protocol.ProfileSegment;
import gov.nasa.ammos.aerie.simulation.protocol.Schedule;
import gov.nasa.ammos.aerie.simulation.protocol.Simulator;
import gov.nasa.jpl.aerie.banananation.Configuration;
import gov.nasa.jpl.aerie.banananation.generated.GeneratedModelType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.IntStream;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.*;
import static gov.nasa.ammos.aerie.simulation.protocol.Schedule.ScheduleEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class IncrementalSimTest {
  public static final GeneratedModelType MODEL = new GeneratedModelType();
  private static boolean debug = false;
  private static Configuration CONFIG = new Configuration(
          Configuration.DEFAULT_PLANT_COUNT,
          Configuration.DEFAULT_PRODUCER,
          Path.of(SimulationUtility.class.getResource("data/lorem_ipsum.txt").getPath()),
          Configuration.DEFAULT_INITIAL_CONDITIONS,
          false
  );

  @Test
  public <Cache> void testRemoveAndAddActivity() {
    if (debug) System.out.println("testRemoveAndAddActivity()");
    final var schedule1 = Schedule.build(
        Pair.of(
            duration(5, SECONDS),
            new Directive("PeelBanana", Map.of()))
    );
    final var schedule2 = Schedule.build(
        Pair.of(
            duration(3, SECONDS),
            new Directive("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final Simulator<Cache> driver = (Simulator<Cache>) SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();

    // Add PeelBanana at time = 5
    Simulator.ResultsWithCache<Cache> simulationResults = driver.simulate(MODEL, CONFIG, schedule1, startTime, simDuration);
    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruitProfile = " + fruitProfile);

    assertEquals(1, simulationResults.getSimulatedActivities().size());
    assertEquals(2, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);
    assertEquals(Duration.of(5, SECONDS), fruitProfile.get(0).extent());
    assertEquals(3.0, fruitProfile.get(1).dynamics().initial);

    // Remove PeelBanana (back to empty schedule)
    simulationResults = driver.simulate(MODEL, CONFIG, Schedule.empty(), startTime, simDuration, simulationResults.cache());
    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruitProfile = " + fruitProfile);

    assertEquals(0, simulationResults.getSimulatedActivities().size());
    assertEquals(1, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);

    // Add PeelBanana at time = 3
    simulationResults = driver.simulate(MODEL, CONFIG, schedule2, startTime, simDuration, simulationResults.cache());
    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruitProfile = " + fruitProfile);

    assertEquals(1, simulationResults.getSimulatedActivities().size());
    assertEquals(2, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);
    assertEquals(Duration.of(3, SECONDS), fruitProfile.get(0).extent());
    assertEquals(3.0, fruitProfile.get(1).dynamics().initial);
  }

  @Test
  public <Cache> void testRemoveActivity() {
    if (debug) System.out.println("testRemoveActivity()");

    final var schedule = Schedule.build(
        Pair.of(
            duration(5, SECONDS),
            new Directive("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final var driver = (Simulator<Cache>) SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();
    var simulationResults = driver.simulate(MODEL, CONFIG, schedule, startTime, simDuration);
    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(MODEL, CONFIG, schedule, Schedule.empty(), startTime, simDuration, simulationResults.cache());

    assertEquals(0, simulationResults.getSimulatedActivities().size());

    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    assertEquals(4.0, fruitProfile.get(fruitProfile.size()-1).dynamics().initial);
  }

  @Test
  public <Cache> void testMoveActivityLater() {
    if (debug) System.out.println("testMoveActivityLater()");

    final var schedule1 = Schedule.build(
        Pair.of(
            duration(3, SECONDS),
            new Directive("PeelBanana", Map.of()))
    );
    final var schedule2 = Schedule.build(
        Pair.of(
            duration(5, SECONDS),
            new Directive("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final var driver = (Simulator<Cache>) SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();
    var simulationResults = driver.simulate(MODEL, CONFIG, schedule1, startTime, simDuration);
    driver.initSimulation(simDuration);
    simulationResults = driver.diffAndSimulate(MODEL, CONFIG, schedule1, schedule2, startTime, simDuration, simulationResults.cache());

    assertEquals(1, simulationResults.getSimulatedActivities().size());
    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    assertEquals(3.0, fruitProfile.get(fruitProfile.size()-1).dynamics().initial);
  }

  @Test
  public <Cache> void testMoveActivityPastAnother() {
    if (debug) System.out.println("testMoveActivityPastAnother()");

    var schedule = Schedule.build(
        Pair.of(
            duration(3, SECONDS),
            new Directive("PeelBanana", Map.of())),
        Pair.of(
            duration(5, SECONDS),
            new Directive("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final var driver = (Simulator<Cache>) SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();
    if (debug) System.out.println("1st schedule: " + schedule);
    var simulationResults = driver.simulate(MODEL, CONFIG, Schedule.empty(), startTime, simDuration);

    final Schedule.ScheduleEntry firstEntry = schedule.entries().getFirst();
    assertEquals(Duration.of(3, SECONDS), firstEntry.startTime());
    schedule = schedule.put(firstEntry.id(), Duration.of(7, SECONDS), firstEntry.directive());

    driver.initSimulation(simDuration);
    if (debug) System.out.println("2nd schedule: " + schedule);
    simulationResults = driver.diffAndSimulate(MODEL, CONFIG, Schedule.empty(), schedule, startTime, simDuration, simulationResults.cache());

    assertEquals(2, simulationResults.getSimulatedActivities().size());
    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruit profile = " + fruitProfile);

    assertEquals(3, fruitProfile.size());
    assertEquals(4.0, fruitProfile.get(0).dynamics().initial);
    assertEquals(3.0, fruitProfile.get(1).dynamics().initial);
    assertEquals(2.0, fruitProfile.get(2).dynamics().initial);
  }

  @Test
  public <Cache> void testZeroDurationEventAtStart() {
    if (debug) System.out.println("testZeroDurationEventAtStart()");

    final var schedule1 = Schedule.build(
        Pair.of(
            duration(0, SECONDS),
            new Directive("PeelBanana", Map.of())),
        Pair.of(
            duration(5, SECONDS),
            new Directive("GrowBanana", Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", SerializedValue.of(Duration.SECOND.times(2).in(Duration.MICROSECONDS)))))
    );

    final var schedule2 = Schedule.build(
        Pair.of(
            duration(8, SECONDS),
            new Directive("PeelBanana", Map.of()))
    );

    final var simDuration = duration(10, SECOND);

    final var driver = (Simulator<Cache>) SimulationUtility.getDriver(simDuration);

    final var startTime = Instant.now();
    var simulationResults = driver.simulate(MODEL, CONFIG, schedule1, startTime, simDuration);

    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruit profile = " + fruitProfile);

    driver.initSimulation(simDuration);
    simulationResults = driver.simulate(MODEL, CONFIG, schedule1.plus(schedule2), startTime, simDuration); // TODO is this plus correct?

    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("fruit profile = " + fruitProfile);

    assertEquals(3, simulationResults.getSimulatedActivities().size());
    assertEquals(4, fruitProfile.size());
    assertEquals(3.0, fruitProfile.get(0).dynamics().initial);
    assertEquals(3.0, fruitProfile.get(1).dynamics().initial);
    assertEquals(4.0, fruitProfile.get(2).dynamics().initial);
    assertEquals(3.0, fruitProfile.get(3).dynamics().initial);
  }

  @Test
  public void testSimultaneousEvents() {
    if (debug) System.out.println("testSimultaneousEvents()");
    // SimulatedActivityId[id=0]=SimulatedActivity[type=BiteBanana, arguments={biteSize=NumericValue[value=3.0]}, start=2023-10-22T19:12:52.109029Z, duration=+00:00:00.000000, parentId=null, childIds=[], directiveId=Optional[ActivityDirectiveId[id=0]], computedAttributes=MapValue[map={newFlag=StringValue[value=B], biteSizeWasBig=BooleanValue[value=true]}]],
    // SimulatedActivityId[id=1]=SimulatedActivity[type=GrowBanana, arguments={growingDuration=NumericValue[value=1000000], quantity=NumericValue[value=1]}, start=2023-10-22T19:12:51.109029Z, duration=+00:00:01.000000, parentId=null, childIds=[], directiveId=Optional.empty, computedAttributes=MapValue[map={}]],
    // SimulatedActivityId[id=3]=SimulatedActivity[type=BiteBanana, arguments={biteSize=NumericValue[value=1.0]}, start=2023-10-22T19:12:52.109029Z, duration=+00:00:00.000000, parentId=null, childIds=[], directiveId=Optional.empty, computedAttributes=MapValue[map={newFlag=StringValue[value=A], biteSizeWasBig=BooleanValue[value=false]}]],
    // SimulatedActivityId[id=4]=SimulatedActivity[type=GrowBanana, arguments={growingDuration=NumericValue[value=1000000], quantity=NumericValue[value=1]}, start=2023-10-22T19:12:55.109029Z, duration=+00:00:01.000000, parentId=null, childIds=[], directiveId=Optional.empty, computedAttributes=MapValue[map={}]],
    // SimulatedActivityId[id=5]=SimulatedActivity[type=GrowBanana, arguments={growingDuration=NumericValue[value=1000000], quantity=NumericValue[value=1]}, start=2023-10-22T19:12:49.109029Z, duration=+00:00:01.000000, parentId=null, childIds=[], directiveId=Optional.empty, computedAttributes=MapValue[map={}]],
    // SimulatedActivityId[id=6]=SimulatedActivity[type=BiteBanana, arguments={biteSize=NumericValue[value=1.0]}, start=2023-10-22T19:12:50.109029Z, duration=+00:00:00.000000, parentId=null, childIds=[], directiveId=Optional.empty, computedAttributes=MapValue[map={newFlag=StringValue[value=A], biteSizeWasBig=BooleanValue[value=false]}]],
    // SimulatedActivityId[id=7]=SimulatedActivity[type=GrowBanana, arguments={growingDuration=NumericValue[value=1000000], quantity=NumericValue[value=1]}, start=2023-10-22T19:12:47.109029Z, duration=+00:00:01.000000, parentId=null, childIds=[], directiveId=Optional.empty, computedAttributes=MapValue[map={}]],
    // SimulatedActivityId[id=8]=SimulatedActivity[type=GrowBanana, arguments={growingDuration=NumericValue[value=1000000], quantity=NumericValue[value=1]}, start=2023-10-22T19:12:53.109029Z, duration=+00:00:01.000000, parentId=null, childIds=[], directiveId=Optional.empty, computedAttributes=MapValue[map={}]]
    final var schedule1 = Schedule.build(
        Pair.of(
            duration(1, SECONDS),
            new Directive("GrowBanana", Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", SerializedValue.of(Duration.SECOND.times(1).in(Duration.MICROSECONDS))))),
        Pair.of(
            duration(2, SECONDS),
            new Directive("GrowBanana", Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", SerializedValue.of(Duration.SECOND.times(1).in(Duration.MICROSECONDS))))),
        Pair.of(
            duration(3, SECONDS),
            new Directive("BiteBanana", Map.of("biteSize", SerializedValue.of(1)))),
        Pair.of(
            duration(4, SECONDS),
            new Directive("GrowBanana", Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", SerializedValue.of(Duration.SECOND.times(1).in(Duration.MICROSECONDS))))),
        Pair.of(
            duration(5, SECONDS),
            new Directive("BiteBanana", Map.of("biteSize", SerializedValue.of(3)))),
        Pair.of(
            duration(5, SECONDS),
            new Directive("BiteBanana", Map.of("biteSize", SerializedValue.of(1)))),
        Pair.of(
            duration(6, SECONDS),
            new Directive("GrowBanana", Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", SerializedValue.of(Duration.SECOND.times(1).in(Duration.MICROSECONDS))))),
        Pair.of(
            duration(8, SECONDS),
            new Directive("GrowBanana", Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", SerializedValue.of(Duration.SECOND.times(1).in(Duration.MICROSECONDS)))))
    );
    var schedule2 = Schedule.empty();
    var schedule3 = Schedule.empty();
    for (ScheduleEntry entry : schedule1.entries()) {
      final SerializedValue val = entry.directive().arguments().get("biteSize");
      if (val == null || !val.equals(SerializedValue.of(3))) {
        schedule2 = schedule2.put(entry.id(), entry.startTime(), entry.directive());
      } else {
        schedule3 = schedule3.put(entry.id(), entry.startTime(), entry.directive());
      }
    }

    final var startTime = Instant.now();
    final var simDuration = duration(10, SECOND);

    // simulate the schedule for a baseline to compare against incremental sim
    var driver = SimulationUtility.getDriver(simDuration, false);
    var simulationResults = driver.simulate(MODEL, CONFIG, schedule1, startTime, simDuration);
    final List<ProfileSegment<RealDynamics>> correctFruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();

    // create a new driver to start over
    driver = SimulationUtility.getDriver(simDuration, false);
    simulationResults = driver.simulate(MODEL, CONFIG, schedule2, startTime, simDuration);

    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();

    // now do incremental sim on schedule
    driver.initSimulation(simDuration);
    simulationResults = driver.simulate(MODEL, CONFIG, schedule1, startTime, simDuration);
    if (debug) System.out.println("correct      fruit profile = " + correctFruitProfile);
    if (debug) System.out.println("partial      fruit profile = " + fruitProfile);

    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("inc sim      fruit profile = " + fruitProfile);
    List<ProfileSegment<RealDynamics>> diff = subtract(fruitProfile, correctFruitProfile);
    if (debug) System.out.println("inc sim diff fruit profile = " + diff);
  }

  @Test
  public void testDaemon() {
    if (debug) System.out.println("testDaemon()");


    final var emptySchedule = Schedule.empty();
    final var schedule = Schedule.build(
        Pair.of(
            duration(5, SECONDS),
            new Directive("BiteBanana", Map.of("biteSize", SerializedValue.of(3))))
    );

    final var startTime = Instant.now();
    final var simDuration = duration(10, SECOND);

    // simulate the schedule for a baseline to compare against incremental sim
    var driver = SimulationUtility.getDriver(simDuration, true);
    final Configuration daemonConfig = new Configuration(
        Configuration.DEFAULT_PLANT_COUNT,
        Configuration.DEFAULT_PRODUCER,
        Path.of(SimulationUtility.class.getResource("data/lorem_ipsum.txt").getPath()),
        Configuration.DEFAULT_INITIAL_CONDITIONS,
        true // run daemons for this test
    );
    var simulationResults = driver.simulate(MODEL, daemonConfig, schedule, startTime, simDuration);
    final List<ProfileSegment<RealDynamics>> correctFruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    String correctResProfile = driver.getFruitProfileAsString();

    if (debug) System.out.println("schedule = " + simulationResults.getSimulatedActivities());


    // create a new driver to start over
    driver = SimulationUtility.getDriver(simDuration, true);
    simulationResults = driver.simulate(MODEL, daemonConfig, emptySchedule, startTime, simDuration);

    var fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    String fruitResProfile = driver.getFruitProfileAsString();

    // now do incremental sim on schedule
    driver.initSimulation(simDuration);
    simulationResults = driver.simulate(MODEL, daemonConfig, schedule, startTime, simDuration);
    String fruitResProfile2 = driver.getFruitProfileAsString();
    if (debug) System.out.println("correct        fruit profile = " + correctFruitProfile);
    if (debug) System.out.println("empty schedule fruit profile = " + fruitProfile);

    fruitProfile = simulationResults.getRealProfiles().get("/fruit").getRight();
    if (debug) System.out.println("inc sim        fruit profile = " + fruitProfile);
    List<ProfileSegment<RealDynamics>> diff = subtract(fruitProfile, correctFruitProfile);
    if (debug) System.out.println("inc sim diff   fruit profile = " + diff);

    if (debug) System.out.println("");

    if (debug) System.out.println("correct        fruit profile = " + correctResProfile);
    if (debug) System.out.println("empty schedule fruit profile = " + fruitResProfile);
    if (debug) System.out.println("inc sim        fruit profile = " + fruitResProfile2);

    RealDynamics z = RealDynamics.linear(0.0, 0.0);
    for (var segment : diff) {
      assertEquals(segment.dynamics(), z, segment + " should be " + z);
    }
  }

  private List<ProfileSegment<RealDynamics>> subtract(List<ProfileSegment<RealDynamics>> lps1, List<ProfileSegment<RealDynamics>> lps2) {
    List<ProfileSegment<RealDynamics>> result = new ArrayList<>();
    int i = 0;
    for (; i < Math.min(lps1.size(), lps2.size()); ++i) {
      var pf1 = lps1.get(i);
      var pf2 = lps2.get(i);
      if (pf1.extent().isEqualTo(pf2.extent())) {
        result.add(new ProfileSegment<>(pf1.extent(), pf1.dynamics().minus(pf2.dynamics())));
      } else {
        result.add(new ProfileSegment<>(Duration.min(pf1.extent(), pf2.extent()), pf1.dynamics().minus(pf2.dynamics())));
        break;
      }
    }
    if (i < Math.max(lps1.size(), lps2.size())) {
      result.add(new ProfileSegment<>(ZERO, RealDynamics.linear(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)));
    }
    return result;
  }


  final static String INIT_SIM = "Initial Simulation";
  final static String COMP_RESULTS = "Compute Results";
  final static String SERIALIZE_RESULTS = "Serialize Results";
  final static String INC_SIM = "Incremental Simulation";
  final static String COMP_INC_RESULTS = "Compute Incremental Results";
  final static String SERIALIZE_INC_RESULTS = "Serialize Combined Results";

  final static String[] labels = new String[] { INIT_SIM, COMP_RESULTS, SERIALIZE_RESULTS,
                                                INC_SIM, COMP_INC_RESULTS, SERIALIZE_INC_RESULTS };

  final static String[] incSimLabels = new String[] { INC_SIM, COMP_INC_RESULTS, SERIALIZE_INC_RESULTS };


  @Test
  public <Cache> void testPerformanceOfOneEditToScaledPlan() {
    if (debug) System.out.println("testPerformanceOfOneEditToScaledPlan()");

    int scaleFactor = 1000;

    final List<Integer> sizes = IntStream.rangeClosed(1, 20).boxed().map(i -> i * scaleFactor).toList();
    System.out.println("Numbers of activities to test: " + sizes);

    long spread = 5;
    Duration unit = SECONDS;

    final Directive biteBanana = new Directive("BiteBanana", Map.of());

    final Directive peelBanana = new Directive("PeelBanana", Map.of());

    final Directive changeProducerChiquita = new Directive("ChangeProducer", Map.of("producer", SerializedValue.of("Chiquita")));

    final Directive changeProducerDole = new Directive("ChangeProducer", Map.of("producer", SerializedValue.of("Dole")));

    //HashMap<String, HashMap<String, List<Double>>> stats = new HashMap<>();

    var testTimer = new Timer("testPerformanceOfOneEditToScaledPlan", false);

    // test each case
    for (int numActs : sizes) {

      var scaleTimer = new Timer("test " + numActs, false);

      // generate numActs activities
      Pair<Duration, Directive>[] pairs = new Pair[numActs];
      for (int i = 0; i < numActs; ++i) {
        pairs[i] = Pair.of(duration(spread * (i + 1), unit),
                           changeProducerChiquita);
        ++i;
        pairs[i] = Pair.of(duration(spread * (i + 1), unit),
                           changeProducerDole);
      }
      var schedule = Schedule.build(pairs);

      final var startTime = Instant.now();
      final var simDuration = duration(spread * (numActs + 2), SECOND);

      var timer = new Timer(INIT_SIM + " " + numActs, false);
      final var driver = (Simulator<Cache>) SimulationUtility.getDriver(simDuration);
      var simulationResults = driver.simulate(MODEL, CONFIG, schedule, startTime, simDuration);
      final var originalSchedule = schedule; // save it off. Schedules are immutable, so this is safe to do without copying.
      timer.stop(false);

      timer = new Timer(COMP_RESULTS + " " + numActs, false);
//      driver.computeResults(startTime, simDuration);
      timer.stop(false);
      timer = new Timer(SERIALIZE_RESULTS + " " + numActs, false);
//      String results = simulationResults.toString();
      timer.stop(false);

      // Modify a directive in the schedule
      final var d0 = schedule.entries().getFirst().id();
      long middleDirectiveNum = d0 + schedule.size() / 2;
      long directiveId = middleDirectiveNum;  // get middle activity
      final ScheduleEntry directive = schedule.get(directiveId);
      schedule = schedule.put(directiveId, directive.startOffset().plus(1, unit), directive.directive());

      timer = new Timer(INC_SIM + " " + numActs, false);
      driver.initSimulation(simDuration);
      simulationResults = driver.diffAndSimulate(MODEL, CONFIG, originalSchedule, schedule, startTime, simDuration, simulationResults.cache());
      timer.stop(false);

      timer = new Timer(COMP_INC_RESULTS + " " + numActs, false);
//      simulationResults = driver.computeResults(startTime, simDuration);
      timer.stop(false);
      timer = new Timer(SERIALIZE_INC_RESULTS + " " + numActs, false);
//      results = simulationResults.toString();  // The results are not combined until they forced to be
      timer.stop(false);

      scaleTimer.stop(false);
    }

    testTimer.stop(false);

    //Timer.logStats();
    // Write out stats
    final ConcurrentSkipListMap<String, ConcurrentSkipListMap<Timer.StatType, Long>>
        mm = Timer.getStats();
    ArrayList<String> header = new ArrayList<>();
    header.add("Number of Activities");
    for (int i = 0; i < labels.length; ++i) {
      header.add(labels[i] + " (duration)");
      header.add(labels[i] + " (cpu time)");
    }
    System.out.println(String.join(", ", header));
    for (int numActs : sizes) {
      ArrayList<String> row = new ArrayList<>();
      row.add("" + numActs);
      for (int i = 0; i < labels.length; ++i) {
        ConcurrentSkipListMap<Timer.StatType, Long> statMap = mm.get(labels[i] + " " + numActs);
        row.add("" + Timer.formatDuration(statMap.get(Timer.StatType.wallClockTime)));
        row.add("" + Timer.formatDuration(statMap.get(Timer.StatType.cpuTime)));
      }
      System.out.println(String.join(", ", row));
    }
  }


  @Test
  @Disabled
  public <Cache> void testPerformanceOfRepeatedSimsToScaledPlan() {
    if (debug) System.out.println("testPerformanceOfRepeatedSimsToScaledPlan()");

    int scaleFactor = 10;
    int numEdits = 50;

    final List<Integer> sizes = IntStream.rangeClosed(1, 5).boxed().map(i -> i * scaleFactor).toList();
    System.out.println("Numbers of activities to test: " + sizes);

    long spread = 5;
    Duration unit = SECONDS;

    final Directive biteBanana = new Directive("BiteBanana", Map.of());
    final Directive peelBanana = new Directive("PeelBanana", Map.of());
    final Directive changeProducerChiquita = new Directive("ChangeProducer", Map.of("producer", SerializedValue.of("Chiquita")));
    final Directive changeProducerDole = new Directive("ChangeProducer", Map.of("producer", SerializedValue.of("Dole")));
    final Directive[] serializedActivities = new Directive[] {changeProducerChiquita, changeProducerDole, peelBanana, biteBanana};


    var testTimer = new Timer("testPerformanceOfOneEditToScaledPlan", false);

    // test each case
    for (int numActs : sizes) {

      var scaleTimer = new Timer("test " + numActs, false);

      // generate numActs activities
      Pair<Duration, Directive>[] pairs = new Pair[numActs];
      for (int i = 0; i < numActs; ++i) {
        pairs[i] = Pair.of(duration(spread * (i + 1), unit),
                           serializedActivities[i % serializedActivities.length]);
      }
      var schedule = Schedule.build(pairs);
      long initialId = schedule.entries().getFirst().id();

      final var startTime = Instant.now();
      final var simDuration = duration(spread * (numActs + 2), SECOND);

      var timer = new Timer(INIT_SIM + " " + numActs, false);
      final var driver = (Simulator<Cache>) SimulationUtility.getDriver(simDuration);
      var simulationResults = driver.simulate(MODEL, CONFIG, schedule, startTime, simDuration);
      final var originalSchedule = schedule;
      timer.stop(false);

      timer = new Timer(COMP_RESULTS + " " + numActs, false);
//       = driver.computeResults(startTime, simDuration);
      timer.stop(false);
      timer = new Timer(SERIALIZE_RESULTS + " " + numActs, false);
//      String results = simulationResults.toString();
      timer.stop(false);

      var random = new Random(3);

      for (int j=0; j < numEdits; ++j) {

        // Modify a directive in the schedule
        long directiveId = initialId + random.nextInt(numActs);
        final var directive = schedule.get(directiveId);
        Duration newOffset = directive.startOffset().plus(1, unit);
        if (newOffset.noShorterThan(simDuration)) newOffset = simDuration.minus(1, unit);
        schedule = schedule.put(directiveId, newOffset, directive.directive());

        timer = new Timer(INC_SIM + " " + numActs + " " + j, false);
        driver.initSimulation(simDuration);
        simulationResults = driver.diffAndSimulate(MODEL, CONFIG, originalSchedule, schedule, startTime, simDuration, simulationResults.cache());
        timer.stop(false);

        timer = new Timer(COMP_INC_RESULTS + " " + numActs + " " + j, false);
//        simulationResults = driver.computeResults(startTime, simDuration);
        timer.stop(false);
        timer = new Timer(SERIALIZE_INC_RESULTS + " " + numActs + " " + j, false);
//        results = simulationResults.toString();  // The results are not combined until they forced to be
        timer.stop(false);
      }
      scaleTimer.stop(false);
    }

    testTimer.stop(false);

    //Timer.logStats();
    // Write out stats
    final ConcurrentSkipListMap<String, ConcurrentSkipListMap<Timer.StatType, Long>>
        mm = Timer.getStats();
    ArrayList<String> header = new ArrayList<>();
    header.add("Number of Activities");
    header.add("Number of Incremental Simulations");
    for (int i = 0; i < incSimLabels.length; ++i) {
      header.add(incSimLabels[i] + " (duration)");
      header.add(incSimLabels[i] + " (cpu time)");
    }
    System.out.println(String.join(", ", header));
    for (int numActs : sizes) {
      for (int j = 0; j < numEdits; ++j) {
        ArrayList<String> row = new ArrayList<>();
        row.add("" + numActs);
        row.add("" + j);
        for (int i = 0; i < incSimLabels.length; ++i) {
          ConcurrentSkipListMap<Timer.StatType, Long> statMap = mm.get(incSimLabels[i] + " " + numActs + " " + j);
          row.add("" + Timer.formatDuration(statMap.get(Timer.StatType.wallClockTime)));
          row.add("" + Timer.formatDuration(statMap.get(Timer.StatType.cpuTime)));
        }
        System.out.println(String.join(", ", row));
      }
    }
  }

}

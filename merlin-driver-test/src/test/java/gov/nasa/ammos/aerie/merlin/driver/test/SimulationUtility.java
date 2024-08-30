package gov.nasa.ammos.aerie.merlin.driver.test;

import gov.nasa.ammos.aerie.simulation.protocol.Simulator;
import gov.nasa.jpl.aerie.merlin.driver.IncrementalSimAdapter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public final class SimulationUtility {
  public static Simulator<?>
  getDriver(final Duration simulationDuration)
  {
    return getDriver(simulationDuration, false);
  }

  public static Simulator<?>
  getDriver(final Duration simulationDuration, boolean runDaemons)
  {
    return new IncrementalSimAdapter();
  }
}

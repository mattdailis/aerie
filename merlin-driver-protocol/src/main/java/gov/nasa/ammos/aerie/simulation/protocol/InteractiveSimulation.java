package gov.nasa.ammos.aerie.simulation.protocol;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;

public interface InteractiveSimulation {
  void restart();
  void addBreakpoint(BreakpointRequest breakpoint);
  String extendSimulationUntil(Duration startOffset);
  void getStackTrace();
  void getVariables();
  Results getCurrentResults();

  List<BreakpointRequest> breakpoints();

  sealed interface BreakpointRequest {
    record AtStartOffset(Duration duration) implements BreakpointRequest {}
    record OnCondition(Condition condition) implements BreakpointRequest {}
    record OnEmit(Topic<?> topic, PauseContext pauseContext) implements BreakpointRequest {}
  }

  enum PauseContext {
    //    Task, // TODO consider what it would mean to pause the simulation engine in a task context
    TopLevel
  }
}

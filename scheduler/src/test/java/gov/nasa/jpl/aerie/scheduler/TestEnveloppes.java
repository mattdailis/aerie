package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestEnveloppes {
  @Test
  public void testEnveloppes() {

    var horizon = new Windows(Window.betweenClosedOpen(Duration.of(0,Duration.SECONDS),Duration.of(20,Duration.SECONDS)));

    Window r1 = Window.betweenClosedOpen(Duration.of(1,Duration.SECONDS), Duration.of(10,Duration.SECONDS));
    Window r2 = Window.betweenClosedOpen(Duration.of(12,Duration.SECONDS), Duration.of(20,Duration.SECONDS));

    var resetExpr = new TimeRangeExpression.Builder().from(new Windows(List.of(r1, r2))).build();

    Window r3 = Window.betweenClosedOpen(Duration.of(6,Duration.SECONDS), Duration.of(11,Duration.SECONDS));
    Window r4 = Window.betweenClosedOpen(Duration.of(3,Duration.SECONDS), Duration.of(7,Duration.SECONDS));
    Window r5 = Window.betweenClosedOpen(Duration.of(0,Duration.SECONDS), Duration.of(3,Duration.SECONDS));
    Window r6 = Window.betweenClosedOpen(Duration.of(3,Duration.SECONDS), Duration.of(4,Duration.SECONDS));

    var firstType = new TimeRangeExpression.Builder().from(new Windows(List.of(r4, r6))).build();

    var secondType = new TimeRangeExpression.Builder().from(new Windows(List.of(r3, r5))).build();


    var enveloppe = new Transformers.EnveloppeBuilder()
        .withinEach(resetExpr)
        .when(firstType)
        .when(secondType)
        .build();

    TimeRangeExpression tre = new TimeRangeExpression.Builder()
        .from(resetExpr)
        .thenTransform(enveloppe)
        .name("encounter_envelopper_TRE")
        .build();
    var ranges = tre.computeRange(null, horizon);
    assert(ranges.size()==1);
    assert(ranges.includes(Window.betweenClosedOpen(Duration.of(1,Duration.SECONDS), Duration.of(10,Duration.SECONDS))));

  }


}



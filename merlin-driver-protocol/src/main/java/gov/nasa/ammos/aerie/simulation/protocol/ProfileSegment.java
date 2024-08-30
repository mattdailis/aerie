package gov.nasa.ammos.aerie.simulation.protocol;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public record ProfileSegment<Dynamics>(Duration extent, Dynamics dynamics) {
}
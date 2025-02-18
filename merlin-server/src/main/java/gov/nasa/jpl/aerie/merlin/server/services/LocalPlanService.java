package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;

import java.util.Map;

public final class LocalPlanService implements PlanService {
  private final PlanRepository planRepository;

  public LocalPlanService(
      final PlanRepository planRepository
  ) {
    this.planRepository = planRepository;
  }

  @Override
  public Plan getPlan(final String planId) throws NoSuchPlanException {
    return this.planRepository.getPlan(planId);
  }

  @Override
  public RevisionData getPlanRevisionData(final String planId) throws NoSuchPlanException {
    return this.planRepository.getPlanRevisionData(planId);
  }

  @Override
  public Map<String, Constraint> getConstraintsForPlan(final String planId) throws NoSuchPlanException {
    return this.planRepository.getAllConstraintsInPlan(planId);
  }

  @Override
  public long addExternalDataset(final String id, final Timestamp datasetStart, final ProfileSet profileSet)
  throws NoSuchPlanException
  {
    return this.planRepository.addExternalDataset(id, datasetStart, profileSet);
  }
}

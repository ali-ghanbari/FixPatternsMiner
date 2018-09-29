package edu.washington.escience.myria.parallel;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;

/**
 * A {@link QueryPlan} node that signifies running each of its children serially.
 */
public final class Sequence extends QueryPlan {
  /** The child query plans to run serially. */
  private final List<QueryPlan> plans;

  /**
   * Construct a {@link QueryPlan} that runs the given tasks in sequence.
   * 
   * @param plans the tasks to be run.
   */
  public Sequence(final List<QueryPlan> plans) {
    this.plans = ImmutableList.copyOf(Objects.requireNonNull(plans, "plans"));
  }

  /**
   * @return the tasks
   */
  public List<QueryPlan> getTasks() {
    return plans;
  }

  @Override
  public void instantiate(final LinkedList<QueryPlan> planQ, final LinkedList<SubQuery> subQueryQ, final Server server) {
    QueryPlan checkTask = planQ.peekFirst();
    Verify.verify(checkTask == this, "this %s should be the first object on the queue, not %s!", this, checkTask);
    planQ.removeFirst();
    planQ.addAll(plans);
  }
}

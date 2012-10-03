package scalagmite;

/*
 * #%L
 * Scalagmite
 * %%
 * Copyright (C) 2012 Gerard Dethier
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.apache.log4j.Logger;

public class Scalagmite {

  private static Scalagmite singleton;

  public static Scalagmite init() {
    if (singleton != null) {
      throw new RuntimeException("Instance was already created");
    }

    singleton = new Scalagmite();

    return singleton;
  }

  public static Scalagmite get() {
    return singleton;
  }

  private enum State {
    INIT, RUNNING, STOPPED
  }

  private State currentState;

  private final Configuration config;

  private final Router router;

  private boolean needScheduler;

  private final Scheduler scheduler;

  private Logger logger;

  Scalagmite() {
    this(new DefaultErrorHandler(), new Provider<Worker>() {
      @Override
      public Worker get() {
        return new WorkerImpl();
      }
    });
  }

  Scalagmite(ErrorHandler handler, Provider<Worker> workerProvider) {
    currentState = State.INIT;

    config = new Configuration();
    router = new Router(handler);
    scheduler = new Scheduler(workerProvider, config);

    logger = Logger.getLogger(getClass());
  }

  public synchronized <T extends AbstractAgent> void register(Class<T> clazz)
      throws ScalagmiteException {
    try {
      T agent = clazz.newInstance();
      agent.setup();
      register(agent, true);
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  public synchronized <T extends AbstractAgent> void register(Class<T> clazz,
      int numberOfInstances) throws ScalagmiteException {
    try {
      for (int i = 0; i < numberOfInstances; ++i) {
        T agent = clazz.newInstance();
        agent.setup();
        register(agent, false);
      }
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  public synchronized void register(AbstractAgent agent, boolean unique)
      throws ScalagmiteException {
    logger.info("Registering agent " + agent.getName());
    router.registerAgent(agent, unique);

    if (agent instanceof SchedulableAgent) {
      needScheduler = true;
      ((SchedulableAgent) agent).setScheduler(scheduler);
    }
  }

  public void main() throws ScalagmiteException, InterruptedException {
    synchronized (this) {
      if (!currentState.equals(State.INIT)) {
        throw new ScalagmiteException("Run cannot be called when state is "
            + currentState.name());
      }
      currentState = State.RUNNING;
    }

    if (needScheduler) {
      scheduler.start();
    }

    Exception error = null;
    try {
      router.startAll();
      router.waitAllStopped();
    } catch (Exception e) {
      error = e;
      router.stopAll();
    }

    if (needScheduler) {
      scheduler.stop();
    }

    if (error != null) {
      throw new ScalagmiteException("Encountered an error", error);
    }
  }
}

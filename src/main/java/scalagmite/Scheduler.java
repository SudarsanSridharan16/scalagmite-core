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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

public class Scheduler {

  private final Provider<Worker> workerProvider;

  private List<Worker> workers = new ArrayList<Worker>();

  private BlockingQueue<Slot> workQueue = new LinkedBlockingQueue<Slot>();

  private List<Exception> errors = new ArrayList<Exception>();

  private final Logger logger;

  Scheduler(Provider<Worker> workerProvider, Configuration config) {
    this.workerProvider = workerProvider;

    int numOfThreads = config.getNumberOfThreads();
    if (numOfThreads <= 0) {
      throw new RuntimeException("Number of threads must be greater than zero");
    }

    for (int i = 0; i < numOfThreads; ++i) {
      addNewWorker();
    }

    logger = Logger.getLogger(getClass());
  }

  private void addNewWorker() {
    Worker worker = workerProvider.get();
    worker.setScheduler(this);
    worker.setQueue(workQueue);
    workers.add(worker);
  }

  public int getNumOfThreads() {
    return workers.size();
  }

  public void join() throws InterruptedException {
    for (Worker w : workers) {
      w.join();
    }
  }

  synchronized void signalError(Worker worker, Exception e) {
    errors.add(e);
    stop();
  }

  public synchronized void start() {
    Logger.getLogger(getClass()).debug(
        "Starting " + workers.size() + " workers...");
    for (Worker w : workers) {
      w.start();
    }
  }

  public synchronized void stop() {
    Logger.getLogger(getClass()).debug(
        "Stopping " + workers.size() + " workers...");
    for (int i = 0; i < workers.size(); ++i) {
      try {
        workQueue.put(new Slot(null));
      } catch (InterruptedException e) {
        logger.error("Could not stop queue");
      }
    }
  }

  void schedule(SchedulableAgent agent) {
    try {
      workQueue.put(new Slot(agent));
    } catch (InterruptedException e) {
      logger.error("Could not schedule agent " + agent.getClass().getName());
    }
  }

}

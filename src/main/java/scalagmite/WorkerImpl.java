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

import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

public class WorkerImpl implements Worker {

  private Scheduler scheduler;

  private BlockingQueue<Slot> queue;

  private Thread thread;

  WorkerImpl() {
  }

  @Override
  public void join() throws InterruptedException {
    if (thread != null) {
      throw new RuntimeException("Worker has not yet been started");
    }
    thread.join();
  }

  @Override
  public void run() {
    while (true) {
      Slot slot;
      try {
        slot = queue.take();
      } catch (InterruptedException e) {
        return;
      }

      if (slot.getAgent() == null) {
        Logger.getLogger(getClass()).debug("Closing worker");
        return;
      }

      Logger.getLogger(getClass()).trace("Handling slot");
      try {
        slot.getAgent().consumeMessage();
      } catch (Exception e) {
        scheduler.signalError(this, e);
        return;
      }
    }
  }

  @Override
  public void setScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public void setQueue(BlockingQueue<Slot> workQueue) {
    this.queue = workQueue;
  }

  @Override
  public void start() {
    if (thread != null) {
      throw new RuntimeException("Worker has already been started");
    }

    thread = new Thread(null, this, "Worker");
    thread.start();
  }

}

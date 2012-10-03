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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

public class Router {

  private final ErrorHandler errorHandler;

  private final Map<String, AgentGroup> registeredAgents = new HashMap<String, AgentGroup>();

  private boolean routerIsUp = true;

  private int runningAgents = 0;

  private final Semaphore waitAllSync = new Semaphore(0);

  Router(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public synchronized AbstractAgent getAgent(String name) {
    return getAgent(name, 0);
  }

  public synchronized AbstractAgent getAgent(String name, int num) {
    if (!registeredAgents.containsKey(name)) {
      return null;
    }
    AgentGroup group = registeredAgents.get(name);
    return group.getAgent(num);
  }

  public synchronized void registerAgent(AbstractAgent agent)
      throws RouterException {
    registerAgent(agent, true);
  }

  public synchronized void registerAgent(AbstractAgent agent, boolean unique)
      throws RouterException {
    if (!routerIsUp) {
      throw new RouterException("Router is already down");
    }

    String name = agent.getName();
    if (name == null || name.isEmpty()) {
      throw new RouterException("Cannot register an agent without name");
    }

    AgentGroup group = registeredAgents.get(name);
    if (group == null) {
      group = new AgentGroup(unique);
      registeredAgents.put(name, group);
    } else if (unique) {
      throw new RouterException(
          "Cannot register unique agent, others already exist");
    }

    agent.setRouter(this);
    group.addAgent(agent);
  }

  public synchronized void signalAgentStart(AbstractAgent agent) {
    Preconditions.checkArgument(registeredAgents.containsKey(agent.getName()),
        "Agent is not registered or name has changed since registration");
    ++runningAgents;
  }

  public synchronized void signalAgentStop(AbstractAgent agent) {
    Preconditions.checkArgument(registeredAgents.containsKey(agent.getName()),
        "Agent is not registered or name has changed since registration");
    --runningAgents;
    if (runningAgents == 0) {
      routerIsUp = false;
      waitAllSync.release();
    }
  }

  public synchronized void signalError(String agentName, Exception e) {
    if (errorHandler.isFatal(agentName, e)) {
      for (AgentGroup group : registeredAgents.values()) {
        group.stopWithError("Fatal error in agent " + agentName);
      }
    }
  }

  public boolean tryWaitAllStopped(long timeout) throws InterruptedException {
    return waitAllSync.tryAcquire(timeout, TimeUnit.MILLISECONDS);
  }

  public synchronized void unregisterAgent(String name) throws RouterException {
    if (registeredAgents.get(name) == null) {
      throw new RouterException("No registered agent with name '" + name + "'");
    }

    registeredAgents.remove(name);
  }

  public void waitAllStopped() throws InterruptedException {
    waitAllSync.acquire();
  }

  public synchronized void startAll() throws AgentException {
    for (AgentGroup group : registeredAgents.values()) {
      group.startAll();
    }
  }

  public synchronized void stopAll() {
    for (AgentGroup group : registeredAgents.values()) {
      group.stopAll();
    }
  }
}

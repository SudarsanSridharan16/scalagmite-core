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
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import scalagmite.events.InitAgent;
import scalagmite.events.StopAgent;

public abstract class AbstractAgent {

  /** Represents the 3 possible states of an AbstractAgent */
  public enum AgentStatus {
    INIT, RUNNING, STOPPED
  }

  /** Messages queue. */
  private LinkedBlockingQueue<Object> incoming;

  /** Message handlers map. */
  private final Map<Class<?>, MessageHandler<?>> handlers;

  /** Init message handler. */
  private MessageHandler<InitAgent> initHandler;

  /** Exit message handler. */
  private MessageHandler<StopAgent> exitHandler;

  /** The current state of the agent. */
  private AgentStatus status;
  private AgentStatus safeStatus;
  private boolean started;
  private boolean stopped;
  private boolean setup;

  private Exception error;

  private String agentName;

  private int number;

  private Router router;

  AbstractAgent() {
    this(null, 0);
  }

  AbstractAgent(String name) {
    this(name, 0);
  }

  AbstractAgent(int capacity) {
    this(null, capacity);
  }

  AbstractAgent(String name, int capacity) {
    handlers = new HashMap<Class<?>, MessageHandler<?>>();

    agentName = (name != null && !name.isEmpty()) ? name : getClass()
        .getCanonicalName();

    if (capacity <= 0) {
      incoming = new LinkedBlockingQueue<Object>();
    } else {
      incoming = new LinkedBlockingQueue<Object>(capacity);
    }

    status = AgentStatus.INIT;
  }

  @SuppressWarnings("unchecked")
  public void consumeMessage() {

    try {
      Object message = incoming.take();

      // Check agent has not already encountered an error
      if (error != null) {
        throw new AgentException(
            "Agent has encountered an error and cannot continue its execution");
      }

      if (AgentStatus.STOPPED.equals(safeStatus)) {
        Logger.getLogger(getClass()).warn(
            "Dropped message " + message.getClass().getName());
        return;
      }

      // If message is an exception, throw it
      if (message instanceof Exception) {
        error = (Exception) message;
        throw error;
      }

      // Consume next message in queue
      @SuppressWarnings("rawtypes")
      MessageHandler handler = handlers.get(message.getClass());
      if (handler != null) {
        handler.handle(message);
      } else {
        throw new UnknownAgentMessage(message);
      }

    } catch (Exception e) {
      setStatus(AgentStatus.STOPPED);
      error = e;
      try {
        router.signalError(agentName, e);
      } catch (Exception e1) {
        Logger.getLogger(getClass()).warn("Ignoring error", e);
      }
    }

  }

  public Throwable getError() {
    return error;
  }

  public String getName() {
    return agentName;
  }

  void setRouter(Router router) {
    this.router = router;
  }

  public Router getRouter() {
    return router;
  }

  public synchronized AgentStatus getStatus() {
    return status;
  }

  public abstract void join() throws InterruptedException;

  public abstract void join(long millis) throws InterruptedException;

  protected void logError(Exception e) {
    if (agentName != null) {
      Logger.getLogger(agentName).error(e.getMessage(), e);
    } else {
      Logger.getLogger(getClass()).error(e.getMessage(), e);
    }
  }

  protected abstract void onStart();

  protected abstract void onStop();

  public void registerExitHandler(MessageHandler<StopAgent> handler)
      throws AgentException {
    if (exitHandler != null) {
      throw new AgentException("An exit handler has already been registered");
    }
    exitHandler = handler;
  }

  public void registerHandler(Class<?> messageType, MessageHandler<?> handler)
      throws AgentException {
    if (handlers.containsKey(messageType)) {
      throw new AgentException(
          "A handler is already registered for message type "
              + messageType.getName());
    }
    handlers.put(messageType, handler);
  }

  public void registerInitHandler(MessageHandler<InitAgent> handler)
      throws AgentException {
    if (initHandler != null) {
      throw new AgentException("An init handler has already been registered");
    }
    initHandler = handler;
  }

  private void registerPrivateHandlers() throws AgentException {
    registerHandler(InitAgent.class, new MessageHandler<InitAgent>() {
      @Override
      public void handle(InitAgent message) throws Exception {
        setStatus(AgentStatus.RUNNING);
        safeStatus = AgentStatus.RUNNING;

        if (initHandler != null) {
          initHandler.handle(message);
        }
      }
    });

    registerHandler(StopAgent.class, new MessageHandler<StopAgent>() {
      @Override
      public void handle(StopAgent message) throws Exception {
        setStatus(AgentStatus.STOPPED);
        safeStatus = AgentStatus.STOPPED;

        if (exitHandler != null) {
          exitHandler.handle(message);
        }

        onStop();
      }
    });

    registerHandler(Exception.class, new MessageHandler<Exception>() {
      @Override
      public void handle(Exception message) throws Exception {
        throw message;
      }
    });
  }

  public void route(String dest, Object message) throws AgentException {
    route(dest, 0, message);
  }

  public void route(String dest, int number, Object message)
      throws AgentException {
    AbstractAgent agent = router.getAgent(dest, number);
    if (agent == null) {
      throw new AgentException("No registered agent with name '" + dest
          + "' and number " + number);
    }

    agent.submitMessage(message);
  }

  void setAgentName(String name) {
    this.agentName = name;
  }

  public void setName(String name) {
    if (setup) {
      throw new RuntimeException("Agent has already been setup");
    }
    this.agentName = name;
  }

  void setNumber(int number) {
    this.number = number;
  }

  public int getNumber() {
    return this.number;
  }

  private synchronized void setStatus(AgentStatus newState) {
    status = newState;
  }

  public void setup() throws AgentException, RouterException {
    registerPrivateHandlers();
    setup = true;
  }

  public synchronized void start() throws AgentException {
    if (!setup) {
      throw new AgentException("setup() has not been invoked, cannot start");
    }

    if (!started) {
      started = true;
      router.signalAgentStart(this);
      submitMessage(new InitAgent());
      onStart();
    }
  }

  public synchronized void stop() throws AgentException {
    stopWithError("Normal stop call");
  }

  public synchronized void stopWithError(String cause) throws AgentException {
    if (!started) {
      throw new AgentException("Cannot stop agent that has not been started");
    }

    if (!stopped) {
      stopped = true;
      router.signalAgentStop(this);
      submitMessage(new StopAgent("Normal stop call"));
    }
  }

  public void submitMessage(Object o) {
    try {
      incoming.put(o);
    } catch (InterruptedException e) {
      Logger.getLogger(getClass()).error("Could not submit message", e);
    }
  }

}

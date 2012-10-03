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

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import scalagmite.AbstractAgent.AgentStatus;
import scalagmite.events.InitAgent;
import scalagmite.events.StopAgent;

public class AbstractAgentTest {

  private Router router;

  private AbstractAgent agent;

  @Before
  public void before() {
    router = Mockito.mock(Router.class);

    agent = new AbstractAgent("name") {
      @Override
      public void join() throws InterruptedException {
      }

      @Override
      public void join(long millis) throws InterruptedException {
      }

      @Override
      protected void onStart() {
      }

      @Override
      protected void onStop() {
      }
    };
    agent.setRouter(router);
  }

  @Test
  public void consume() throws Exception {
    MessageHandler<Integer> handler = Mockito.mock(MessageHandler.class);
    agent.registerHandler(Integer.class, handler);

    Integer newVal = 3;
    agent.submitMessage(newVal);
    agent.consumeMessage();

    Mockito.verify(handler).handle(newVal);
    Assert.assertThat(agent.getError(), IsNull.nullValue());
  }

  @Test
  public void consumeError() throws AgentException, InterruptedException,
      RouterException {
    Exception error = new Exception("error");
    agent.submitMessage(error);
    agent.consumeMessage();
    Assert.assertThat(agent.getError(), IsNull.notNullValue());
    Assert.assertThat(agent.getStatus(), Is.is(AgentStatus.STOPPED));
    Mockito.verify(router).signalError(agent.getName(), error);
  }

  @Test
  public void flow() throws Exception {
    Assert.assertThat(agent.getStatus(), Is.is(AbstractAgent.AgentStatus.INIT));

    MessageHandler<InitAgent> initHandler = Mockito.mock(MessageHandler.class);
    MessageHandler<StopAgent> exitHandler = Mockito.mock(MessageHandler.class);

    agent.registerInitHandler(initHandler);
    agent.registerExitHandler(exitHandler);
    agent.setup(); // Otherwise, handlers are not actually activated

    agent.submitMessage(new InitAgent());
    agent.consumeMessage(); // consume init event
    Assert.assertThat(agent.getStatus(),
        Is.is(AbstractAgent.AgentStatus.RUNNING));
    Mockito.verify(initHandler).handle(Mockito.any(InitAgent.class));

    agent.submitMessage(new StopAgent(""));
    agent.consumeMessage(); // consume exit event
    Assert.assertThat(agent.getStatus(),
        Is.is(AbstractAgent.AgentStatus.STOPPED));
    Mockito.verify(exitHandler).handle(Mockito.any(StopAgent.class));
  }

  @Test
  public void routerCom() throws AgentException, InterruptedException,
      RouterException {
    agent.setup();
    agent.start();
    Mockito.verify(router).signalAgentStart(agent);
    agent.stop();
    Mockito.verify(router).signalAgentStop(agent);
  }
}

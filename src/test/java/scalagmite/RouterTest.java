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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RouterTest {

  private ErrorHandler errorHandler;

  private Router router;

  @Before
  public void before() {
    errorHandler = Mockito.mock(ErrorHandler.class);
    router = new Router(errorHandler);
  }

  @Test(expected = RouterException.class)
  public void duplicate() throws RouterException {
    String name = "name";
    AbstractAgent agent = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent.getName()).thenReturn(name);
    router.registerAgent(agent);
    router.registerAgent(agent);
  }

  @Test
  public void register() throws AgentException, RouterException {
    String name = "name";
    AbstractAgent agent = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent.getName()).thenReturn(name);
    router.registerAgent(agent);
    AbstractAgent registered = router.getAgent(name);

    Assert.assertThat(registered, Is.is(agent));
  }

  @Test(expected = RouterException.class)
  public void registerEmpty() throws RouterException {
    AbstractAgent agent = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent.getName()).thenReturn("");
    router.registerAgent(agent);
  }

  @Test(expected = RouterException.class)
  public void registerNull() throws RouterException {
    AbstractAgent agent = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent.getName()).thenReturn(null);
    router.registerAgent(agent);
  }

  @Test
  public void testSignalError() {
    Exception e = new Exception();
    router.signalError("name", e);
    Mockito.verify(errorHandler).isFatal("name", e);
  }

  @Test
  public void testWaitForAll() throws RouterException, InterruptedException {
    AbstractAgent agent1 = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent1.getName()).thenReturn("agent1");
    router.registerAgent(agent1);

    AbstractAgent agent2 = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent2.getName()).thenReturn("agent2");
    router.registerAgent(agent2);

    router.signalAgentStart(agent1);
    router.signalAgentStart(agent2);

    router.signalAgentStop(agent1);
    router.signalAgentStop(agent2);

    Assert.assertTrue(router.tryWaitAllStopped(1));
  }

  @Test
  public void testWaitForAll2() throws RouterException, InterruptedException {
    AbstractAgent agent1 = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent1.getName()).thenReturn("agent1");
    router.registerAgent(agent1);

    AbstractAgent agent2 = Mockito.mock(AbstractAgent.class);
    Mockito.when(agent2.getName()).thenReturn("agent2");
    router.registerAgent(agent2);

    router.signalAgentStart(agent1);
    router.signalAgentStart(agent2);

    router.signalAgentStop(agent1);

    Assert.assertTrue(!router.tryWaitAllStopped(1));
  }
}

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

import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ThreadAgentTest {

  private Router router;

  private ThreadAgent agent;

  @Before
  public void before() {
    router = Mockito.mock(Router.class);
    agent = new ThreadAgent();
    agent.setRouter(router);
  }

  @Test
  public void consume() throws Exception {
    MessageHandler<Integer> handler = Mockito.mock(MessageHandler.class);
    agent.registerHandler(Integer.class, handler);

    agent.setup();
    agent.start();

    Integer newVal = 3;
    agent.submitMessage(newVal);

    agent.stop();
    agent.join();

    Mockito.verify(handler).handle(newVal);
    Assert.assertThat(agent.getError(), IsNull.nullValue());
  }
}

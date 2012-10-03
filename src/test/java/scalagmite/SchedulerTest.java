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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SchedulerTest {
  private static final int THREAD_NUM = 5;

  private Configuration config;

  private Provider<Worker> provider;

  private Scheduler scheduler;

  @Before
  public void before() {
    config = Mockito.mock(Configuration.class);
    Mockito.when(config.getNumberOfThreads()).thenReturn(THREAD_NUM);

    provider = new Provider<Worker>() {
      public Worker get() {
        return Mockito.mock(Worker.class);
      }
    };

    scheduler = new Scheduler(provider, config);
  }

  @Test
  public void schedule() {
    scheduler.schedule(Mockito.mock(SchedulableAgent.class));
  }
}

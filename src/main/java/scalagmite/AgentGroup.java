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

import org.apache.log4j.Logger;

public class AgentGroup {
  private boolean unique;

  private ArrayList<AbstractAgent> agents;

  AgentGroup() {
    this(false);
  }

  AgentGroup(boolean unique) {
    this.unique = unique;
    agents = new ArrayList<AbstractAgent>();
  }

  public AbstractAgent getAgent(int num) {
    if (num >= agents.size()) {
      return null;
    } else {
      return agents.get(num);
    }
  }

  public void addAgent(AbstractAgent agent) throws RouterException {
    if (unique && agents.size() >= 1) {
      throw new RouterException(
          "Group has unique flag, cannot add another agent");
    }

    agent.setNumber(agents.size());
    agents.add(agent);
  }

  public void stopWithError(String cause) {
    for (AbstractAgent agent : agents) {
      try {
        agent.stopWithError(cause);
      } catch (AgentException e) {
        Logger.getLogger(getClass()).warn(e);
      }
    }
  }

  public void startAll() throws AgentException {
    for (AbstractAgent agent : agents) {
      agent.start();
    }
  }

  public void stopAll() {
    for (AbstractAgent agent : agents) {
      try {
        agent.stop();
      } catch (AgentException e) {
        Logger.getLogger(getClass()).warn(e);
      }
    }
  }
}

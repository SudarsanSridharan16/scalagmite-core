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

public class ThreadAgent extends AbstractAgent implements Runnable {

  private Thread agentThread;

  protected ThreadAgent() {
    this(null);
  }

  protected ThreadAgent(String name) {
    this(name, 0);
  }

  protected ThreadAgent(String name, int capacity) {
    super(name, capacity);

    name = name != null ? name : getClass().getSimpleName();
    agentThread = new Thread(this, name);
  }

  @Override
  public void join() throws InterruptedException {
    agentThread.join();
  }

  @Override
  public void join(long millis) throws InterruptedException {
    agentThread.join(millis);
  }

  @Override
  protected void onStart() {
    agentThread.start();
  }

  @Override
  protected void onStop() {
    // Nothing to do
  }

  /**
   * This method implements the <code>Runnable</code> interface. It describes
   * the sequential code executed by message handling thread:
   * <ol>
   * <li>initialization,</li>
   * <li>message handling loop,</li>
   * <li>termination (caused by a stop event or an error).</li>
   * </ol>
   */
  @Override
  public void run() {
    while (!AgentStatus.STOPPED.equals(getStatus())) {
      consumeMessage();
    }
  }

}

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

/**
 * @author Gerard Dethier
 * 
 *         Exception thrown by an agent to signal that it cannot handle a
 *         message that was inserted into its queue. This exception is thrown on
 *         handling, not on message submission.
 */
public class UnknownAgentMessage extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs an exception whose detail message is the class of the unknown
   * agent message. No cause is set.
   * 
   * @param message
   *          The unknown agent message.
   */
  public UnknownAgentMessage(Object message) {
    super(message.getClass().toString());
  }
}

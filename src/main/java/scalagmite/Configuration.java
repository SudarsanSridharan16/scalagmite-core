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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Configuration {

  private static final String SCHEDULER_THREADS = "scheduler.threads";

  private Properties properties = new Properties();

  Configuration() {
    InputStream in = getClass().getResourceAsStream("default.properties");
    if (in != null) {
      try {
        properties.load(in);
      } catch (IOException e) {
        Logger.getLogger(getClass()).error(e);
      }
    }
  }

  public int getNumberOfThreads() {
    return Integer.parseInt(properties.getProperty(SCHEDULER_THREADS, "5"));
  }
}

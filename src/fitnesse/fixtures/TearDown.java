// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.fixtures;

import static fitnesse.fixtures.FitnesseFixtureContext.*;
import fitnesse.util.Clock;

public class TearDown {
  public TearDown() throws Exception {
    context.fitNesse.stop();
    authenticator = null;
    Clock.restoreDefaultClock();
    context = null;
  }
}

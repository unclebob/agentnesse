// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.fixtures;

import static agnesse.fixtures.AgNesseFixtureContext.*;
import agnesse.util.Clock;

public class TearDown {
  public TearDown() throws Exception {
    context.agNesse.stop();
    Clock.restoreDefaultClock();
    context = null;
  }
}

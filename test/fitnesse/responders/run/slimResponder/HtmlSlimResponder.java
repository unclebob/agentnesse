// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.run.slimResponder;

import fitnesse.testsystems.slim.SlimClient;
import fitnesse.testsystems.slim.HtmlSlimTestSystem;
import fitnesse.testsystems.slim.InProcessSlimClientBuilder;
import fitnesse.testsystems.slim.SlimClientBuilder;
import fitnesse.testsystems.slim.SlimTestSystem;
import fitnesse.testsystems.slim.tables.SlimTableFactory;

import java.io.IOException;

public class HtmlSlimResponder extends SlimResponder {
  public HtmlSlimResponder() {
  }

  @Override
  protected SlimTestSystem getTestSystem() throws IOException {

    SlimClient slimClient;
    if (fastTest) {
      slimClient = new InProcessSlimClientBuilder(getDescriptor(), ClassLoader.getSystemClassLoader()).build();
    } else {
      slimClient = new SlimClientBuilder(getDescriptor()).build();
    }
    SlimTestSystem testSystem = new HtmlSlimTestSystem("slim", slimClient,
            new SlimTableFactory());
    testSystem.addTestSystemListener(this);
    return testSystem;
  }

}

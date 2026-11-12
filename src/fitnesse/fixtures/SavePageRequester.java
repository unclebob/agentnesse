// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.fixtures;


public class SavePageRequester extends ResponseRequester {
  public String saveContents;

  @Override
  protected void details() {
    request.addInput("responder", "saveData");
    request.addInput("pageContent", saveContents);
  }

  public void setSaveContents(String saveContents) {
    this.saveContents = saveContents;
  }
}

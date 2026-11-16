// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.testsystems.slim;

import java.util.Iterator;

public interface TableScanner<T extends Table> extends Iterable<T> {
  int getTableCount();

  T getTable(int i);

  @Override
  Iterator<T> iterator();
}

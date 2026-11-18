// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.slim.converters;

public class IntConverter extends ConverterBase<Integer> {

  @Override protected Integer getObject(String arg) { return parsed(arg, "integer", Integer::valueOf); }
}

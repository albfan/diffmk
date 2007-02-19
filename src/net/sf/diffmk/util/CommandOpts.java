/* CommandOpts */

package net.sf.diffmk.util;

/* Copyright (C) 2001, 2002 Sun Microsystems, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import java.util.Hashtable;
import java.util.Enumeration;

public class CommandOpts {
  public static final int STRING = 0;
  public static final int INTEGER = 1;
  public static final int BOOLEAN = 2;

  protected Hashtable options = null;
  protected Hashtable option = null;
  protected Hashtable parsed = null;

  public CommandOpts() {
    options = new Hashtable();
    option = new Hashtable();
    parsed = new Hashtable();
  }

  public void addOption(String name, int type) {
    if (type < STRING || type > BOOLEAN) {
      System.err.println("Invalid type for " + name);
    } else {
      options.put(name, new Integer(type));
    }
  }

  public void setParsed(String name) {
    if (options.containsKey(name)) {
      parsed.put(name, new Boolean(true));
    }
  }

  public void setOption(String name, String value) {
    if (options.containsKey(name)) {
      Integer type = (Integer) options.get(name);
      if (type.intValue() != STRING) {
	System.err.println("Invalid type for name: string");
      } else {
	//System.out.println(name + "=" + value);
	option.put(name, value);
      }
    } else {
      System.err.println("No such option: " + name);
    }
  }

  public void setOption(String name, int value) {
    if (options.containsKey(name)) {
      Integer type = (Integer) options.get(name);
      if (type.intValue() != INTEGER) {
	System.err.println("Invalid type for name: integer");
      } else {
	//System.out.println(name + "=" + value);
	option.put(name, new Integer(value));
      }
    } else {
      System.err.println("No such option: " + name);
    }
  }

  public void setOption(String name, boolean value) {
    if (options.containsKey(name)) {
      Integer type = (Integer) options.get(name);
      if (type.intValue() != BOOLEAN) {
	System.err.println("Invalid type for name: boolean");
      } else {
	//System.out.println(name + "=" + value);
	option.put(name, new Boolean(value));
      }
    } else {
      System.err.println("No such option: " + name);
    }
  }

  public boolean argumentSpecified(String name) {
    return parsed.containsKey(name);
  }

  public String getStringOption(String name) {
    if (options.containsKey(name)) {
      Integer type = (Integer) options.get(name);
      if (type.intValue() != STRING) {
	System.err.println("Invalid type for name: string");
      } else {
	return (String) option.get(name);
      }
    } else {
      System.err.println("No such option: " + name);
    }

    return null;
  }

  public boolean getBooleanOption(String name) {
    if (options.containsKey(name)) {
      Integer type = (Integer) options.get(name);
      if (type.intValue() != BOOLEAN) {
	System.err.println("Invalid type for name: boolean");
      } else {
	Boolean value = (Boolean) option.get(name);
	return value.booleanValue();
      }
    } else {
      System.err.println("No such option: " + name);
    }

    return false;
  }

  public int getIntegerOption(String name) {
    if (options.containsKey(name)) {
      Integer type = (Integer) options.get(name);
      if (type.intValue() != INTEGER) {
	System.err.println("Invalid type for name: integer");
      } else {
	Integer value = (Integer) option.get(name);
	return value.intValue();
      }
    } else {
      System.err.println("No such option: " + name);
    }

    return 0;
  }

  public int parseArgs(String[] args) {
    int argPos = 0;
    while (argPos < args.length
	   && args[argPos].startsWith("-")) {
      String arg = args[argPos++].substring(1);
      if (arg.startsWith("-")) {
	arg = arg.substring(1);
      }

      String nArg = null;

      if (arg.startsWith("no")) {
	nArg = arg.substring(2);
      }

      // Figure out which option this is:
      String name = null;
      int posNameCount = 0;
      int negNameCount = 0;
      for (Enumeration names = options.keys() ;
	   names.hasMoreElements() ; ) {
	String keyName = (String) names.nextElement();
	if (keyName.startsWith(arg)) {
	  posNameCount++;
	  name = keyName;
	}

	if (nArg != null && keyName.startsWith(nArg)) {
	  Integer type = (Integer) options.get(keyName);
	  if (type.intValue() == BOOLEAN) {
	    negNameCount++;
	    name = keyName;
	  }
	}
      }

      // Does arg uniquely identify an option or a negated boolean option?
      if (posNameCount + negNameCount == 0) {
	System.err.println("Unrecognized option: " + arg);
	System.exit(1);
      } else if (posNameCount + negNameCount > 1) {
	System.err.println("Ambiguous option: " + arg);
	System.exit(1);
      }

      // Ok, what type is this option?
      Integer type = (Integer) options.get(name);
      if (type.intValue() == BOOLEAN) {
	setParsed(name);
	if (negNameCount > 0) {
	  setOption(name, false);
	} else {
	  setOption(name, true);
	}
      } else if (type.intValue() == INTEGER) {
	String intStr = args[argPos++];
	try {
	  setOption(name, Integer.parseInt(intStr));
	  setParsed(name);
	} catch (Exception e) {
	  System.err.println("Numeric option "
			     + name
			     + " is not a number: "
			     + intStr);
	}
      } else { // STRING
	setParsed(name);
	setOption(name, args[argPos++]);
      }
    }

    return argPos;
  }
}

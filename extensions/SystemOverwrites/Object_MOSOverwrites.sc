//
// Object_MOSOverwrites.sc
//
//
// PUBLIC METHODS--
//   *doesNotUnderstandMethod
//   (*)doesNotUnderstand
//
//
// MOS DEPENDENCIES--
//   Log
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Object  {

  //--------------------------------------------------------------- -o-
  // Helper methods.

  //---------------------------- -o-
  *doesNotUnderstandMethod  { | methodType, selector ...args 
                              |
    var  dumpArgs       = "",
         methodTypeStr  = "class";

    if (\instance == methodType, { methodTypeStr = "instance"; });


    //
    if (args.size > 0, { dumpArgs = ("  Args: %.").format(args); });

    Log.error( nil, 
               "Unknown % method \"%.%\".%", 
                  methodTypeStr, this.classname, selector, dumpArgs
             ).postln;
    ^'';
  }


  //---------------------------- -o-
  *doesNotUnderstand  { |selector ...args|
    ^this.doesNotUnderstandMethod(\class, selector, *args);
  }

  //
  doesNotUnderstand  { |selector ...args|
    ^this.class.doesNotUnderstandMethod(\instance, selector, *args);
  }


} //Object


//
// Group_MOS.sc
//
//
// PUBLIC METHODS--
//   *newRegistered
//
// 
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Node_MOS
//   Object_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Group  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Class methods.

  *newRegistered  { | target, addAction, server, paused
                    |
    ^Node.newRegistered( target:     target, 
                         addAction:  addAction,
                         server:     server, 
                         paused:     paused
                       );
  }


} //Group


//
// Bus_MOS.sc
//
//
// PUBLIC METHODS--
//   *newControlOnServer
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Log
//   Object_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Bus  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }



  //------------------------------------------- -o--
  // Class methods.

  //------------------------ -o-
  *newControlOnServer  {  | server
                          |
    var  bus;

    //DEFAULTS.
    server = server ?? Server.default;

    //
    if (server.hasShmInterface.not, 
    {
      Log.error( thisFunction, 
                 "CANNOT CREATE Bus: server shared memory interface"
                    ++ " is NOT YET initialized...  (%)",
                      server.name.asString.quote
               ).postln; 
      ^nil;
    });

    //
    bus = Bus.control(server, 1);

    ^bus;
  }

} //Bus


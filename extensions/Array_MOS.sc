//
// Array_MOS.sc
//
//
// PUBLIC METHODS--
//   freeAll
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Object_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Array  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Instance methods.

  freeAll  { this.collect(_.free); }


} //Array


//
// Number_MOS.sc
//
//
// PUBLIC METHODS--
//   inRange
//   sizeInBytes
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Dump
//   Object_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Number  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Instance methods.

  //-------------------- -o-
  inRange  { | min, max
             |
    if (min.isNil || max.isNil, { ^false; });

    if ( (this >= min) && (this <= max), { ^true; });

    ^false;
  }


  //-------------------- -o-
  sizeInBytes  { ^Dump.sizeInBytes(this); } 


} //Number

